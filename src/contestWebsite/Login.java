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
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
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

@SuppressWarnings("serial")
public class Login extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(loggedIn && !userCookie.isAdmin())
			resp.sendRedirect("/signout");
		else
		{
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
			ve.init();
			Template t = ve.getTemplate("login.html");
			VelocityContext context = new VelocityContext();
			
			String user = req.getParameter("user");
			String error = req.getParameter("error");
			context.put("year", Calendar.getInstance().get(Calendar.YEAR));
			context.put("username", user == null ? "" : user);
			context.put("loggedIn", false);

			if("401".equals(error))
				error = "Invalid login";
			else if("403".equals(error))
				error = "Maximum login attempts exceeded, please reset your password";
			else
				error = null;
			context.put("error", error);
			
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("contestInfo");
			List<Entity> infos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(infos.size() > 0 && infos.get(0).getProperty("testingMode") != null && (Boolean) infos.get(0).getProperty("testingMode"))
				context.put("testingMode", true);

			StringWriter sw = new StringWriter();
			t.merge(context, sw);
			sw.close();
			resp.setContentType("text/html");
			resp.setHeader("X-Frame-Options", "SAMEORIGIN");
			resp.getWriter().print(HTMLCompressor.customCompress(sw));
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String username = req.getParameter("username");
		String password = req.getParameter("password");
		@SuppressWarnings("deprecation")
		Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, username);
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
		String hash = "";
		String salt = "";
		if(users.size() == 0)
			resp.sendRedirect("/login?user=" + username + "&error=" + "401");
		else
		{
			Entity user = users.get(0);
			hash = (String) user.getProperty("hash");
			salt = (String) user.getProperty("salt");

			Transaction txn = datastore.beginTransaction();
			try
			{	
				if(Password.check(password, salt + "$" + hash))
				{
					String newHash = Password.getSaltedHash(password);
					Cookie cookie = new Cookie("user-id", URLEncoder.encode(username + "$" + newHash.split("\\$")[1], "UTF-8"));
					cookie.setMaxAge("stay".equals(req.getParameter("signedIn")) ? -1 : 3600);
					resp.addCookie(cookie);

					user.setProperty("salt", newHash.split("\\$")[0]);
					user.setProperty("hash", newHash.split("\\$")[1]);
					user.removeProperty("loginAttempts");
					datastore.put(user);
					resp.sendRedirect("/?refresh=1");
				}
				else
				{
					Long loginAttempts = (Long) user.getProperty("loginAttempts");
					if(loginAttempts == null)
					{
						user.setProperty("loginAttempts", 1);
						resp.sendRedirect("/login?user=" + username + "&error=" + "401");
					}
					else
					{
						if(loginAttempts >= 30)
						{
							user.setProperty("loginAttempts", ++loginAttempts);
							resp.sendRedirect("/login?user=" + username + "&error=" + "403");
						}
						else
						{
							user.setProperty("loginAttempts", ++loginAttempts);
							resp.sendRedirect("/login?user=" + username + "&error=" + "401");
						}
					}

					datastore.put(user);
				}

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
		}
	}
}