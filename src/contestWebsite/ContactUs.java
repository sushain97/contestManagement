/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

package contestWebsite;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.IOException;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.Captcha;
import util.HTMLCompressor;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;

@SuppressWarnings("serial")
public class ContactUs extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		Template t = ve.getTemplate("contactUs.html");
		VelocityContext context = new VelocityContext();
		
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));

		UserCookie userCookie = UserCookie.getCookie(req);
		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		context.put("loggedIn", loggedIn);
		if(loggedIn)
		{
			context.put("admin", userCookie.isAdmin());
			if(!userCookie.isAdmin())
			{
				context.put("user", user.getProperty("user-id"));
				context.put("name", user.getProperty("name"));
				context.put("email", user.getProperty("user-id"));
				context.put("school", user.getProperty("school"));
			}
		}
		else
		{
			context.put("email", null);
			context.put("name", null);
			context.put("school", null);
		}

		HttpSession sess = req.getSession(true);
		try
		{
			if(!loggedIn || userCookie.isAdmin())
			{
				Captcha captcha = new Captcha();
				sess.setAttribute("hash", captcha.getHashedAnswer());
				sess.setAttribute("salt", captcha.getSalt());
				sess.setAttribute("nocaptcha", false);
				context.put("captcha", captcha.getQuestion());
				context.put("hash", captcha.getHashedAnswer());
				context.put("salt", captcha.getSalt());
				context.put("nocaptcha", false);
			}
			else {
				context.put("nocaptcha", true);
				sess.setAttribute("nocaptcha", true);
			}
			
			context.put("updated", req.getParameter("updated"));
			
			StringWriter sw = new StringWriter();
			t.merge(context, sw);
			sw.close();
			resp.setContentType("text/html");
			resp.setHeader("X-Frame-Options", "SAMEORIGIN");
			resp.getWriter().print(HTMLCompressor.customCompress(sw));
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
	}

	@SuppressWarnings("deprecation")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("user").addFilter("name", FilterOperator.EQUAL, req.getParameter("name"));
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
		Entity feedback = new Entity("feedback");
		if(users.size() != 0)
			feedback.setProperty("user-id", users.get(0).getProperty("user-id"));

		HttpSession sess = req.getSession(false);
		if(!(Boolean) sess.getAttribute("nocaptcha"))
		{
			try
			{
				if(!Captcha.authCaptcha((String) sess.getAttribute("salt"), req.getParameter("captcha"), (String) sess.getAttribute("hash")))
				{
					resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Invalid Captcha hash provided");
					return;
				}
			}
			catch(NoSuchAlgorithmException e)
			{
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
				return;
			}
		}
		
		String name = escapeHtml4(req.getParameter("name"));
		String school = escapeHtml4(req.getParameter("school"));
		String comment = escapeHtml4(req.getParameter("text"));
		String email = escapeHtml4(req.getParameter("email"));
		feedback.setProperty("name", name);
		feedback.setProperty("school", school);
		feedback.setProperty("email", email);
		feedback.setProperty("comment", comment);
		feedback.setProperty("resolved", false);

		Transaction txn = datastore.beginTransaction();
		try
		{
			datastore.put(feedback);
			txn.commit();
			
			resp.sendRedirect("/contactUs?updated=1");

			Session session = Session.getDefaultInstance(new Properties(), null);
			query = new Query("contestInfo");
			List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			String appEngineEmail = "";
			if(info.size() != 0)
				appEngineEmail = (String) info.get(0).getProperty("account");

			try
			{
				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress((String) info.get(0).getProperty("email"), "Contest Administrator"));
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
			catch (MessagingException e)
			{ 
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			}
		}
		catch(Exception e)
		{ 
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
		finally
		{
			if(txn.isActive())
				txn.rollback();
		}
	}
}