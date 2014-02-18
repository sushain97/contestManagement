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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;

import util.BaseHttpServlet;
import util.PMF;
import util.Pair;
import util.Statistics;
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
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import contestTabulation.Level;
import contestTabulation.School;
import contestTabulation.Score;
import contestTabulation.Student;
import contestTabulation.Subject;
import contestTabulation.Test;

@SuppressWarnings("serial")
public class Data extends BaseHttpServlet {
	@Override
	@SuppressWarnings({"deprecation", "unchecked"})
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets, html/templates");
		ve.init();
		VelocityContext context = new VelocityContext();
		String template = null;

		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		if (!loggedIn || !userCookie.isAdmin()) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
		else {
			String choice = req.getParameter("choice");
			if (choice == null) {
				resp.sendRedirect("/data?choice=overview");
			}
			else if (choice.equals("overview")) {
				template = "data.html";
			}
			else if (choice.equals("registrations")) {
				template = "dataRegistrations.html";
				context.put("updated", req.getParameter("updated"));
				context.put("price", infoAndCookie.x.getProperty("price"));
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, "middle");
				List<Entity> middleRegs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, "high");
				List<Entity> highRegs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				context.put("middleRegs", middleRegs);
				context.put("highRegs", highRegs);
			}
			else if (choice.equals("questions")) {
				template = "dataQuestions.html";
				context.put("updated", req.getParameter("updated"));
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("feedback").addFilter("resolved", FilterOperator.EQUAL, true);
				List<Entity> resolvedQs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				query = new Query("feedback").addFilter("resolved", FilterOperator.NOT_EQUAL, true);
				List<Entity> unresolvedQs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				context.put("resolvedQs", resolvedQs);
				context.put("unresolvedQs", unresolvedQs);
			}
			else if (choice.equals("scores")) {
				template = "dataScores.html";

				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				PersistenceManager pm = PMF.get().getPersistenceManager();

				String type = req.getParameter("type");

				Map<String, Integer> awardCriteria = new HashMap<String, Integer>();
				JSONObject awardCriteriaJSON = null;
				try {
					awardCriteriaJSON = new JSONObject(((Text) infoAndCookie.x.getProperty("awardCriteria")).getValue());
					Iterator<String> awardCountKeyIter = awardCriteriaJSON.keys();
					while (awardCountKeyIter.hasNext()) {
						String awardCountType = awardCountKeyIter.next();
						try {
							awardCriteria.put(awardCountType, (Integer) awardCriteriaJSON.get(awardCountType));
						}
						catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				catch (JSONException e) {
					e.printStackTrace();
				}

				if (type != null) {
					context.put("type", type);
					String[] types = type.split("_");
					Level level = Level.fromString(req.getParameter("level"));
					context.put("level", level.toString());
					context.put("tests", Test.getTests(level));

					if (type.equals("students")) {
						context.put("subjects", Subject.values());

						javax.jdo.Query q = pm.newQuery(Student.class);
						q.setFilter("grade >= :lowGrade && grade <= :highGrade");
						List<Student> students = (List<Student>) q.execute(level.getLowGrade(), level.getHighGrade());
						Collections.sort(students, Student.getNameComparator());
						context.put("students", students);
					}
					else if (type.startsWith("school_")) {
						javax.jdo.Query q = pm.newQuery(School.class);
						q.setFilter("name == :schoolName");
						List<School> schools = (List<School>) q.execute(types[1]);
						if (!schools.isEmpty()) {
							School school = schools.get(0);
							context.put("school", school);

							HashMap<Test, List<Integer>> scores = new HashMap<Test, List<Integer>>();
							for (Test test : Test.getTests(level)) {
								scores.put(test, new ArrayList<Integer>());
							}

							for (Student student : school.getStudents()) {
								for (Entry<Subject, Score> scoreEntry : student.getScores().entrySet()) {
									scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
								}
							}

							HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
							HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
							for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
								Pair<List<Integer>, List<Integer>> stats = Statistics.calculateStats(scoreEntry.getValue());
								summaryStats.put(scoreEntry.getKey(), stats.x);
								outliers.put(scoreEntry.getKey(), stats.y);
							}

							context.put("summaryStats", summaryStats);
							context.put("outliers", outliers);
						}
					}
					else if (type.startsWith("category_")) {
						context.put("test", Test.fromString(types[1]));
						context.put("trophy", awardCriteria.get("category_" + level + "_trophy"));
						context.put("medal", awardCriteria.get("category_" + level + "_medal"));

						try {
							Entity categoryWinnersEntity = datastore.get(KeyFactory.createKey("CategoryWinners", types[1] + "_" + level.toString()));
							List<Key> categoryWinnersKeys = (List<Key>) categoryWinnersEntity.getProperty("students");
							javax.jdo.Query q = pm.newQuery("select from " + Student.class.getName() + " where :keys.contains(key)");
							context.put("winners", q.execute(categoryWinnersKeys));
						}
						catch (EntityNotFoundException e) {
						}
					}
					else if (type.startsWith("categorySweep")) {
						context.put("trophy", awardCriteria.get("categorySweep_" + level));

						List<Key> categorySweepstakesWinnersEntityKeys = new ArrayList<Key>();
						for (Subject subject : Subject.values()) {
							categorySweepstakesWinnersEntityKeys.add(KeyFactory.createKey("CategorySweepstakesWinners", subject + "_" + level.toString()));
						}

						Map<Key, Entity> categorySweepstakesWinnersEntityMap = datastore.get(categorySweepstakesWinnersEntityKeys);

						Map<Subject, List<School>> categorySweepstakesWinners = new HashMap<Subject, List<School>>();
						for (Entry<Key, Entity> categorySweepstakesWinnersEntityEntry : categorySweepstakesWinnersEntityMap.entrySet()) {
							Subject category = Subject.valueOf(categorySweepstakesWinnersEntityEntry.getKey().getName().split("_")[0]);
							List<Key> categorySweepstakesWinnersKeys = (List<Key>) categorySweepstakesWinnersEntityEntry.getValue().getProperty("schools");
							javax.jdo.Query q = pm.newQuery("select from " + School.class.getName() + " where :keys.contains(key)");
							categorySweepstakesWinners.put(category, (List<School>) q.execute(categorySweepstakesWinnersKeys));
						}

						context.put("winners", categorySweepstakesWinners);
					}
					else if (type.equals("sweep")) {
						context.put("trophy", awardCriteria.get("sweepstakes_" + level));

						javax.jdo.Query q = pm.newQuery(School.class);
						q.setOrdering("totalScore desc");
						q.setFilter("level == :schoolLevel");
						context.put("winners", q.execute(level));
					}
					else if (type.equals("visualizations")) {
						List<Key> visualizationKeys = new ArrayList<Key>();
						for (Test test : Test.getTests(level)) {
							visualizationKeys.add(KeyFactory.createKey("Visualization", test.toString()));
						}
						Map<Key, Entity> visualizationEntities = datastore.get(visualizationKeys);

						HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
						HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();

						for (Entry<Key, Entity> visualizationEntry : visualizationEntities.entrySet()) {
							Test test = Test.fromString(visualizationEntry.getKey().getName());
							summaryStats.put(test, (List<Integer>) visualizationEntry.getValue().getProperty("summaryStats"));
							outliers.put(test, (List<Integer>) visualizationEntry.getValue().getProperty("outliers"));
						}

						context.put("summaryStats", summaryStats);
						context.put("outliers", outliers);
					}
				}
				else {
					context.put("type", "overview");
				}

				javax.jdo.Query q = pm.newQuery("select name from " + School.class.getName());
				q.setFilter("level == :schoolLevel");
				context.put("middleSchools", q.execute(Level.MIDDLE));
				context.put("highSchools", q.execute(Level.HIGH));

				context.put("date", infoAndCookie.x.getProperty("updated"));
				context.put("subjects", Subject.values());
				context.put("levels", Level.values());
				context.put("esc", new EscapeTool());
			}
			else {
				resp.sendRedirect("/data?choice=overview");
				return;
			}

			close(context, ve.getTemplate(template), resp);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if (loggedIn && userCookie.isAdmin()) {
			String choice = req.getParameter("choice");
			if (choice == null) {
				resp.sendRedirect("/data?choice=overview");
			}
			else if (choice.equals("overview")) {
				resp.sendRedirect("/data?choice=overview");
			}
			else if (choice.equals("registrations")) {
				String edit = req.getParameter("edit");
				if (edit != null) {
					resp.sendRedirect("/editRegistration?key=" + req.getParameter("edit"));
				}
				else {
					resp.sendRedirect("/data?choice=registrations");
				}
			}
			else if (choice.equals("questions")) {
				Map<String, String[]> params = req.getParameterMap();
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					for (String paramName : params.keySet()) {
						if (!paramName.equals("choice") && !paramName.equals("updated")) {
							Key key = KeyFactory.createKey("feedback", Integer.parseInt(paramName));
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
								throw new IllegalArgumentException();
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

				resp.sendRedirect("/data?choice=questions&updated=1");
			}
			else if (choice.equals("scores")) {
				resp.sendRedirect("/data?choice=overview");
			}
			else {
				resp.sendRedirect("/data?choice=overview");
			}
		}
		else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
	}
}
