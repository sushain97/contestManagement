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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.List;

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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import contestTabulation.Test;

@SuppressWarnings("serial")
public class PublicResults extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		UserCookie userCookie = UserCookie.getCookie(req);
		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		if(!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1"))
			resp.sendRedirect("/?refresh=1");

		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		Template t = ve.getTemplate("publicResults.html");
		VelocityContext context = new VelocityContext();
		
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
		context.put("loggedIn", loggedIn);
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		if(loggedIn)
		{
			context.put("admin", userCookie.isAdmin());
			if(!userCookie.isAdmin())
			{
				String name = (String) user.getProperty("name");
				context.put("name", name);
				context.put("user", user.getProperty("user-id"));
			}
			else
			{
				context.put("user", user.getProperty("user-id"));
				context.put("name", "Contest Administrator");
			}
		}
		Query query = new Query("contestInfo");
		List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		if(info.size() != 0)
		{
			if(info.get(0).getProperty("testsGraded") != null)
			{
				String testsGradedString = (String) info.get(0).getProperty("testsGraded");
				String[] testsGraded = testsGradedString.substring(1, testsGradedString.length() - 1).split(",");
				for(int i = 0; i < testsGraded.length; i++)
					testsGraded[i] = testsGraded[i].trim().toUpperCase();
				context.put("testsGraded", testsGraded);
				context.put("Test", Test.class);
			}
			
			Object complete = info.get(0).getProperty("complete");
			if((complete != null && (Boolean) complete) || (loggedIn && userCookie.isAdmin()))
			{
				context.put("complete", true);
				String type = req.getParameter("type");
				context.put("type", type);
				
				if(type != null)
				{
					String[] types = type.split("_");
					Filter levelFilter = new FilterPredicate("level", FilterOperator.EQUAL, types[0]);
					if(types.length == 2)
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, types[1]);
						Filter filter = CompositeFilterOperator.and(typeFilter, levelFilter);
						query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
						{
							context.put("hideFullNames", info.get(0).getProperty("hideFullNames"));
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
						}
					}
					else
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, types[1]);
						Filter testFilter = new FilterPredicate("test", FilterOperator.EQUAL, types[2]);
						Filter filter = CompositeFilterOperator.and(CompositeFilterOperator.and(typeFilter, levelFilter), testFilter);
						query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
						{
							context.put("hideFullNames", info.get(0).getProperty("hideFullNames"));
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
						}
					}
				}
				else
					context.put("overview", true);

				context.put("date", info.get(0).getProperty("updated"));
			}
			else
				context.put("complete", false);
		}
		else
			context.put("complete", false);

		StringWriter sw = new StringWriter();
		t.merge(context, sw);
		sw.close();
		resp.setContentType("text/html");
		resp.setHeader("X-Frame-Options", "SAMEORIGIN");
		resp.getWriter().print(HTMLCompressor.customCompress(sw));
	}
}