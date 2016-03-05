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

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.common.base.Function;

import contestTabulation.Level;
import contestTabulation.School;
import contestTabulation.Student;
import contestTabulation.Subject;
import contestTabulation.Test;
import util.BaseHttpServlet;
import util.Pair;
import util.Retrieve;
import util.Statistics;
import util.UserCookie;

@SuppressWarnings("serial")
public class Data extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets, html/templates");
		ve.init();
		VelocityContext context = new VelocityContext();
		String template = null;

		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		Entity contestInfo = infoAndCookie.x;

		if (!loggedIn || !userCookie.isAdmin()) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
		else {
			String choice = req.getPathInfo();
			if (choice == null) {
				resp.sendRedirect("/data/overview");
				return;
			}
			else if (choice.equals("/overview")) {
				template = "data.html";
			}
			else if (choice.equals("/registrations")) {
				template = "dataRegistrations.html";
				context.put("updated", req.getParameter("updated"));
				context.put("price", contestInfo.getProperty("price"));
				context.put("classificationQuestion", contestInfo.getProperty("classificationQuestion"));
				context.put("dateFormat", new SimpleDateFormat("MMM dd, yyyy hh:mm aa"));
				context.put("Test", Test.class);
				context.put("subjects", Subject.values());
				context.put("levels", Level.values());
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

				Map<Level, List<Entity>> registrations = new HashMap<Level, List<Entity>>();
				for (Level level : Level.values()) {
					Query query = new Query("registration").setFilter(new FilterPredicate("schoolLevel", FilterOperator.EQUAL, level.toString()));
					List<Entity> regs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
					if (regs != null) {
						registrations.put(level, regs);
					}
				}

				context.put("registrations", registrations);

				context.put("regJSONtoList", new Function<Text, List<Map<String, Object>>>() {
					@Override
					public List<Map<String, Object>> apply(Text textJSON) {
						List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

						String rawJSON = unescapeHtml4(textJSON.getValue());
						try {
							JSONArray arrayJSON = new JSONArray(rawJSON);
							for (int i = 0; i < arrayJSON.length(); i++) {
								JSONObject objectJSON = arrayJSON.getJSONObject(i);

								Iterator<String> objectKeys = objectJSON.keys();
								Map<String, Object> map = new HashMap<String, Object>();
								while (objectKeys.hasNext()) {
									String objectKey = objectKeys.next();
									map.put(objectKey, objectJSON.get(objectKey));
								}

								list.add(map);
							}

							return list;
						}
						catch (JSONException e) {
							e.printStackTrace();
						}

						return null;
					}
				});

				context.put("esc", new EscapeTool());
			}
			else if (choice.equals("/questions")) {
				template = "dataQuestions.html";
				context.put("updated", req.getParameter("updated"));
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("feedback").setFilter(new FilterPredicate("resolved", FilterOperator.EQUAL, true));
				List<Entity> resolvedQs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				query = new Query("feedback").setFilter(new FilterPredicate("resolved", FilterOperator.NOT_EQUAL, true));
				List<Entity> unresolvedQs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				context.put("resolvedQs", resolvedQs);
				context.put("unresolvedQs", unresolvedQs);
			}
			else if (choice.equals("/scores")) {
				template = "dataScores.html";

				Map<String, Integer> awardCriteria = Retrieve.awardCriteria(contestInfo);

				String type = req.getParameter("type");
				if (type != null) {
					context.put("type", type);
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

					if (type.equals("students")) {
						context.put("subjects", Subject.values());
						context.put("students", Retrieve.allStudents(level));
						context.put("firstSubject", Subject.values()[0]);
					}
					else if (type.startsWith("qualifying_")) {
						context.put("School", School.class);
						Pair<School, List<Student>> schoolAndStudents = Retrieve.schoolStudents(types[1], level);
						context.put("school", schoolAndStudents.x);
						context.put("students", schoolAndStudents.y);
					}
					else if (type.startsWith("school_")) {
						Pair<School, Map<Test, Statistics>> schoolAndStats = Retrieve.schoolOverview(types[1], level);
						context.put("school", schoolAndStats.x);
						context.put("statistics", schoolAndStats.y);
					}
					else if (type.startsWith("category_")) {
						context.put("test", Test.fromString(types[1]));
						context.put("trophy", awardCriteria.get("category_" + level + "_trophy"));
						context.put("medal", awardCriteria.get("category_" + level + "_medal"));
						context.put("winners", Retrieve.categoryWinners(types[1], level));
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
					context.put("type", "overview");
					context.put("testsGradedNums", contestInfo.hasProperty("testsGradedNums") && contestInfo.getProperty("testsGradedNums") != null
							? ((Text) contestInfo.getProperty("testsGradedNums")).getValue()
							: "{}");
					if (contestInfo.hasProperty("testsGraded") && contestInfo.getProperty("testsGraded") != null) {
						context.put("testsGraded", contestInfo.getProperty("testsGraded"));
					}
				}

				Map<Level, List<String>> schools = new HashMap<Level, List<String>>();
				for (Level level : Level.values()) {
					schools.put(level, Retrieve.schoolNames(level));
				}
				context.put("schools", schools);

				context.put("qualifyingCriteria", Retrieve.qualifyingCriteria(contestInfo));
				context.put("hideFullNames", false);
				context.put("subjects", Subject.values());
				context.put("Level", Level.class);
				context.put("levels", Level.values());
				context.put("date", contestInfo.getProperty("updated"));
				context.put("esc", new EscapeTool());
			}
			else {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid choice: " + choice);
				return;
			}

			close(context, ve.getTemplate(template, "UTF-8"), resp);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if (loggedIn && userCookie.isAdmin()) {
			String choice = req.getPathInfo();
			if (choice == null) {
				resp.sendRedirect("/data/overview");
			}
			else if (choice.equals("/overview")) {
				resp.sendRedirect("/data/overview");
			}
			else if (choice.equals("/registrations")) {
				String edit = req.getParameter("edit");
				if (edit != null) {
					resp.sendRedirect("/editRegistration?key=" + req.getParameter("edit"));
				}
				else {
					resp.sendRedirect("/data/registrations");
				}
			}
			else if (choice.equals("/questions")) {
				Map<String, String[]> params = req.getParameterMap();
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					for (String paramName : params.keySet()) {
						if (!paramName.equals("choice") && !paramName.equals("updated")) {
							Key key = KeyFactory.createKey("feedback", Long.parseLong(paramName));
							String option = params.get(paramName)[0];
							if (option.equals("r")) {
								Entity q = datastore.get(key);
								q.setProperty("resolved", true);
								datastore.put(q);
							}
							else if (option.equals("d")) {
								datastore.delete(key);
							}
							else {
								resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid option (must be 'r' or 'q'): " + option);
							}
						}
					}
					txn.commit();
				}
				catch (Exception e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					return;
				}
				finally {
					if (txn.isActive()) {
						txn.rollback();
					}
				}

				resp.sendRedirect("/data/questions?updated=1");
			}
			else if (choice.equals("/scores")) {
				try {
					JSONArray testsGradedJSON = new JSONArray(req.getParameter("testsGraded"));
					ArrayList<String> testsGraded = new ArrayList<String>();
					for (int i = 0; i < testsGradedJSON.length(); i++) {
						testsGraded.add(testsGradedJSON.get(i).toString());
					}

					DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
					Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
					try {
						Entity contestInfo = Retrieve.contestInfo();
						contestInfo.setProperty("testsGraded", testsGraded);
						datastore.put(contestInfo);
						txn.commit();
					}
					catch (Exception e) {
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					}
					finally {
						if (txn.isActive()) {
							txn.rollback();
						}
					}
				}
				catch (JSONException e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
				}
			}
			else {
				resp.sendRedirect("/data/overview");
			}
		}
		else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
	}
}
