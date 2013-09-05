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

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.HTMLCompressor;
import util.Password;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;

@SuppressWarnings({ "serial", "unused" })
public class ForgotPassword extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));

		UserCookie userCookie = UserCookie.getCookie(req);

		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		String noise = req.getParameter("noise");
		String updatedPass = req.getParameter("updatedPass");
		String error = req.getParameter("error");

		context.put("loggedIn", loggedIn);
		if(loggedIn && !userCookie.isAdmin())
			resp.sendRedirect("/signout");
		else if(noise == null && updatedPass == null && error == null)
		{
			context.put("updated", req.getParameter("updated"));
			StringWriter sw = new StringWriter();
			Template t = ve.getTemplate("forgotPass.html");
			t.merge(context, sw);
			sw.close();
			resp.setContentType("text/html");
			resp.getWriter().print(HTMLCompressor.customCompress(sw));
		}
		else
		{
			context.put("noise", noise);
			context.put("updated", updatedPass);
			context.put("error", error);
			StringWriter sw = new StringWriter();
			Template t = ve.getTemplate("resetPass.html");
			t.merge(context, sw);
			sw.close();
			resp.setContentType("text/html");
			resp.setHeader("X-Frame-Options", "SAMEORIGIN");
			resp.getWriter().print(HTMLCompressor.customCompress(sw));
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		if(req.getParameter("noise") == null)
		{
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			String email = req.getParameter("email");
			Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, email);

			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
			if(users.size() != 0)
			{
				Entity user = users.get(0);
				Transaction txn = datastore.beginTransaction();
				Random rand = new Random();
				String noise = Long.toString(Math.abs(rand.nextLong()), 36);
				try
				{
					user.setProperty("reset", noise);
					datastore.put(user);
					txn.commit();
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

				Session session = Session.getDefaultInstance(new Properties(), null);
				query = new Query("contestInfo");
				List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
				String appEngineEmail = "";
				if(info.size() != 0)
					appEngineEmail = (String) info.get(0).getProperty("account");
				
				String url = req.getRequestURL().toString();
				url = url.substring(0, url.indexOf("/", 7));
				url = url + "/forgotPass?noise=" + noise;
						
				try
				{
					Message msg = new MimeMessage(session);
					msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email, (String) user.getProperty("name")));
					msg.setSubject("Password Reset for Dulles Tournament Website");
					
					VelocityEngine ve = new VelocityEngine();
					ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/email");
					ve.init();
					Template t = ve.getTemplate("forgotPass.html");
					VelocityContext context = new VelocityContext();
					
					context.put("user", user.getProperty("user-id"));
					context.put("url", url);
					
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
			resp.sendRedirect("/forgotPass?updated=1");
		}
		else
		{
			Map<String, String[]> params = req.getParameterMap();
			String password = params.get("password")[0];
			String confPassword = params.get("confPassword")[0];
			String noise = params.get("noise")[0];

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("user").addFilter("reset", FilterOperator.EQUAL, noise);

			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
			if(users.size() != 0)
			{
				if(confPassword.equals(password))
				{
					Transaction txn = datastore.beginTransaction();
					try
					{
						Entity user = users.get(0);
						user.removeProperty("reset");
						user.removeProperty("loginAttempts");
						String hash = Password.getSaltedHash(password);
						user.setProperty("salt", hash.split("\\$")[0]);
						user.setProperty("hash", hash.split("\\$")[1]);
						datastore.put(user);
						txn.commit();
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
					resp.sendRedirect("/forgotPass?updatedPass=1");
				}
				else
					resp.sendRedirect("/forgotPass?error=1&noise=" + noise);
			}
			else
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid noise(" + noise +") for password recovery");
		}
	}
}