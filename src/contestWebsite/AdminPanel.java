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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.yaml.snakeyaml.Yaml;

import util.BaseHttpServlet;
import util.Pair;
import util.Password;
import util.Retrieve;
import util.UserCookie;

import com.google.api.client.util.Charsets;
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
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.common.io.CharStreams;

import contestTabulation.Level;
import contestTabulation.Subject;
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

			context.put("middleSubjects", Test.getTests(Level.MIDDLE));
			context.put("Level", Level.class);
			context.put("subjects", Subject.values());

			String[] defaultEmails = {"forgotPass", "question", "registration"};
			for (String defaultEmail : defaultEmails) {
				String email;
				if (contestInfo.hasProperty(defaultEmail + "Email")) {
					email = ((Text) contestInfo.getProperty(defaultEmail + "Email")).getValue();
				}
				else {
					InputStream emailStream = getServletContext().getResourceAsStream("/html/email/" + defaultEmail + ".html");
					email = CharStreams.toString(new InputStreamReader(emailStream, Charsets.UTF_8));
					emailStream.close();
				}
				context.put(defaultEmail + "Email", email);
			}

			try {
				context.put("awardCriteria", Retrieve.awardCriteria(contestInfo));
				context.put("qualifyingCriteria", Retrieve.qualifyingCriteria(contestInfo));
				context.put("clientId", contestInfo.getProperty("OAuth2ClientId"));
			}
			catch (Exception e) {
				System.err.println("Surpressing exception while loading admin panel");
				e.printStackTrace();
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+6"));

			try {
				Date endDate = dateFormat.parse((String) contestInfo.getProperty("editEndDate"));
				Date startDate = dateFormat.parse((String) contestInfo.getProperty("editStartDate"));
				if (new Date().after(endDate) || new Date().before(startDate)) {
					context.put("regEditClosed", true);
				}
			}
			catch (Exception e) {
				context.put("regEditClosed", true);
			}

			try {
				Date endDate = dateFormat.parse((String) contestInfo.getProperty("endDate"));
				Date startDate = dateFormat.parse((String) contestInfo.getProperty("startDate"));
				if (new Date().after(endDate) || new Date().before(startDate)) {
					context.put("regClosed", true);
				}
			}
			catch (Exception e) {
				context.put("regClosed", true);
			}

			MemcacheService memCache = MemcacheServiceFactory.getMemcacheService();
			memCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(java.util.logging.Level.INFO));
			byte[] tabulationTaskStatusBytes = (byte[]) memCache.get("tabulationTaskStatus");
			if (tabulationTaskStatusBytes != null) {
				String[] tabulationTaskStatus = new String(tabulationTaskStatusBytes).split("_");
				context.put("tabulationTaskStatus", tabulationTaskStatus[0]);
				List<String> tabulationTaskStatusTime = new ArrayList<String>();
				long timeAgo = new Date().getTime() - new Date(Long.parseLong(tabulationTaskStatus[1])).getTime();
				List<Pair<TimeUnit, String>> timeUnits = new ArrayList<Pair<TimeUnit, String>>() {
					{
						add(new Pair<TimeUnit, String>(TimeUnit.DAYS, "day"));
						add(new Pair<TimeUnit, String>(TimeUnit.HOURS, "hour"));
						add(new Pair<TimeUnit, String>(TimeUnit.MINUTES, "minute"));
						add(new Pair<TimeUnit, String>(TimeUnit.SECONDS, "second"));
					}
				};
				for (Pair<TimeUnit, String> entry : timeUnits) {
					if (entry.getX().convert(timeAgo, TimeUnit.MILLISECONDS) > 0) {
						long numUnit = entry.getX().convert(timeAgo, TimeUnit.MILLISECONDS);
						tabulationTaskStatusTime.add(numUnit + " " + entry.getY() + (numUnit == 1 ? "" : "s"));
						timeAgo -= TimeUnit.MILLISECONDS.convert(numUnit, entry.getX());
					}
				}
				if (tabulationTaskStatusTime.size() >= 1) {
					context.put("tabulationTaskStatusTime", StringUtils.join(tabulationTaskStatusTime, ", "));
				}
				else {
					context.put("tabulationTaskStatusTime", timeAgo + " milliseconds");
				}
			}

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

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try {
				Entity info = Retrieve.contestInfo();
				Entity contestInfo = info != null ? info : new Entity("contestInfo");
				String view = params.get("view")[0];

				boolean testingMode;
				if (view.equals("general")) {
					testingMode = params.get("testing") != null && !params.containsKey("changePass");
				}
				else if (contestInfo.hasProperty("testingMode")) {
					testingMode = (boolean) contestInfo.getProperty("testingMode");
				}
				else {
					testingMode = false;
				}

				String[] stringPropNames = {}, textPropNames = {};

				if (view.equals("general")) {
					contestInfo.setProperty("levels", StringUtils.join(params.get("levels"), "+"));
					contestInfo.setProperty("price", Integer.parseInt(params.get("price")[0]));
					contestInfo.setProperty("testingMode", testingMode);
					contestInfo.setProperty("complete", params.get("complete") != null);
					contestInfo.setProperty("hideFullNames", params.get("fullnames") != null);

					stringPropNames = new String[] {"title", "endDate", "startDate", "editStartDate", "editEndDate", "classificationQuestion",
							"testDownloadURL"};
				}
				else if (view.equals("tabulation")) {
					for (Level level : Level.values()) {
						String[] docNames = params.get("doc" + level.getName());
						if (docNames != null) {
							contestInfo.setProperty("doc" + level.getName(), docNames[0]);
						}

						String[] schoolGroupsParam = params.get(level.toString() + "SchoolGroups");
						if (schoolGroupsParam != null) {
							try {
								Yaml yaml = new Yaml();
								Map<String, List<String>> schoolGroups = (Map<String, List<String>>) yaml.load(schoolGroupsParam[0]);
								Map<String, String> schoolGroupNames = new HashMap<String, String>();
								if (schoolGroups == null) {
									schoolGroups = new HashMap<String, List<String>>();
								}
								for (Entry<String, List<String>> schoolGroupEntry : schoolGroups.entrySet()) {
									for (String school : schoolGroupEntry.getValue()) {
										schoolGroupNames.put(school, schoolGroupEntry.getKey());
									}
								}
								contestInfo.setProperty(level.toString() + "SchoolGroups", new Text(schoolGroupsParam[0]));
								contestInfo.setProperty(level.toString() + "SchoolGroupsNames", new Text(new Yaml().dump(schoolGroupNames)));
							}
							catch (Exception e) {
								e.printStackTrace();
								resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
								return;
							}
						}
					}

					for (Subject subject : Subject.values()) {
						String[] subjectColors = params.get("color" + subject.getName());
						if (subjectColors != null) {
							contestInfo.setProperty("color" + subject.getName(), subjectColors[0]);
						}
					}

					if (params.get("submitType")[0].equals("enqueueTabulationTask")) {
						Queue queue = QueueFactory.getDefaultQueue();
						TaskOptions options = withUrl("/tabulate").retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));

						for (Level level : Level.values()) {
							String[] docNames = params.get("doc" + level.getName());
							if (docNames != null) {
								options.param("doc" + level.getName(), docNames[0]);
							}
						}

						queue.add(options);
					}
				}
				else if (view.equals("content")) {
					GeoPt location = new GeoPt(Float.parseFloat(params.get("location_lat")[0]), Float.parseFloat(params.get("location_long")[0]));
					contestInfo.setProperty("location", location);

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
						ArrayList<ArrayList<String>> list = (ArrayList<ArrayList<String>>) yaml.load(slideshowText);
						contestInfo.setProperty("slideshow", new Text(slideshowText));
					}
					catch (Exception e) {
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
						return;
					}

					stringPropNames = new String[] {"school", "address"};
					textPropNames = new String[] {"aboutText"};
				}
				else if (view.equals("awards")) {
					JSONObject awardCriteria = new JSONObject(), qualifyingCriteria = new JSONObject();;
					for (Entry<String, String[]> entry : params.entrySet()) {
						if (entry.getKey().startsWith("counts_")) {
							awardCriteria.put(entry.getKey().replace("counts_", ""), Integer.parseInt(entry.getValue()[0]));
						}
						else if (entry.getKey().startsWith("qualifying_")) {
							if (!entry.getValue()[0].isEmpty()) {
								qualifyingCriteria.put(entry.getKey().replace("qualifying_", ""), Integer.parseInt(entry.getValue()[0]));
							}
						}
					}
					contestInfo.setProperty("awardCriteria", new Text(awardCriteria.toString()));
					contestInfo.setProperty("qualifyingCriteria", new Text(qualifyingCriteria.toString()));
				}
				else if (view.equals("emails")) {
					stringPropNames = new String[] {"email", "account"};
					textPropNames = new String[] {"forgotPassEmail", "questionEmail", "registrationEmail"};
				}
				else if (view.equals("apis")) {
					stringPropNames = new String[] {"OAuth2ClientSecret", "OAuth2ClientId", "siteVerification", "publicKey", "privateKey"};
					textPropNames = new String[] {"googleAnalytics"};
				}

				for (String propName : stringPropNames) {
					contestInfo.setProperty(propName, params.get(propName)[0]);
				}

				for (String propName : textPropNames) {
					contestInfo.setProperty(propName, new Text(params.get(propName)[0]));
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
