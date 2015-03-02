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
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.BaseHttpServlet;
import util.Pair;
import util.Password;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

@SuppressWarnings("serial")
public class Login extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if (loggedIn && !userCookie.isAdmin()) {
			resp.sendRedirect("/signout");
		}
		else {
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
			ve.init();
			VelocityContext context = new VelocityContext();
			Pair<Entity, UserCookie> infoAndCookie = init(context, req);

			String user = req.getParameter("user");
			String error = req.getParameter("error");
			context.put("username", user == null ? "" : user);

			if ("401".equals(error)) {
				error = "Invalid login";
			}
			else if ("403".equals(error)) {
				error = "Maximum login attempts exceeded, please reset your password";
			}
			else {
				error = null;
			}
			context.put("error", error);

			Entity contestInfo = infoAndCookie.x;
			if (contestInfo != null && contestInfo.hasProperty("testingMode") && (Boolean) contestInfo.getProperty("testingMode")) {
				context.put("testingMode", true);
			}

			close(context, ve.getTemplate("login.html"), resp);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String username = req.getParameter("username").toLowerCase();
		String password = req.getParameter("password");

		String redirect = req.getParameter("redirect");
		if (redirect == null) {
			redirect = "/?refresh=1";
		}

		Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, username));
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		String hash = "", salt = "";
		if (users.size() == 0) {
			resp.sendRedirect("/login?user=" + username + "&error=401&redirect=" + redirect);
		}
		else {
			Entity user = users.get(0);
			hash = (String) user.getProperty("hash");
			salt = (String) user.getProperty("salt");

			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try {
				if (Password.check(password, salt + "$" + hash)) {
					SecureRandom random = new SecureRandom();
					String authToken = new BigInteger(130, random).toString(32);

					Entity token = new Entity("authToken");
					token.setProperty("user-id", username);
					token.setProperty("token", authToken);
					user.setProperty("loginAttempts", 0);

					boolean persistent = "stay".equals(req.getParameter("signedIn"));
					Calendar calendar = Calendar.getInstance();
					if (persistent) {
						calendar.add(Calendar.WEEK_OF_YEAR, 1);
					}
					else {
						calendar.add(Calendar.MINUTE, 120);
					}
					token.setProperty("expires", new Date(calendar.getTimeInMillis()));

					Cookie cookie = new Cookie("authToken", authToken);
					cookie.setValue(authToken);
					resp.addCookie(cookie);

					datastore.put(token);
					datastore.put(user);
					resp.sendRedirect(redirect);
				}
				else {
					Long loginAttempts = (Long) user.getProperty("loginAttempts");
					if (loginAttempts == null) {
						user.setProperty("loginAttempts", 1);
						resp.sendRedirect("/login?user=" + username + "&error=401&redirect=" + redirect);
					}
					else {
						if (loginAttempts >= 30) {
							user.setProperty("loginAttempts", ++loginAttempts);
							resp.sendRedirect("/login?user=" + username + "&error=403&redirect=" + redirect);
						}
						else {
							user.setProperty("loginAttempts", ++loginAttempts);
							resp.sendRedirect("/login?user=" + username + "&error=401&redirect=" + redirect);
						}
					}

					datastore.put(user);
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
	}
}
