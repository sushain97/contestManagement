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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.BaseHttpServlet;
import util.Pair;
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
public class PublicResults extends BaseHttpServlet
{
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
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		if(!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1"))
			resp.sendRedirect("/?refresh=1");
		
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
		
		Entity contestInfo = infoAndCookie.x;
		if(contestInfo != null)
		{
			if(contestInfo.getProperty("testsGraded") != null)
			{
				String testsGradedString = (String) contestInfo.getProperty("testsGraded");
				String[] testsGraded = testsGradedString.substring(1, testsGradedString.length() - 1).split(",");
				for(int i = 0; i < testsGraded.length; i++)
					testsGraded[i] = testsGraded[i].trim().toUpperCase();
				context.put("testsGraded", testsGraded);
				context.put("Test", Test.class);
			}
			
			Object complete = contestInfo.getProperty("complete");
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
						Query query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
						{
							context.put("hideFullNames", contestInfo.getProperty("hideFullNames"));
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
						}
					}
					else
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, types[1]);
						Filter testFilter = new FilterPredicate("test", FilterOperator.EQUAL, types[2]);
						Filter filter = CompositeFilterOperator.and(CompositeFilterOperator.and(typeFilter, levelFilter), testFilter);
						Query query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
						{
							context.put("hideFullNames", contestInfo.getProperty("hideFullNames"));
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
						}
					}
				}
				else
					context.put("overview", true);

				context.put("date", contestInfo.getProperty("updated"));
			}
			else
				context.put("complete", false);
		}
		else
			context.put("complete", false);

		close(context, ve.getTemplate("publicResults.html"), resp);
	}
}