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

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

@SuppressWarnings("serial")
public class EditRegistration extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		if (loggedIn && userCookie.isAdmin()) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Key key = KeyFactory.createKey("registration", Long.parseLong(req.getParameter("key")));
			try {
				Entity registration = datastore.get(key);
				Map<String, Object> props = registration.getProperties();

				String registrationType = (String) props.get("registrationType");
				if (registrationType.equals("coach")) {
					context.put("coach", true);
				}
				else {
					context.put("student", true);
				}

				String schoolLevel = (String) props.get("schoolLevel");
				if (schoolLevel.equals("middle")) {
					context.put("middle", true);
				}
				else {
					context.put("high", true);
				}

				String account = (String) props.get("account");
				if (account.equals("yes")) {
					context.put("account", true);
				}
				else {
					context.put("account", false);
				}

				context.put("schoolName", props.get("schoolName"));
				context.put("name", props.get("name"));
				context.put("email", props.get("email"));
				context.put("paid", props.get("paid"));
				context.put("division", props.get("division"));
				context.put("studentData", ((Text) props.get("studentData")).getValue());

				Entity contestInfo = infoAndCookie.x;
				context.put("price", contestInfo.getProperty("price"));
				context.put("key", key);
				context.put("levels", contestInfo.getProperty("levels"));

				close(context, ve.getTemplate("editRegistration.html"), resp);
			}
			catch (EntityNotFoundException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			}
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
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			Key key = KeyFactory.createKey("registration", Long.parseLong(req.getParameter("key")));
			try {
				Entity registration = datastore.get(key);
				Map<String, String[]> params = new HashMap<String, String[]>(req.getParameterMap());
				for (Entry<String, String[]> param : params.entrySet()) {
					param.setValue(new String[] {escapeHtml4(param.getValue()[0])});
				}

				if (params.get("ajax") != null && "1".equals(params.get("ajax")[0])) {
					String newValue = params.get("newValue")[0];
					String modified = params.get("modified")[0];
					if ("yes".equals(params.get("account")[0]) && ("email".equals(modified) || "schoolName".equals(modified) || "name".equals(modified))) {
						Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, registration.getProperty("email")));
						Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
						switch (modified) {
							case "email":
								user.setProperty("user-id", newValue.toLowerCase());
								break;
							case "schoolName":
								user.setProperty("school", newValue);
								break;
							case "name":
								user.setProperty("name", newValue);
						}
						datastore.put(user);

						registration.setProperty(modified, newValue);
					}
					else {
						registration.setProperty(modified, newValue);
					}
					datastore.put(registration);
					txn.commit();
				}
				else {
					if (params.containsKey("delete")) {
						if (registration.getProperty("account").equals("yes")) {
							Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, registration.getProperty("email")));
							Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
							datastore.delete(user.getKey());
						}
						datastore.delete(registration.getKey()); // TODO: Do not completely delete
						txn.commit();
						resp.sendRedirect("/data?choice=registrations&updated=1");
					}
					else {
						String schoolLevel = params.get("schoolLevel")[0];
						String name = params.get("name")[0];
						String schoolName = params.get("schoolName")[0];
						String email = params.get("email")[0];
						String account = params.get("account")[0];
						if (registration.getProperty("account").equals("yes") && account.equals("no")) {
							registration.setProperty("account", "no");
							Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, registration.getProperty("email")));
							Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
							datastore.delete(user.getKey());
						}
						else if (registration.getProperty("account").equals("yes")) {
							Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, registration.getProperty("email")));
							Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
							user.setProperty("name", name);
							user.setProperty("school", schoolName);
							user.setProperty("schoolLevel", schoolLevel);
							user.setProperty("user-id", email);
							datastore.put(user);
						}

						registration.setProperty("registrationType", params.get("registrationType")[0]);
						registration.setProperty("schoolName", schoolName);
						registration.setProperty("schoolLevel", schoolLevel);
						registration.setProperty("name", name);
						registration.setProperty("email", email);
						registration.setProperty("cost", Integer.parseInt(params.get("cost")[0]));
						registration.setProperty("paid", params.get("paid")[0]);
						registration.setProperty("division", params.containsKey("division") ? params.get("division")[0] : "");
						registration.setProperty("studentData", new Text(params.get("studentData")[0]));

						datastore.put(registration);
						txn.commit();
					}

					resp.sendRedirect("/data?choice=registrations&updated=1");
				}
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
