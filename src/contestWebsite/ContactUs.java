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
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.gdata.util.common.base.Charsets;
import com.google.gdata.util.common.io.CharStreams;
import com.google.gdata.util.common.io.InputSupplier;

import util.BaseHttpServlet;
import util.Pair;
import util.Retrieve;
import util.UserCookie;

@SuppressWarnings("serial")
public class ContactUs extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");
		Entity user = userCookie != null ? userCookie.authenticateUser() : null;

		HttpSession sess = req.getSession(true);
		sess.setAttribute("nocaptcha", loggedIn && !userCookie.isAdmin());

		if (loggedIn && !userCookie.isAdmin()) {
			context.put("user", user.getProperty("user-id"));
			context.put("name", user.getProperty("name"));
			context.put("email", user.getProperty("user-id"));
			context.put("school", user.getProperty("school"));
		}
		else {
			context.put("email", sess.getAttribute("email"));
			context.put("name", sess.getAttribute("name"));
			context.put("school", sess.getAttribute("school"));
			context.put("comment", sess.getAttribute("comment"));
		}

		if (req.getParameter("comment") != null) {
			context.put("comment", req.getParameter("comment"));
		}

		context.put("captchaError", req.getParameter("captchaError"));
		context.put("nocaptcha", loggedIn && !userCookie.isAdmin());
		context.put("updated", req.getParameter("updated"));
		context.put("admin", userCookie != null && userCookie.isAdmin());
		context.put("publicKey", infoAndCookie.x.getProperty("publicKey"));

		close(context, ve.getTemplate("contactUs.html"), resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("user").setFilter(new FilterPredicate("name", FilterOperator.EQUAL, req.getParameter("name")));
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
		Entity feedback = new Entity("feedback");
		if (users.size() != 0) {
			feedback.setProperty("user-id", users.get(0).getProperty("user-id"));
		}

		String name = escapeHtml4(req.getParameter("name"));
		String school = escapeHtml4(req.getParameter("school"));
		String comment = escapeHtml4(req.getParameter("text"));
		String email = escapeHtml4(req.getParameter("email"));

		HttpSession sess = req.getSession(true);
		sess.setAttribute("name", name);
		sess.setAttribute("school", school);
		sess.setAttribute("email", email);
		sess.setAttribute("comment", comment);

		Entity contestInfo = Retrieve.contestInfo();
		if (!(Boolean) sess.getAttribute("nocaptcha")) {
			URL reCaptchaURL = new URL("https://www.google.com/recaptcha/api/siteverify");
			String charset = java.nio.charset.StandardCharsets.UTF_8.name(),
					privateKey = URLEncoder.encode((String) contestInfo.getProperty("privateKey"), charset),
					captchaResponse = URLEncoder.encode(req.getParameter("g-recaptcha-response"), charset),
					IP = URLEncoder.encode(req.getRemoteAddr(), charset);
			String reCaptchaQuery = String.format("secret=%s&response=%s&remoteip=%s", privateKey, captchaResponse, IP);

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
				if (!JSONResponse.getBoolean("success")) {
					resp.sendRedirect("/contactUs?captchaError=1");
					return;
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
				resp.sendRedirect("/contactUs?captchaError=1");
				return;
			}
		}

		feedback.setProperty("name", name);
		feedback.setProperty("timestamp", new Date());
		feedback.setProperty("school", school);
		feedback.setProperty("email", email);
		feedback.setProperty("comment", new Text(comment));
		feedback.setProperty("resolved", false);

		Transaction txn = datastore.beginTransaction();
		try {
			datastore.put(feedback);
			txn.commit();

			Session session = Session.getDefaultInstance(new Properties(), null);
			String appEngineEmail = (String) contestInfo.getProperty("account");

			try {
				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress((String) contestInfo.getProperty("email"), "Contest Administrator"));
				msg.setSubject("Question about tournament from " + name);
				msg.setReplyTo(new InternetAddress[] {
						new InternetAddress(req.getParameter("email"), name),
						new InternetAddress(appEngineEmail, "Tournament Website Admin")
				});

				VelocityEngine ve = new VelocityEngine();
				ve.init();

				VelocityContext context = new VelocityContext();
				context.put("name", name);
				context.put("email", email);
				context.put("school", school);
				context.put("message", comment);

				StringWriter sw = new StringWriter();
				Velocity.evaluate(context, sw, "questionEmail", ((Text) contestInfo.getProperty("questionEmail")).getValue());
				msg.setContent(sw.toString(), "text/html");
				Transport.send(msg);
			}
			catch (MessagingException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
				return;
			}

			resp.sendRedirect("/contactUs?updated=1");
			sess.invalidate();
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
