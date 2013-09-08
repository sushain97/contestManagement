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

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

@SuppressWarnings("serial")
public class AdminPanel extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		Template t = ve.getTemplate("adminPanel.html");
		VelocityContext context = new VelocityContext();
		
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));

		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();

		String updated = req.getParameter("updated");
		if(updated != null && updated.equals("1") && !loggedIn)
			resp.sendRedirect("/adminPanel?updated=1");
		context.put("updated", req.getParameter("updated"));

		if(loggedIn && userCookie.isAdmin())
		{
			context.put("user", userCookie.getUsername());
			context.put("admin", true);

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

			Query query = new Query("contestInfo");
			String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
			String startDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
			String email = "", account = "", docHigh = "", docMiddle = "", docAccount = "";
			Boolean complete = null, testingMode = null, hideFullNames = null;
			Object price = "";
			List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(info.size() != 0)
			{
				Entity contestInfo = info.get(0);
				endDate = (String) contestInfo.getProperty("endDate");
				startDate = (String) contestInfo.getProperty("startDate");
				email = (String) contestInfo.getProperty("email");
				account = (String) contestInfo.getProperty("account");
				docMiddle = (String) contestInfo.getProperty("docMiddle");
				docHigh = (String) contestInfo.getProperty("docHigh");
				docAccount = (String) contestInfo.getProperty("docAccount");
				price = contestInfo.getProperty("price");
				complete = (Boolean) contestInfo.getProperty("complete");
				testingMode = (Boolean) contestInfo.getProperty("testingMode");
				hideFullNames = (Boolean) contestInfo.getProperty("hideFullNames");
			}

			context.put("loggedIn", loggedIn);
			context.put("confPassError", req.getParameter("confPassError") != null && req.getParameter("confPassError").equals("1") ? "Those passwords didn't match, try again." : null);
			context.put("passError", req.getParameter("passError") != null && req.getParameter("passError").equals("1") ? "That password is incorrect, try again." : null);
			context.put("account", account == null ? "" : account);
			context.put("email", email == null ? "" : email);
			context.put("docAccount", docAccount == null ? "" : docAccount);
			context.put("docHigh", docHigh == null ? "" : docHigh);
			context.put("docMiddle", docMiddle == null ? "" : docMiddle);
			context.put("complete", complete);
			context.put("testingMode", testingMode);
			context.put("hideFullNames", hideFullNames);
			context.put("price", price);
			context.put("startDate", startDate == null ? new SimpleDateFormat("MM/dd/yyyy").format(new Date()) : startDate);
			context.put("endDate", endDate == null ? new SimpleDateFormat("MM/dd/yyyy").format(new Date()) : endDate);
			
			StringWriter sw = new StringWriter();
			t.merge(context, sw);
			sw.close();
			resp.setContentType("text/html");
			resp.setHeader("X-Frame-Options", "SAMEORIGIN");
			resp.getWriter().print(HTMLCompressor.customCompress(sw));
		}
		else
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(loggedIn && userCookie.isAdmin())
		{
			Map<String, String[]> params = req.getParameterMap();
			String endDate = params.get("endDate")[0];
			String startDate = params.get("startDate")[0];
			String email = params.get("email")[0];
			String account = params.get("account")[0];
			int price = Integer.parseInt(params.get("price")[0]);
			Boolean complete = params.get("complete") != null;
			Boolean hideFullNames = params.get("fullnames") != null;
			Boolean testingMode = params.get("testing") != null && !params.containsKey("changePass");

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try
			{
				Query query = new Query("contestInfo");
				List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
				Entity contestInfo;
				if(info.size() != 0)
					contestInfo = info.get(0);
				else
					contestInfo =  new Entity("contestInfo");

				contestInfo.setProperty("endDate", endDate);
				contestInfo.setProperty("startDate", startDate);
				contestInfo.setProperty("email", email);
				contestInfo.setProperty("account", account);
				contestInfo.setProperty("price", price);
				contestInfo.setProperty("complete", complete);
				contestInfo.setProperty("testingMode", testingMode);
				contestInfo.setProperty("hideFullNames", hideFullNames);

				if(params.containsKey("update"))
				{
					String docHigh = params.get("docHigh")[0];
					String docMiddle = params.get("docMiddle")[0];
					String docAccount = params.get("docAccount")[0];
					String docPassword = params.get("docPassword")[0];

					contestInfo.setProperty("docAccount", docAccount);
					contestInfo.setProperty("docHigh", docHigh);
					contestInfo.setProperty("docMiddle", docMiddle);

					Queue queue = QueueFactory.getDefaultQueue(); //TODO: Use OAuth2
					queue.add(withUrl("/tabulate").param("docPassword", docPassword).param("docAccount", docAccount).param("docMiddle", docMiddle).param("docHigh", docHigh));
				}

				datastore.put(contestInfo);

				query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, "admin");
				Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
				String hash = (String) user.getProperty("hash");
				String salt = (String) user.getProperty("salt");
				
				if(testingMode)
				{
					String newHash = Password.getSaltedHash("password");
					resp.addCookie(new Cookie("user-id", URLEncoder.encode("admin" + "$" + newHash.split("\\$")[1], "UTF-8")));

					user.setProperty("salt", newHash.split("\\$")[0]);
					user.setProperty("hash", newHash.split("\\$")[1]);
					datastore.put(user);
					resp.sendRedirect("/adminPanel?updated=1");
				}
				else if(params.containsKey("changePass"))
				{
					String curPassword = params.get("curPassword")[0];
					String confPassword = params.get("confPassword")[0];
					String newPassword = params.get("newPassword")[0];

					if(Password.check(curPassword, salt + "$" + hash))
					{
						if(confPassword.equals(newPassword))
						{
							String newHash = Password.getSaltedHash(newPassword);
							resp.addCookie(new Cookie("user-id", URLEncoder.encode("admin" + "$" + newHash.split("\\$")[1], "UTF-8")));

							user.setProperty("salt", newHash.split("\\$")[0]);
							user.setProperty("hash", newHash.split("\\$")[1]);
							datastore.put(user);
							resp.sendRedirect("/adminPanel?updated=1");
						}
						else
							resp.sendRedirect("/adminPanel?confPassError=1");
					}
					else
						resp.sendRedirect("/adminPanel?passError=1");
				}
				else
					resp.sendRedirect("/adminPanel?updated=1");
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
		else
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
	}
}