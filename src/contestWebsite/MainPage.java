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
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;
import org.yaml.snakeyaml.Yaml;

import util.BaseHttpServlet;
import util.Pair;
import util.PropNames;
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
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import contestTabulation.Subject;

@SuppressWarnings("serial")
public class MainPage extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		Entity user = userCookie != null ? userCookie.authenticateUser() : null;
		boolean loggedIn = (boolean) context.get("loggedIn");

		if (!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1")) {
			resp.sendRedirect("/?refresh=1");
		}

		if (loggedIn) {
			if (!userCookie.isAdmin()) {
				String username = (String) user.getProperty("user-id");
				String name = (String) user.getProperty("name");

				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("registration").setFilter(new FilterPredicate("email", FilterOperator.EQUAL, username));
				Entity reg = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
				ArrayList<String> regData = new ArrayList<String>();
				Map<String, Object> props = reg.getProperties();

				for (Entry<String, Object> prop : props.entrySet()) {
					String key = prop.getKey();
					if (!key.equals("account") && !key.equals("cost") && PropNames.names.get(key) != null && !prop.getValue().equals("")) {
						regData.add("<dt>" + PropNames.names.get(key) + "</dt>\n<dd>" + prop.getValue() + "</dd>");
					}
				}

				Entity contestInfo = infoAndCookie.x;
				String endDateStr = (String) contestInfo.getProperty("editEndDate");
				String startDateStr = (String) contestInfo.getProperty("editStartDate");

				Date endDate = new Date();
				Date startDate = new Date();
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
					dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+6"));
					endDate = dateFormat.parse(endDateStr);
					startDate = dateFormat.parse(startDateStr);
				}
				catch (ParseException e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Incorrect date format");
				}

				if (new Date().after(endDate) || new Date().before(startDate)) {
					context.put("regEditClosed", true);
				}

				Collections.sort(regData);
				context.put("regData", regData);
				context.put("level", contestTabulation.Level.fromString(props.get("schoolLevel").toString()));
				context.put("studentData", unescapeHtml4(((Text) props.get("studentData")).getValue()));
				context.put("price", contestInfo.getProperty("price"));
				context.put("name", name);
				context.put("user", user.getProperty("user-id"));
				context.put("updated", req.getParameter("updated"));
			}
			else {
				context.put("error", req.getParameter("error"));
				context.put("user", user.getProperty("user-id"));
				context.put("name", "Contest Administrator");
				context.put("admin", true);
			}
			context.put("complete", infoAndCookie.x.getProperty("complete"));
			context.put("testDownloadURL", infoAndCookie.x.getProperty("testDownloadURL"));
		}
		else {
			Yaml yaml = new Yaml();
			ArrayList<ArrayList<String>> slideshow = (ArrayList<ArrayList<String>>) yaml.load(((Text) infoAndCookie.x.getProperty("slideshow")).getValue());
			context.put("slideshow", slideshow);
		}

		context.put("esc", new EscapeTool());
		context.put("aboutText", ((Text) infoAndCookie.x.getProperty("aboutText")).getValue());
		context.put("siteVerification", infoAndCookie.x.getProperty("siteVerification"));

		close(context, ve.getTemplate("main.html"), resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
			Entity contestInfo = infoAndCookie.x;
			String endDateStr = (String) contestInfo.getProperty("editEndDate");
			String startDateStr = (String) contestInfo.getProperty("editStartDate");

			Date endDate = new Date();
			Date startDate = new Date();
			try {
				endDate = new SimpleDateFormat("MM/dd/yyyy").parse(endDateStr);
				startDate = new SimpleDateFormat("MM/dd/yyyy").parse(startDateStr);
			}
			catch (ParseException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Incorrect date format");
			}

			if (new Date().after(endDate) || new Date().before(startDate)) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Registration editing deadline passed.");
			}
			else {
				Query query = new Query("registration").setFilter(new FilterPredicate("email", FilterOperator.EQUAL, user.getProperty("user-id")));
				Entity registration = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);

				String studentData = req.getParameter("studentData");

				JSONArray regData = null;
				try {
					regData = new JSONArray(studentData);
				}
				catch (JSONException e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					return;
				}

				long price = (Long) infoAndCookie.x.getProperty("price");
				int cost = (int) (0 * price);

				for (int i = 0; i < regData.length(); i++) {
					try {
						JSONObject studentRegData = regData.getJSONObject(i);
						for (Subject subject : Subject.values()) {
							cost += price * (studentRegData.getBoolean(subject.toString()) ? 1 : 0);
						}
					}
					catch (JSONException e) {
						e.printStackTrace();
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
						return;
					}
				}

				registration.setProperty("cost", cost);
				registration.setProperty("studentData", new Text(studentData));

				Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					datastore.put(registration);
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

				resp.sendRedirect("/?updated=1");
			}
		}
		else if (loggedIn && userCookie.isAdmin()) {
			String username = req.getParameter("email").toLowerCase();
			Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, username));
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if (users.size() >= 1) {
				Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					query = new Query("authToken").setKeysOnly();
					Filter tokenFilter = new FilterPredicate("token", FilterOperator.EQUAL, URLDecoder.decode(userCookie.getValue(), "UTF-8"));
					Filter expiredFilter = new FilterPredicate("expires", FilterOperator.LESS_THAN, new Date());
					query.setFilter(CompositeFilterOperator.or(tokenFilter, expiredFilter));
					datastore.delete(datastore.prepare(query).asList(FetchOptions.Builder.withDefaults()).get(0).getKey());

					userCookie.setMaxAge(0);
					userCookie.setValue("");
					resp.addCookie(userCookie);

					SecureRandom random = new SecureRandom();
					String authToken = new BigInteger(130, random).toString(32);
					Entity token = new Entity("authToken");
					token.setProperty("user-id", username);
					token.setProperty("token", authToken);

					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.MINUTE, 60);
					token.setProperty("expires", new Date(calendar.getTimeInMillis()));

					Cookie cookie = new Cookie("authToken", authToken);
					cookie.setValue(authToken);
					resp.addCookie(cookie);

					datastore.put(token);
					datastore.put(user);
					resp.sendRedirect("/");

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
				resp.sendRedirect("/?error=1");
			}
		}
		else {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account required for that operation");
		}
	}
}
