/* Component of GAE Project for Dulles TMSCA Contest Automation
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

package contestWebsite;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;

import util.BaseHttpServlet;
import util.Pair;
import util.PropNames;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import contestTabulation.Test;
@SuppressWarnings("serial")
public class MainPage extends BaseHttpServlet
{
	private final static String[] captions = { "",
		"Bobby S. awarded first place in Math by Larry White",
		"First place: Bobby S., Daniel F., Rik N., Ms. Sarah Cleveland",
		"",
		"",
		"First place: Bobby S., Daniel F., Rik N., Mr. Feng Li",
		"First place: Bobby S., Greg C., Daniel F., Kevin L., Dr. Drew Poche",
		"",
		"",
		"",
		"Graduating Seniors with Ms. Sarah Cleveland",
		"Graduating Seniors with Dr. Drew Poche",
		"Graduating Seniors",
		"Siddarth G., Kevin L., Bobby S., Daniel F., Keerthana K., Rik N., Saiesh K., Sushain C.",
		"Bobby S., Arjun G., Saiesh K., Sushain C., Daniel F., Mitchell H., Siddarth G., Kevin L., Keerthana K., Rik N.",
		"Keerthana K., Kevin L.",
		"Siddarth G., Kevin L., Bobby S.",
		"Roy H., Emily W., Daniel H., Prashant R., Mitchell H., Kaelan Y., Arjun G., Chung H."
	};
	private final static String[] titles = { "TMSCA State Awards: 2013",
		"UIL State Math Individual Awards: 2013",
		"UIL State Math Team Awards: 2013",
		"Texas A&M University Math Competition: 2013",
		"University of Houston Math Competition: 2012",
		"TMSCA State Math Team Awards: 2012",
		"TMSCA State Science Team Awards: 2012",
		"TMSCA State Awards: 2012",
		"Math & Science Club Banquet: 2013",
		"Math & Science Club Banquet: 2013",
		"Math & Science Club Banquet: 2013",
		"Math & Science Club Banquet: 2013",
		"Math & Science Club Banquet: 2013",
		"UIL State Team: 2013",
		"UIL State Team: 2013",
		"Math & Science Club Co-Presidents: 2014",
		"UIL State Team: 2013",
		"UIL State Science Team: 2013",
		"Math & Science Club Officers: 2013"
	};

	@SuppressWarnings("deprecation")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		Entity user = userCookie != null ? userCookie.authenticateUser() : null;
		boolean loggedIn = (boolean) context.get("loggedIn");
		
		if(!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1"))
			resp.sendRedirect("/?refresh=1");
		
		if(loggedIn)
		{
			if(!userCookie.isAdmin())
			{
				String username = (String) user.getProperty("user-id");
				String name = (String) user.getProperty("name");
				
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("registration").addFilter("email", FilterOperator.EQUAL, username);
				Entity reg = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
				ArrayList<String> regData = new ArrayList<String>();
				Map<String, Object> props = reg.getProperties();

				for(Entry<String, Object> prop : props.entrySet())
				{
					String key = prop.getKey();
					if(!key.equals("account") && PropNames.names.get(key) != null && !prop.getValue().equals(""))
						regData.add("<dt>" + PropNames.names.get(key) + "</dt>\n<dd>" + prop.getValue() + "</dd>");
				}
				ArrayList<String> students = new ArrayList<String>();
				JSONArray studentData = null;
				try
				{
					studentData = new JSONArray(unescapeHtml4(((Text) props.get("studentData")).getValue()));
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
						JSONObject studentRegData = studentData.getJSONObject(i);
						
						ArrayList<String> tests = new ArrayList<String>();
						for(String subject: Test.tests())
							if(studentRegData.getBoolean(subject))
								tests.add(Test.letterToName(subject));
						
						students.add("<dt>" + studentRegData.getString("name") + " (" + studentRegData.getInt("grade") + "th)</dt>\n<dd>" + StringUtils.join(tests.toArray(), ", ") + "</dd>");
					}
					catch(JSONException e)
					{
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
						return;
					}
				}
				
				Collections.sort(regData);
				Collections.sort(students);
				context.put("regData", regData);
				context.put("studentData", students);
				context.put("name", name);
				context.put("user", user.getProperty("user-id"));
			}
			else
			{
				context.put("user", user.getProperty("user-id"));
				context.put("name", "Contest Administrator");
				context.put("admin", true);
			}
		}
		
		context.put("num", Math.min(titles.length, captions.length)-2);
		context.put("titles", titles);
		context.put("captions", captions);
		context.put("esc", new EscapeTool());

		close(context, ve.getTemplate("main.html"), resp);
	}
}