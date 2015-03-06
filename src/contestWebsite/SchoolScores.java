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

package contestWebsite;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;
import org.yaml.snakeyaml.Yaml;

import util.BaseHttpServlet;
import util.Pair;
import util.Retrieve;
import util.Statistics;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;

import contestTabulation.Level;
import contestTabulation.School;
import contestTabulation.Subject;
import contestTabulation.Test;

@SuppressWarnings("serial")
public class SchoolScores extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets, html/templates");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		Entity user = userCookie != null ? userCookie.authenticateUser() : null;
		boolean loggedIn = (boolean) context.get("loggedIn");

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		if (loggedIn && !userCookie.isAdmin()) {
			context.put("user", user.getProperty("user-id"));
			context.put("name", user.getProperty("name"));

			Level level = Level.fromString((String) user.getProperty("schoolLevel"));
			context.put("levels", Level.values());
			context.put("level", level.toString());
			context.put("tests", Test.getTests(level));
			context.put("subjects", Subject.values());
			context.put("Test", Test.class);
			context.put("Level", Level.class);

			Query query = new Query("registration").setFilter(new FilterPredicate("email", FilterOperator.EQUAL, user.getProperty("user-id")));
			List<Entity> registration = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			context.put("coach", !registration.isEmpty() && registration.get(0).getProperty("registrationType").equals("coach"));

			Pair<School, Map<Test, Statistics>> schoolAndStats = Retrieve.schoolOverview((String) user.getProperty("school"), level);
			if (schoolAndStats != null) {
				context.put("school", schoolAndStats.x);
				context.put("statistics", schoolAndStats.y);
			}

			if (!user.getProperty("school").equals(schoolAndStats.x.getName())) {
				Map<String, List<String>> schoolGroups = (Map<String, List<String>>) new Yaml().load(((Text) infoAndCookie.x.getProperty(level.toString() + "SchoolGroups")).getValue());
				context.put("schoolGroup", schoolGroups.get(schoolAndStats.x.getName()));
			}

			context.put("qualifyingCriteria", Retrieve.qualifyingCriteria(infoAndCookie.x));
			context.put("date", infoAndCookie.x.getProperty("updated"));
			context.put("esc", new EscapeTool());
			context.put("hideFullNames", false);

			close(context, ve.getTemplate("schoolScores.html"), resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account required for that operation");
		}
	}
}
