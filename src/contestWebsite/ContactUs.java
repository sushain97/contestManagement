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
import java.io.StringWriter;
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

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.BaseHttpServlet;
import util.Pair;
import util.Retrieve;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;

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

		context.put("captchaError", req.getParameter("captchaError"));
		context.put("nocaptcha", loggedIn && !userCookie.isAdmin());
		context.put("updated", req.getParameter("updated"));
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
			String remoteAddr = req.getRemoteAddr();
			ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
			reCaptcha.setPrivateKey((String) contestInfo.getProperty("privateKey"));

			String challenge = req.getParameter("recaptcha_challenge_field");
			String userResponse = req.getParameter("recaptcha_response_field");
			ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, userResponse);

			if (!reCaptchaResponse.isValid()) {
				resp.sendRedirect("/contactUs?captchaError=1");
				return;
			}
		}

		feedback.setProperty("name", name);
		feedback.setProperty("school", school);
		feedback.setProperty("email", email);
		feedback.setProperty("comment", comment);
		feedback.setProperty("resolved", false);

		Transaction txn = datastore.beginTransaction();
		try {
			datastore.put(feedback);
			txn.commit();

			resp.sendRedirect("/contactUs?updated=1");
			sess.invalidate();

			Session session = Session.getDefaultInstance(new Properties(), null);
			String appEngineEmail = (String) contestInfo.getProperty("account");

			try {
				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress((String) contestInfo.getProperty("email"), "Contest Administrator"));
				msg.setSubject("Question about Tournament from " + name);

				VelocityEngine ve = new VelocityEngine();
				ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/email");
				ve.init();
				Template t = ve.getTemplate("question.html");
				VelocityContext context = new VelocityContext();

				context.put("name", name);
				context.put("email", email);
				context.put("message", comment);

				StringWriter sw = new StringWriter();
				t.merge(context, sw);
				msg.setContent(sw.toString(), "text/html");
				Transport.send(msg);
			}
			catch (MessagingException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
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
}
