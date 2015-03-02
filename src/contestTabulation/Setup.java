/*
 * Component of GAE Project for TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [http://www.gnu.org/licenses/].
 */

package contestTabulation;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import util.BaseHttpServlet;
import util.Retrieve;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

@SuppressWarnings("serial")
public class Setup extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Queue queue = QueueFactory.getDefaultQueue();

		TaskOptions options = null;

		for (Level level : Level.values()) {
			String docName = req.getParameter("doc" + level.getName());
			if (docName != null) {
				options = withUrl("/createSpreadsheet").param("doc" + level.getName(), docName);
				break;
			}
		}

		if (options != null) {
			queue.add(options);
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Spreadsheet creation request must have document name parameter set");
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Entity contestInfo = Retrieve.contestInfo();

		GoogleCredential credential = new GoogleCredential.Builder()
			.setJsonFactory(jsonFactory)
			.setTransport(httpTransport)
			.setClientSecrets((String) contestInfo.getProperty("OAuth2ClientId"), (String) contestInfo.getProperty("OAuth2ClientSecret"))
			.build()
			.setFromTokenResponse(new JacksonFactory().fromString(((Text) contestInfo.getProperty("OAuth2Token")).getValue(), GoogleTokenResponse.class));

		String docName = null, docLevel = null;
		for (Level level : Level.values()) {
			docName = req.getParameter("doc" + level.getName());
			if (docName != null) {
				docLevel = level.toString();
				break;
			}
		}

		if (docLevel == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Spreadsheet creation request must have paramater document name parameter set");
			return;
		}

		Query query = new Query("registration")
			.setFilter(new FilterPredicate("schoolLevel", FilterOperator.EQUAL, docLevel))
			.addSort("schoolName", SortDirection.ASCENDING);
		List<Entity> registrations = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		Map<String, List<JSONObject>> studentData = new HashMap<String, List<JSONObject>>();
		for (Entity registration : registrations) {
			String regSchoolName = ((String) registration.getProperty("schoolName")).trim();
			String regStudentDataJSON = unescapeHtml4(((Text) registration.getProperty("studentData")).getValue());

			JSONArray regStudentData = null;
			try {
				regStudentData = new JSONArray(regStudentDataJSON);
			}
			catch (JSONException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
				return;
			}

			for (int i = 0; i < regStudentData.length(); i++) {
				if (!studentData.containsKey(regSchoolName)) {
					studentData.put(regSchoolName, new ArrayList<JSONObject>());
				}
				try {
					studentData.get(regSchoolName).add(regStudentData.getJSONObject(i));
				}
				catch (JSONException e) {
					resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					e.printStackTrace();
					return;
				}
			}
		}

		for (List<JSONObject> students : studentData.values()) {
			Collections.sort(students, new Comparator<JSONObject>() {
				@Override
				public int compare(JSONObject a, JSONObject b) {
					try {
						return a.getString("name").compareTo(b.getString("name"));
					}
					catch (JSONException e) {
						e.printStackTrace();
						return 0;
					}
				}
			});
		}

		Workbook workbook = new XSSFWorkbook();

		XSSFCellStyle boldStyle = (XSSFCellStyle) workbook.createCellStyle();
		Font boldFont = workbook.createFont();
		boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		boldStyle.setFont(boldFont);

		Map<Subject, XSSFCellStyle> subjectCellStyles = new HashMap<Subject, XSSFCellStyle>();
		for (Subject subject : Subject.values()) {
			final double ALPHA = .144;
			String colorStr = (String) contestInfo.getProperty("color" + subject.getName());
			byte[] backgroundColor = new byte[] {Integer.valueOf(colorStr.substring(1, 3), 16).byteValue(),
					Integer.valueOf(colorStr.substring(3, 5), 16).byteValue(), Integer.valueOf(colorStr.substring(5, 7), 16).byteValue()};
			// http://en.wikipedia.org/wiki/Alpha_compositing#Alpha_blending
			byte[] borderColor = new byte[] {(byte) ((backgroundColor[0] & 0xff) * (1 - ALPHA)), (byte) ((backgroundColor[1] & 0xff) * (1 - ALPHA)),
					(byte) ((backgroundColor[2] & 0xff) * (1 - ALPHA))};

			XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
			style.setFillBackgroundColor(new XSSFColor(backgroundColor));
			style.setFillPattern(CellStyle.ALIGN_FILL);

			style.setBorderBottom(CellStyle.BORDER_THIN);
			style.setBottomBorderColor(new XSSFColor(borderColor));
			style.setBorderTop(CellStyle.BORDER_THIN);
			style.setTopBorderColor(new XSSFColor(borderColor));
			style.setBorderRight(CellStyle.BORDER_THIN);
			style.setRightBorderColor(new XSSFColor(borderColor));
			style.setBorderLeft(CellStyle.BORDER_THIN);
			style.setLeftBorderColor(new XSSFColor(borderColor));
			subjectCellStyles.put(subject, style);
		}

		Entry<String, List<JSONObject>>[] studentDataEntries = studentData.entrySet().toArray(new Entry[] {});
		Arrays.sort(studentDataEntries, Collections.reverseOrder(new Comparator<Entry<String, List<JSONObject>>>() {
			@Override
			public int compare(Entry<String, List<JSONObject>> arg0, Entry<String, List<JSONObject>> arg1) {
				return Integer.compare(arg0.getValue().size(), arg1.getValue().size());
			}
		}));

		for (Entry<String, List<JSONObject>> studentDataEntry : studentDataEntries) {
			Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(studentDataEntry.getKey()));
			Row row = sheet.createRow((short) 0);

			String[] columnNames = {"Name", "Grade", "N", "C", "M", "S"};
			for (int i = 0; i < columnNames.length; i++) {
				String columnName = columnNames[i];
				Cell cell = row.createCell(i);
				cell.setCellValue(columnName);
				cell.setCellStyle(boldStyle);
				CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
			}

			int longestNameLength = 7;
			int rowNum = 1;
			for (JSONObject student : studentDataEntry.getValue()) {
				try {
					row = sheet.createRow((short) rowNum);
					row.createCell(0).setCellValue(student.getString("name"));
					row.createCell(1).setCellValue(student.getInt("grade"));

					for (Subject subject : Subject.values()) {
						String value = student.getBoolean(subject.toString()) ? "" : "X";
						Cell cell = row.createCell(Arrays.asList(columnNames).indexOf(subject.toString()));
						cell.setCellValue(value);
						cell.setCellStyle(subjectCellStyles.get(subject));
					}

					if (student.getString("name").length() > longestNameLength) {
						longestNameLength = student.getString("name").length();
					}

					rowNum++;
				}
				catch (JSONException e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					return;
				}
			}

			sheet.createFreezePane(0, 1, 0, 1);
			// sheet.autoSizeColumn((short) 0); Not supported by App Engine
			sheet.setColumnWidth((short) 0, (int) (256 * longestNameLength * 1.1));
		}

		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("contestTabulation").build();

		File body = new File();
		body.setTitle(docName);
		body.setMimeType("application/vnd.google-apps.spreadsheet");

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		workbook.write(outStream);
		ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
		InputStreamContent content = new InputStreamContent("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", inStream);

		drive.files().insert(body, content).execute();
		workbook.close();
	}
}
