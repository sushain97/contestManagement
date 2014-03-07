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

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.yaml.snakeyaml.Yaml;

import util.BaseHttpServlet;
import util.Pair;
import util.Password;
import util.Retrieve;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import contestTabulation.Level;
import contestTabulation.Test;

@SuppressWarnings("serial")
public class AdminPanel extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		String updated = req.getParameter("updated");
		if (updated != null && updated.equals("1") && !loggedIn) {
			resp.sendRedirect("/adminPanel?updated=1");
		}
		context.put("updated", req.getParameter("updated"));

		if (loggedIn && userCookie.isAdmin()) {
			Entity contestInfo = infoAndCookie.x;
			context.put("contestInfo", contestInfo);

			String confPassError = req.getParameter("confPassError");
			context.put("confPassError", confPassError != null && confPassError.equals("1") ? "Those passwords didn't match, try again." : null);
			String passError = req.getParameter("passError");
			context.put("passError", passError != null && passError.equals("1") ? "That password is incorrect, try again." : null);

			context.put("awardCriteria", Retrieve.awardCriteria(contestInfo));
			context.put("qualifyingCriteria", Retrieve.qualifyingCriteria(contestInfo));
			context.put("clientId", contestInfo.getProperty("OAuth2ClientId"));
			context.put("middleSubjects", Test.getTests(Level.MIDDLE));

			close(context, ve.getTemplate("adminPanel.html"), resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if (loggedIn && userCookie.isAdmin()) {
			Map<String, String[]> params = req.getParameterMap();
			boolean testingMode = params.get("testing") != null && !params.containsKey("changePass");

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try {
				Entity info = Retrieve.contestInfo();
				Entity contestInfo = info != null ? info : new Entity("contestInfo");

				String[] stringPropNames = {"earlyEndDate", "earlyStartDate", "normalStartDate", "normalEndDate", "lateStartDate", "lateEndDate",
						"email", "account", "levels", "title", "publicKey", "privateKey", "school", "address",
						"siteVerification", "OAuth2ClientSecret", "OAuth2ClientId"};
				for (String propName : stringPropNames) {
					contestInfo.setProperty(propName, params.get(propName)[0]);
				}
				contestInfo.setProperty("testingMode", testingMode);
				contestInfo.setProperty("aboutText", new Text(params.get("aboutText")[0]));
				contestInfo.setProperty("googleAnalytics", new Text(params.get("googleAnalytics")[0]));
				contestInfo.setProperty("location", new GeoPt(Float.parseFloat(params.get("location_lat")[0]), Float.parseFloat(params.get("location_long")[0])));
				contestInfo.setProperty("price", Integer.parseInt(params.get("price")[0]));
				contestInfo.setProperty("complete", params.get("complete") != null);
				contestInfo.setProperty("hideFullNames", params.get("fullnames") != null);

				JSONObject awardCriteria = new JSONObject(), qualifyingCriteria = new JSONObject();;
				for (Entry<String, String[]> entry : params.entrySet()) {
					if (entry.getKey().startsWith("counts_")) {
						awardCriteria.put(entry.getKey().replace("counts_", ""), Integer.parseInt(entry.getValue()[0]));
					}
					else if (entry.getKey().startsWith("qualifying_")) {
						qualifyingCriteria.put(entry.getKey().replace("qualifying_", ""), Integer.parseInt(entry.getValue()[0]));
					}
				}
				contestInfo.setProperty("awardCriteria", new Text(awardCriteria.toString()));
				contestInfo.setProperty("qualifyingCriteria", new Text(qualifyingCriteria.toString()));

				Yaml yaml = new Yaml();
				String[] mapPropNames = {"schedule", "directions"};
				for (String propName : mapPropNames) {
					String text = params.get(propName)[0];

					try {
						@SuppressWarnings("unused")
						HashMap<String, String> map = (HashMap<String, String>) yaml.load(text);
						contestInfo.setProperty(propName, new Text(text));
					}
					catch (Exception e) {
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
						return;
					}
				}

				String slideshowText = params.get("slideshow")[0];
				try {
					@SuppressWarnings("unused")
					ArrayList<ArrayList<String>> map = (ArrayList<ArrayList<String>>) yaml.load(slideshowText);
					contestInfo.setProperty("slideshow", new Text(slideshowText));
				}
				catch (Exception e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
					return;
				}

				if (params.containsKey("update")) {
					String docHigh = params.get("docHigh")[0];
					String docMiddle = params.get("docMiddle")[0];

					contestInfo.setProperty("docHigh", docHigh);
					contestInfo.setProperty("docMiddle", docMiddle);

					Queue queue = QueueFactory.getDefaultQueue();
					queue.add(withUrl("/tabulate").param("docMiddle", docMiddle).param("docHigh", docHigh));
				}

				datastore.put(contestInfo);

				Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, "admin"));
				Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
				String hash = (String) user.getProperty("hash");
				String salt = (String) user.getProperty("salt");

				if (testingMode) {
					String newHash = Password.getSaltedHash("password");
					resp.addCookie(new Cookie("user-id", URLEncoder.encode("admin" + "$" + newHash.split("\\$")[1], "UTF-8")));

					user.setProperty("salt", newHash.split("\\$")[0]);
					user.setProperty("hash", newHash.split("\\$")[1]);
					datastore.put(user);
					resp.sendRedirect("/adminPanel?updated=1");
				}
				else if (params.containsKey("changePass")) {
					String curPassword = params.get("curPassword")[0];
					String confPassword = params.get("confPassword")[0];
					String newPassword = params.get("newPassword")[0];

					if (Password.check(curPassword, salt + "$" + hash)) {
						if (confPassword.equals(newPassword)) {
							String newHash = Password.getSaltedHash(newPassword);
							resp.addCookie(new Cookie("user-id", URLEncoder.encode("admin" + "$" + newHash.split("\\$")[1], "UTF-8")));

							user.setProperty("salt", newHash.split("\\$")[0]);
							user.setProperty("hash", newHash.split("\\$")[1]);
							datastore.put(user);
							resp.sendRedirect("/adminPanel?updated=1");
						}
						else {
							resp.sendRedirect("/adminPanel?confPassError=1");
						}
					}
					else {
						resp.sendRedirect("/adminPanel?passError=1");
					}
				}
				else {
					resp.sendRedirect("/adminPanel?updated=1");
				}
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
		else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
	}
}
