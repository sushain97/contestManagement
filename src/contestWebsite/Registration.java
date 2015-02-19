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
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.BaseHttpServlet;
import util.Pair;
import util.Password;
import util.PropNames;
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
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.gdata.util.common.base.Charsets;
import com.google.gdata.util.common.io.CharStreams;
import com.google.gdata.util.common.io.InputSupplier;

import contestTabulation.Level;
import contestTabulation.Subject;

@SuppressWarnings("serial")
public class Registration extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);
		boolean loggedIn = (boolean) context.get("loggedIn");

		if (loggedIn && !infoAndCookie.y.isAdmin()) {
			context.put("registrationError", "You are already registered.");
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+6"));

		String endDateStr = dateFormat.format(new Date());
		String startDateStr = dateFormat.format(new Date());

		Entity contestInfo = infoAndCookie.x;
		if (contestInfo != null) {
			endDateStr = (String) contestInfo.getProperty("endDate");
			startDateStr = (String) contestInfo.getProperty("startDate");

			Date endDate = new Date();
			Date startDate = new Date();
			try {
				endDate = dateFormat.parse(endDateStr);
				startDate = dateFormat.parse(startDateStr);
			}
			catch (ParseException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Incorrect date format");
			}

			if (loggedIn && infoAndCookie.y.isAdmin()) {
				context.put("registrationError", "");
			}
			else if (new Date().after(endDate) || new Date().before(startDate)) {
				context.put("registrationError", "Registration is closed, please try again next year.");
			}
			else {
				context.put("registrationError", "");
			}

			context.put("price", contestInfo.getProperty("price"));
			context.put("classificationQuestion", contestInfo.getProperty("classificationQuestion"));
			context.put("publicKey", contestInfo.getProperty("publicKey"));
		}
		else {
			context.put("registrationError", "Registration is closed, please try again next year.");
			context.put("price", 5);
		}

		HttpSession sess = req.getSession(true);
		sess.setAttribute("nocaptcha", loggedIn && infoAndCookie.y.isAdmin());
		context.put("nocaptcha", loggedIn && infoAndCookie.y.isAdmin());

		String userError = req.getParameter("userError");
		String passwordError = req.getParameter("passwordError");
		String captchaError = req.getParameter("captchaError");

		if (sess != null && (userError + passwordError + captchaError).contains("1")) {
			context.put("coach".equals(sess.getAttribute("registrationType")) ? "coach" : "student", true);
			context.put("account", "yes".equals(sess.getAttribute("account")));

			String[] propNames = {"schoolName", "name", "email", "updated", "classification", "studentData", "schoolLevel"};
			for (String propName : propNames) {
				context.put(propName, sess.getAttribute(propName));
			}
		}
		else {
			context.put("account", true);
			context.put("schoolName", "");
			context.put("name", "");
			context.put("email", "");
			context.put("studentData", "[]");
		}

		if ("1".equals(req.getParameter("updated"))) {
			context.put("updated", true);
			if (sess != null) {
				Map<String, Object> props = (Map<String, Object>) sess.getAttribute("props");
				if (props != null) {
					ArrayList<String> regData = new ArrayList<String>();
					for (Entry<String, Object> prop : props.entrySet()) {
						String key = prop.getKey();
						if (!key.equals("account") && PropNames.names.get(key) != null) {
							regData.add("<dt>" + PropNames.names.get(key) + "</dt>\n<dd>" + prop.getValue() + "</dd>");
						}
					}

					Collections.sort(regData);
					context.put("regData", regData);
					context.put("studentData", sess.getAttribute("studentData"));

					sess.invalidate();
				}
			}
		}

		context.put("userError", userError);
		context.put("passwordError", passwordError);
		context.put("captchaError", captchaError);
		if (userError != null || passwordError != null || captchaError != null) {
			context.put("error", true);
		}

		context.put("Level", Level.class);

		close(context, ve.getTemplate("registration.html"), resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity contestInfo = Retrieve.contestInfo();

		Map<String, String[]> params = new HashMap<String, String[]>(req.getParameterMap());
		for (Entry<String, String[]> param : params.entrySet()) {
			if (!"studentData".equals(param.getKey())) {
				params.put(param.getKey(), new String[] {escapeHtml4(param.getValue()[0])});
			}
		}

		String registrationType = params.get("registrationType")[0];
		String account = "no";
		if (params.containsKey("account")) {
			account = "yes";
		}
		String email = params.containsKey("email") && params.get("email")[0].length() > 0 ? params.get("email")[0].toLowerCase().trim() : null;
		String schoolLevel = params.get("schoolLevel")[0];
		String schoolName = params.get("schoolName")[0].trim();
		String name = params.get("name")[0].trim();
		String classification = params.containsKey("classification") ? params.get("classification")[0] : "";
		String studentData = req.getParameter("studentData");
		String password = null;
		String confPassword = null;

		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if ((!loggedIn || !userCookie.isAdmin()) && email == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "E-Mail Address parameter ('email') must be specified");
			return;
		}

		HttpSession sess = req.getSession(true);
		sess.setAttribute("registrationType", registrationType);
		sess.setAttribute("account", account);
		sess.setAttribute("account", account);
		sess.setAttribute("name", name);
		sess.setAttribute("classification", classification);
		sess.setAttribute("schoolName", schoolName);
		sess.setAttribute("schoolLevel", schoolLevel);
		sess.setAttribute("email", email);
		sess.setAttribute("studentData", studentData);

		boolean reCaptchaResponse = false;
		if (!(Boolean) sess.getAttribute("nocaptcha")) {
			URL reCaptchaURL = new URL("https://www.google.com/recaptcha/api/siteverify");
			String charset = java.nio.charset.StandardCharsets.UTF_8.name();
			String reCaptchaQuery = String.format("secret=%s&response=%s&remoteip=%s",
					URLEncoder.encode((String) contestInfo.getProperty("privateKey"), charset),
					URLEncoder.encode(req.getParameter("g-recaptcha-response"), charset),
					URLEncoder.encode(req.getRemoteAddr(), charset));

			final URLConnection connection = new URL(reCaptchaURL + "?" + reCaptchaQuery).openConnection();
			connection.setRequestProperty("Accept-Charset", charset);
			String response = CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
				@Override
				public InputStream getInput() throws IOException {
					return connection.getInputStream();
				}
			}, Charsets.UTF_8));

			try {
				JSONObject JSONResponse = new JSONObject(response);
				reCaptchaResponse = JSONResponse.getBoolean("success");
			}
			catch (JSONException e) {
				e.printStackTrace();
				resp.sendRedirect("/contactUs?captchaError=1");
				return;
			}
		}

		if (!(Boolean) sess.getAttribute("nocaptcha") && !reCaptchaResponse) {
			resp.sendRedirect("/registration?captchaError=1");
		}
		else {
			if (account.equals("yes")) {
				password = params.get("password")[0];
				confPassword = params.get("confPassword")[0];
			}

			Query query = new Query("registration").setFilter(new FilterPredicate("email", FilterOperator.EQUAL, email)).setKeysOnly();
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));

			if (users.size() != 0 && email != null || account.equals("yes") && !confPassword.equals(password)) {
				if (users.size() != 0) {
					resp.sendRedirect("/registration?userError=1");
				}
				else if (!params.get("confPassword")[0].equals(params.get("password")[0])) {
					resp.sendRedirect("/registration?passwordError=1");
				}
				else {
					resp.sendRedirect("/registration?updated=1");
				}
			}
			else {
				Entity registration = new Entity("registration");
				registration.setProperty("registrationType", registrationType);
				registration.setProperty("account", account);
				registration.setProperty("schoolName", schoolName);
				registration.setProperty("schoolLevel", schoolLevel);
				registration.setProperty("name", name);
				registration.setProperty("classification", classification);
				registration.setProperty("studentData", new Text(studentData));
				registration.setProperty("email", email);
				registration.setProperty("paid", "");
				registration.setProperty("timestamp", new Date());

				JSONArray regData = null;
				try {
					regData = new JSONArray(studentData);
				}
				catch (JSONException e) {
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					return;
				}

				long price = (Long) contestInfo.getProperty("price");
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

				Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					datastore.put(registration);

					if (account.equals("yes") && password != null && password.length() > 0 && email != null) {
						Entity user = new Entity("user");
						String hash = Password.getSaltedHash(password);
						user.setProperty("name", name);
						user.setProperty("school", schoolName);
						user.setProperty("schoolLevel", schoolLevel);
						user.setProperty("user-id", email);
						user.setProperty("salt", hash.split("\\$")[0]);
						user.setProperty("hash", hash.split("\\$")[1]);
						datastore.put(user);
					}

					txn.commit();

					sess.setAttribute("props", registration.getProperties());

					if (email != null) {
						Session session = Session.getDefaultInstance(new Properties(), null);
						String appEngineEmail = (String) contestInfo.getProperty("account");

						String url = req.getRequestURL().toString();
						url = url.substring(0, url.indexOf("/", 7));

						try {
							Message msg = new MimeMessage(session);
							msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
							msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email, name));
							msg.setSubject("Thank you for your registration!");

							VelocityEngine ve = new VelocityEngine();
							ve.init();

							VelocityContext context = new VelocityContext();
							context.put("name", name);
							context.put("url", url);
							context.put("cost", cost);
							context.put("title", contestInfo.getProperty("title"));
							context.put("account", account.equals("yes"));

							StringWriter sw = new StringWriter();
							Velocity.evaluate(context, sw, "registrationEmail", ((Text) contestInfo.getProperty("registrationEmail")).getValue());
							msg.setContent(sw.toString(), "text/html");
							Transport.send(msg);
						}
						catch (MessagingException e) {
							e.printStackTrace();
							resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
							return;
						}
					}

					resp.sendRedirect("/registration?updated=1");
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
}
