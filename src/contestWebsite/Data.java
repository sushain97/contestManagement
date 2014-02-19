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
import util.Retrieve;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

import contestTabulation.Level;
import contestTabulation.School;
import contestTabulation.Subject;
import contestTabulation.Test;

@SuppressWarnings("serial")
public class Data extends BaseHttpServlet {
	@Override
	@SuppressWarnings("deprecation")
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
				return;
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

				Map<String, Integer> awardCriteria = Retrieve.awardCriteria(infoAndCookie.x);

				String type = req.getParameter("type");
				if (type != null) {
					context.put("type", type);
					String[] types = type.split("_");
					Level level = Level.fromString(req.getParameter("level"));
					context.put("level", level.toString());
					context.put("tests", Test.getTests(level));

					if (type.equals("students")) {
						context.put("subjects", Subject.values());
						context.put("students", Retrieve.allStudents(level));
					}
					else if (type.startsWith("school_")) {
						Pair<School, Pair<Map<Test, List<Integer>>, Map<Test, List<Integer>>>> schoolAndStats = Retrieve.schoolOverview(types[1]);
						context.put("school", schoolAndStats.x);
						context.put("summaryStats", schoolAndStats.y.x);
						context.put("outliers", schoolAndStats.y.y);
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
						Pair<Map<Test, List<Integer>>, Map<Test, List<Integer>>> statsAndOutliers = Retrieve.visualizations(level);
						context.put("summaryStats", statsAndOutliers.x);
						context.put("outliers", statsAndOutliers.y);
					}
					else {
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
						return;
					}
				}
				else {
					context.put("type", "overview");
				}

				PersistenceManager pm = PMF.get().getPersistenceManager();
				javax.jdo.Query q = pm.newQuery("select name from " + School.class.getName());
				q.setFilter("level == :schoolLevel");
				context.put("middleSchools", q.execute(Level.MIDDLE));
				context.put("highSchools", q.execute(Level.HIGH));

				context.put("hideFullNames", false);
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
