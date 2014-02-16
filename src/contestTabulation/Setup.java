/* Component of GAE Project for TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

package contestTabulation;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.BaseHttpServlet;

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
import com.google.gdata.util.ServiceException;

@SuppressWarnings("serial")
public class Setup extends BaseHttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		Queue queue = QueueFactory.getDefaultQueue();

		if(req.getParameterMap().containsKey("docMiddle"))
			queue.add(withUrl("/createSpreadsheet").param("docMiddle", req.getParameter("docMiddle")));
		else if(req.getParameterMap().containsKey("docHigh"))
			queue.add(withUrl("/createSpreadsheet").param("docHigh", req.getParameter("docHigh")));
		else
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}

	@SuppressWarnings("deprecation")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("contestInfo");
		Entity contestInfo = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);

		GoogleCredential credential = new GoogleCredential.Builder()
			.setJsonFactory(jsonFactory)
			.setTransport(httpTransport)
			.setClientSecrets((String) contestInfo.getProperty("OAuth2ClientId"), (String) contestInfo.getProperty("OAuth2ClientSecret")).build()
			.setFromTokenResponse(new JacksonFactory().fromString(((Text) contestInfo.getProperty("OAuth2Token")).getValue(), GoogleTokenResponse.class));

		String docName, level;
		if(req.getParameterMap().containsKey("docMiddle"))
		{
			docName = req.getParameter("docMiddle");
			level = "middle";
		}
		else if(req.getParameterMap().containsKey("docHigh"))
		{
			docName = req.getParameter("docHigh");
			level = "high";
		}
		else
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("contestTabulation").build();

		File body = new File();
		body.setTitle(docName);
		body.setMimeType("application/vnd.google-apps.spreadsheet");	    
		File file = drive.files().insert(body).execute();

		query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, level).addSort("schoolName", SortDirection.ASCENDING);
		List<Entity> registrations = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		SpreadsheetService service = new SpreadsheetService("contestTabulation");
		service.setOAuth2Credentials(credential);
		SpreadsheetEntry spreadsheet;
		try
		{
			spreadsheet = service.getEntry(new URL("https://spreadsheets.google.com/feeds/spreadsheets/" + file.getId()), SpreadsheetEntry.class);

			String currentSchool = null;
			WorksheetEntry worksheet = null;
			URL listFeedUrl = null;

			for(Entity registration: registrations)
			{
				String schoolName = (String) registration.getProperty("schoolName");
				if(!schoolName.equals(currentSchool))
				{
					currentSchool = schoolName;
					worksheet = new WorksheetEntry();
					worksheet.setColCount(6);
					worksheet.setRowCount(1);
					worksheet.setTitle(new PlainTextConstruct(schoolName));
					URL worksheetFeedUrl = spreadsheet.getWorksheetFeedUrl();
					worksheet = service.insert(worksheetFeedUrl, worksheet);

					URL cellFeedUrl = worksheet.getCellFeedUrl();
					CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

					String[] columnNames = {"Name", "Grade", "N", "C", "M", "S"};
					for(int i = 0; i < columnNames.length; i++)
						cellFeed.insert(new CellEntry(1, i + 1, columnNames[i]));

					listFeedUrl = worksheet.getListFeedUrl();
				}

				String studentDataJSON = unescapeHtml4(((Text) registration.getProperty("studentData")).getValue());

				JSONArray studentData = null;
				try
				{
					studentData = new JSONArray(studentDataJSON);
				}
				catch(JSONException e)
				{	
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					return;
				}

				for(int i = 0; i < studentData.length(); i++)
				{
					try
					{
						JSONObject student = studentData.getJSONObject(i);

						ListEntry row = new ListEntry();
						row.getCustomElements().setValueLocal("name", student.getString("name"));
						row.getCustomElements().setValueLocal("grade", Integer.toString(student.getInt("grade")));

						for(String subject: Test.tests())
							row.getCustomElements().setValueLocal(subject, student.getBoolean(subject) ? "" : "X");

						row = service.insert(listFeedUrl, row);
					}
					catch(JSONException e)
					{
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
						return;
					}
				}
			}
		}
		catch(ServiceException e)
		{
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			return;
		}
	}
}
