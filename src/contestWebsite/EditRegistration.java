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

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.HTMLCompressor;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

@SuppressWarnings("serial")
public class EditRegistration extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		Template t = ve.getTemplate("editRegistration.html");
		VelocityContext context = new VelocityContext();

		context.put("year", Calendar.getInstance().get(Calendar.YEAR));

		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();

		if(loggedIn && userCookie.isAdmin())
		{
			context.put("user", userCookie.getUsername());
			context.put("admin", true);
			context.put("loggedIn", loggedIn);

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Key key = KeyFactory.createKey("registration", Long.parseLong(req.getParameter("key")));
			try
			{
				Entity registration = datastore.get(key);
				Map<String, Object> props = registration.getProperties();

				String registrationType = (String) props.get("registrationType");
				if(registrationType.equals("coach"))
					context.put("coach", true);
				else
					context.put("student", true);

				String schoolLevel = (String) props.get("schoolLevel");
				if(schoolLevel.equals("middle"))
					context.put("middle", true);
				else
					context.put("high", true);

				String[] subjects = {"N", "C", "M", "S"};
				String[] numbers = { "", "one", "two", "three", "four", "five", "six", "seven",
						"eight", "nine", "ten", "eleven", "twelve" };

				for(int i = 6; i <= 12; i++)
					for(int j = 0; j < 4; j++)
						context.put(numbers[i] + subjects[j], props.get(i + subjects[j].toLowerCase()));

				if(schoolLevel.equals("middle"))
					for(int i = 9; i <= 12; i++)
						for(int j = 0; j < 4; j++)
							context.put(numbers[i] + subjects[j], 0);
				else
					for(int i = 6; i <= 8; i++)
						for(int j = 0; j < 4; j++)
							context.put(numbers[i] + subjects[j], 0);

				String account = (String) props.get("account");
				if(account.equals("yes"))
					context.put("account", true);
				else
					context.put("account", false);

				context.put("schoolName", props.get("schoolName"));
				context.put("aliases", props.get("aliases"));
				context.put("name", props.get("name"));
				context.put("email", props.get("email"));
				context.put("paid", props.get("paid"));

				Query query = new Query("contestInfo");
				List<Entity> infos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
				if(infos.size() != 0)
				{
					Entity info = infos.get(0);
					if(info.getProperty("price") != null)
						context.put("price", (Long) info.getProperty("price"));
					else
						context.put("price", 5);
				}
				else
					context.put("price", 5);

				context.put("key", key);
				StringWriter sw = new StringWriter();
				t.merge(context, sw);
				sw.close();
				resp.setContentType("text/html");
				resp.setHeader("X-Frame-Options", "SAMEORIGIN");
				resp.getWriter().print(HTMLCompressor.customCompress(sw));
			}
			catch(EntityNotFoundException e)
			{ 
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			}
		}
		else
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(loggedIn && userCookie.isAdmin())
		{
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			Key key = KeyFactory.createKey("registration", Long.parseLong(req.getParameter("key")));
			try
			{
				Entity registration = datastore.get(key);
				Map<String, String[]> params = new HashMap<String, String[]>(req.getParameterMap());
				for(Entry<String, String[]> param : params.entrySet())
					param.setValue(new String[] { escapeHtml4(param.getValue()[0]) });
				
				if(params.get("ajax") != null && "1".equals(params.get("ajax")[0]))
				{
					String newValue = params.get("newValue")[0];
					String modified = params.get("modified")[0];
					if("yes".equals(params.get("account")[0]) && ("email".equals(modified) || "schoolName".equals(modified) || "name".equals(modified)))
					{
						Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
						Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
						switch(modified)
						{
							case "email":
								user.setProperty("user-id", newValue);
								break;
							case "schoolName":
								user.setProperty("school", newValue);
								break;
							case "name":
								user.setProperty("name", newValue);
						}
						datastore.put(user);
						
						registration.setProperty(modified, newValue);
					}
					else
					{
						if("true".equals(params.get("test")[0]))
						{
							registration.setProperty(modified, Integer.parseInt(newValue));
							long cost = 0;
							long price = 5;
							
							Query query = new Query("contestInfo");
							List<Entity> infos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
							if(infos.size() != 0 && infos.get(0).getProperty("price") != null)
								price = (Long) infos.get(0).getProperty("price");

							String[] subjects = {"n", "c", "m", "s"};
							if(registration.getProperty("schoolLevel").equals("middle"))
								for(int i = 6; i <= 8; i++)
									for(int j = 0; j < 4; j++)
									{
										Object num = registration.getProperty(i + subjects[j]);
										cost += (num instanceof Long ? (Long) num : (Integer) num) * price;
									}
							else
								for(int i = 9; i <= 12; i++)
									for(int j = 0; j < 4; j++)
									{
										Object num = registration.getProperty(i + subjects[j]);
										cost += (num instanceof Long ? (Long) num : (Integer) num) * price;
									}
							registration.setProperty("cost", cost);
						}
						else
							registration.setProperty(modified, newValue);
					}
					datastore.put(registration);
					txn.commit();
				}
				else
				{
					if(params.containsKey("delete"))
					{
						if(registration.getProperty("account").equals("yes"))
						{
							Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
							Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
							datastore.delete(user.getKey());
						}
						datastore.delete(registration.getKey()); //TODO: Do not completely delete
						txn.commit();
						resp.sendRedirect("/data?choice=registrations&updated=1");
					}
					else
					{
						String schoolLevel = params.get("schoolLevel")[0];
						String name = params.get("name")[0];
						String schoolName = params.get("schoolName")[0];
						String email = params.get("email")[0];

						String account = params.get("account")[0];
						if(registration.getProperty("account").equals("yes") && account.equals("no"))
						{
							registration.setProperty("account", "no");
							Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
							Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
							datastore.delete(user.getKey());
						}
						else if(registration.getProperty("account").equals("yes"))
						{
							Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
							Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
							user.setProperty("name", name);
							user.setProperty("school", schoolName);
							user.setProperty("schoolLevel", schoolLevel);
							user.setProperty("user-id", email);
							datastore.put(user);
						}

						registration.setProperty("registrationType", params.get("registrationType")[0]);
						registration.setProperty("schoolName", schoolName);
						registration.setProperty("schoolLevel", schoolLevel);
						registration.setProperty("name", name);
						registration.setProperty("email", email);
						registration.setProperty("cost", Integer.parseInt(params.get("cost")[0]));
						registration.setProperty("paid", params.get("paid")[0]);
						if(!params.get("aliases")[0].equals("$aliases"))
							registration.setProperty("aliases", params.get("aliases")[0]);

						
						String[] subjects = {"n", "c", "m", "s"};
						if(schoolLevel.equals("middle"))
							for(int i = 6; i <= 8; i++)
								for(int j = 0; j < 4; j++)
									registration.setProperty(i + subjects[j], new Integer(Integer.parseInt(params.get(i + subjects[j])[0])));
						else
							for(int i = 9; i <= 12; i++)
								for(int j = 0; j < 4; j++)
									registration.setProperty(i + subjects[j], new Integer(Integer.parseInt(params.get(i + subjects[j])[0])));

						datastore.put(registration);
						txn.commit();
					}

					resp.sendRedirect("/data?choice=registrations&updated=1");
				}
			}
			catch(Exception e)
			{ 
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			}
			finally
			{
				if(txn.isActive())
					txn.rollback();
			}
		}
		else
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
	}
}