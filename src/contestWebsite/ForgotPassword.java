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
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.BaseHttpServlet;
import util.Pair;
import util.Password;
import util.Retrieve;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;

@SuppressWarnings("serial")
public class ForgotPassword extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		String resetToken = req.getParameter("resetToken");
		String updatedPass = req.getParameter("updatedPass");
		String error = req.getParameter("error");

		if (loggedIn && !userCookie.isAdmin()) {
			resp.sendRedirect("/signout");
		}
		else if (resetToken == null && updatedPass == null && error == null) {
			context.put("updated", req.getParameter("updated"));

			close(context, ve.getTemplate("forgotPass.html"), resp);
		}
		else {
			context.put("resetToken", resetToken);
			context.put("updated", updatedPass);
			context.put("error", error);

			close(context, ve.getTemplate("resetPass.html"), resp);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getParameter("resetToken") == null) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			String email = req.getParameter("email").toLowerCase();
			Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, email));

			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
			if (users.size() != 0) {
				Entity user = users.get(0);
				Transaction txn = datastore.beginTransaction();

				SecureRandom random = new SecureRandom();
				String resetToken = new BigInteger(130, random).toString(32);

				try {
					user.setProperty("resetToken", resetToken);
					datastore.put(user);
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

				Session session = Session.getDefaultInstance(new Properties(), null);
				Entity contestInfo = Retrieve.contestInfo();
				String appEngineEmail = (String) contestInfo.getProperty("account");

				String url = req.getRequestURL().toString();
				url = url.substring(0, url.indexOf("/", 7));
				url = url + "/forgotPass?resetToken=" + resetToken;

				try {
					Message msg = new MimeMessage(session);
					msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email, (String) user.getProperty("name")));
					msg.setSubject("Reset your password for the " + contestInfo.getProperty("title") + " website");

					VelocityEngine ve = new VelocityEngine();
					ve.init();

					VelocityContext context = new VelocityContext();
					context.put("user", user.getProperty("user-id"));
					context.put("title", contestInfo.getProperty("title"));
					context.put("url", url);

					StringWriter sw = new StringWriter();
					Velocity.evaluate(context, sw, "forgotPassEmail", ((Text) contestInfo.getProperty("forgotPassEmail")).getValue());
					msg.setContent(sw.toString(), "text/html");
					Transport.send(msg);
				}
				catch (MessagingException e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
				}
			}
			resp.sendRedirect("/forgotPass?updated=1");
		}
		else {
			Map<String, String[]> params = req.getParameterMap();
			String password = params.get("password")[0];
			String confPassword = params.get("confNewPass")[0];
			String resetToken = params.get("resetToken")[0];

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("user").setFilter(new FilterPredicate("resetToken", FilterOperator.EQUAL, resetToken));
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));

			if (users.size() > 0) {
				if (confPassword.equals(password)) {
					Transaction txn = datastore.beginTransaction();
					try {
						Entity user = users.get(0);
						user.removeProperty("resetToken");
						user.removeProperty("loginAttempts");
						String hash = Password.getSaltedHash(password);
						user.setProperty("salt", hash.split("\\$")[0]);
						user.setProperty("hash", hash.split("\\$")[1]);
						datastore.put(user);
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
					resp.sendRedirect("/forgotPass?updatedPass=1");
				}
				else {
					resp.sendRedirect("/forgotPass?error=1&resetToken=" + resetToken);
				}
			}
			else {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid reset token for password recovery: " + resetToken);
			}
		}
	}
}
