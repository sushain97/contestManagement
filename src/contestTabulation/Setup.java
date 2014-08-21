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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.BaseHttpServlet;
import util.Retrieve;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
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
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

@SuppressWarnings("serial")
public class Setup extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Queue queue = QueueFactory.getDefaultQueue();

		if (req.getParameterMap().containsKey("docMiddle")) {
			queue.add(withUrl("/createSpreadsheet").param("docMiddle", req.getParameter("docMiddle")));
		}
		else if (req.getParameterMap().containsKey("docHigh")) {
			queue.add(withUrl("/createSpreadsheet").param("docHigh", req.getParameter("docHigh")));
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Spreadsheet creation request must have paramater 'docMiddle' or 'docHigh' set");
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

		String docName, level;
		if (req.getParameterMap().containsKey("docMiddle")) {
			docName = req.getParameter("docMiddle");
			level = Level.MIDDLE.toString();
		}
		else if (req.getParameterMap().containsKey("docHigh")) {
			docName = req.getParameter("docHigh");
			level = Level.HIGH.toString();
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Spreadsheet creation request must have paramater 'docMiddle' or 'docHigh' set");
			return;
		}

		Query query = new Query("registration")
			.setFilter(new FilterPredicate("schoolLevel", FilterOperator.EQUAL, level))
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

		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("contestTabulation").build();

		File body = new File();
		body.setTitle(docName);
		body.setMimeType("application/vnd.google-apps.spreadsheet");
		File file = drive.files().insert(body).execute();

		SpreadsheetService service = new SpreadsheetService("contestTabulation");
		service.setOAuth2Credentials(credential);

		try {
			SpreadsheetEntry spreadsheet = service.getEntry(new URL("https://spreadsheets.google.com/feeds/spreadsheets/" + file.getId()), SpreadsheetEntry.class);

			for (Entry<String, List<JSONObject>> studentDataEntry : studentData.entrySet()) {
				WorksheetEntry worksheet = new WorksheetEntry();
				worksheet.setColCount(6);
				worksheet.setRowCount(1);
				worksheet.setTitle(new PlainTextConstruct(studentDataEntry.getKey()));
				URL worksheetFeedUrl = spreadsheet.getWorksheetFeedUrl();
				worksheet = service.insert(worksheetFeedUrl, worksheet);

				URL cellFeedUrl = worksheet.getCellFeedUrl();
				CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

				String[] columnNames = {"Name", "Grade", "N", "C", "M", "S"};
				for (int i = 0; i < columnNames.length; i++) {
					cellFeed.insert(new CellEntry(1, i + 1, columnNames[i]));
				}

				URL listFeedUrl = worksheet.getListFeedUrl();

				for (JSONObject student : studentDataEntry.getValue()) {
					try {
						ListEntry row = new ListEntry();
						row.getCustomElements().setValueLocal("name", student.getString("name"));
						row.getCustomElements().setValueLocal("grade", Integer.toString(student.getInt("grade")));

						for (Subject subject : Subject.values()) {
							row.getCustomElements().setValueLocal(subject.toString(), student.getBoolean(subject.toString()) ? "" : "X");
						}

						row = service.insert(listFeedUrl, row);
					}
					catch (JSONException e) {
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
						return;
					}
				}
			}

			WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
			worksheetFeed.getEntries().get(0).delete();
		}
		catch (ServiceException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			return;
		}
	}
}
