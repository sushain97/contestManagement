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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;

import util.BaseHttpServlet;
import util.Pair;
import util.Retrieve;
import util.Statistics;
import util.UserCookie;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.labs.repackaged.org.json.JSONException;

import contestTabulation.Level;
import contestTabulation.School;
import contestTabulation.Student;
import contestTabulation.Subject;
import contestTabulation.Test;

@SuppressWarnings("serial")
public class PublicResults extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets, html/templates");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		Map<String, Integer> awardCriteria = Retrieve.awardCriteria(infoAndCookie.x);

		if (!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1")) {
			resp.sendRedirect("/?refresh=1");
		}

		Entity contestInfo = infoAndCookie.x;

		context.put("testsGradedNums", contestInfo.hasProperty("testsGradedNums") && contestInfo.getProperty("testsGradedNums") != null ? ((Text) contestInfo.getProperty("testsGradedNums")).getValue()
				: "{}");
		if (contestInfo.hasProperty("testsGraded") && contestInfo.getProperty("testsGraded") != null) {
			context.put("testsGraded", contestInfo.getProperty("testsGraded"));
		}

		Object complete = contestInfo.getProperty("complete");
		if (complete != null && (Boolean) complete || loggedIn && userCookie.isAdmin()) {
			context.put("complete", true);

			String type = req.getParameter("type");
			context.put("type", type);

			if (type != null) {
				String[] types = type.split("_");

				String levelString = req.getParameter("level");
				Level level;
				try {
					level = Level.fromString(levelString);
				}
				catch (IllegalArgumentException e) {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid level: " + levelString);
					return;
				}

				context.put("level", level.toString());
				context.put("tests", Test.getTests(level));
				context.put("Test", Test.class);

				if (type.startsWith("category_")) {
					context.put("test", Test.fromString(types[1]));
					context.put("trophy", awardCriteria.get("category_" + level + "_trophy"));
					context.put("medal", awardCriteria.get("category_" + level + "_medal"));
					context.put("winners", Retrieve.categoryWinners(types[1], level));
				}
				else if (type.startsWith("qualifying_")) {
					context.put("School", School.class);
					Pair<School, List<Student>> schoolAndStudents = Retrieve.schoolStudents(types[1], level);
					context.put("school", schoolAndStudents.x);
					context.put("students", schoolAndStudents.y);
				}
				else if (type.startsWith("categorySweep")) {
					context.put("trophy", awardCriteria.get("categorySweep_" + level));
					context.put("winners", Retrieve.categorySweepstakesWinners(level));
				}
				else if (type.equals("sweep")) {
					context.put("trophy", awardCriteria.get("sweepstakes_" + level));
					context.put("winners", Retrieve.sweepstakesWinners(level));
				}
				else if (type.equals("visualizations")) {
					Map<Test, Statistics> statistics;
					try {
						statistics = Retrieve.visualizations(level);
					}
					catch (JSONException e) {
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
						e.printStackTrace();
						return;
					}

					context.put("statistics", statistics);
				}
				else {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid type: " + type);
					return;
				}
			}
			else {
				context.put("type", "avail");
			}
		}
		else {
			context.put("complete", false);
		}

		Map<Level, List<String>> schools = new HashMap<Level, List<String>>();
		for (Level level : Level.values()) {
			schools.put(level, Retrieve.schoolNames(level));
		}
		context.put("schools", schools);

		context.put("qualifyingCriteria", Retrieve.qualifyingCriteria(infoAndCookie.x));
		context.put("hideFullNames", contestInfo.getProperty("hideFullNames"));
		context.put("date", contestInfo.getProperty("updated"));
		context.put("subjects", Subject.values());
		context.put("Level", Level.class);
		context.put("levels", Level.values());
		context.put("esc", new EscapeTool());

		close(context, ve.getTemplate("publicResults.html"), resp);
	}
}
