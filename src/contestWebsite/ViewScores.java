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

@SuppressWarnings("serial")
public class ViewScores extends HttpServlet
{
	@SuppressWarnings("deprecation")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		Template t = ve.getTemplate("schoolScores.html");
		VelocityContext context = new VelocityContext();
		
		UserCookie userCookie = UserCookie.getCookie(req);
		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
		context.put("loggedIn", loggedIn);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		if(loggedIn && !userCookie.isAdmin())
		{
			context.put("user", user.getProperty("user-id"));
			context.put("name", user.getProperty("name"));

			Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, "school");
			Filter nameFilter = new FilterPredicate("school", FilterOperator.EQUAL, user.getProperty("school"));
			Filter filter = CompositeFilterOperator.and(typeFilter, nameFilter);
			Query query = new Query("html").setFilter(filter);
			List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(html.size() != 0)
				context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());

			query = new Query("contestInfo");
			Entity info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
			context.put("date", info.getProperty("updated"));
			
			query = new Query("registration").addFilter("email", FilterOperator.EQUAL, user.getProperty("user-id"));
			List<Entity> registration = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(registration.size() > 0 && registration.get(0).getProperty("registrationType").equals("coach"))
				context.put("coach", true);

			StringWriter sw = new StringWriter();
			t.merge(context, sw);
			sw.close();
			resp.setContentType("text/html");
			resp.setHeader("X-Frame-Options", "SAMEORIGIN");
			resp.getWriter().print(HTMLCompressor.customCompress(sw));
		}
		else
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account required for that operation");
	}
}