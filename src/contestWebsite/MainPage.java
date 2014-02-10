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
import org.yaml.snakeyaml.Yaml;

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
	@SuppressWarnings({ "deprecation", "unchecked" })
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
		else
		{
			Yaml yaml = new Yaml();
			ArrayList<ArrayList<String>> slideshow = (ArrayList<ArrayList<String>>) yaml.load(((Text) infoAndCookie.x.getProperty("slideshow")).getValue());
			context.put("slideshow", slideshow);
		}
		
		context.put("esc", new EscapeTool());
		context.put("aboutText", ((Text) infoAndCookie.x.getProperty("aboutText")).getValue());

		close(context, ve.getTemplate("main.html"), resp);
	}
}