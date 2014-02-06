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
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

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
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

@SuppressWarnings("serial")
public class AdminPanel extends BaseHttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		String updated = req.getParameter("updated");
		if(updated != null && updated.equals("1") && !loggedIn)
			resp.sendRedirect("/adminPanel?updated=1");
		context.put("updated", req.getParameter("updated"));

		if(loggedIn && userCookie.isAdmin())
		{
			context.put("contestInfo", infoAndCookie.x);
			context.put("confPassError", req.getParameter("confPassError") != null && req.getParameter("confPassError").equals("1") ? "Those passwords didn't match, try again." : null);
			context.put("passError", req.getParameter("passError") != null && req.getParameter("passError").equals("1") ? "That password is incorrect, try again." : null);
			
			close(context, ve.getTemplate("adminPanel.html"), resp);
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
			boolean testingMode = params.get("testing") != null && !params.containsKey("changePass");

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try
			{
				Query query = new Query("contestInfo");
				List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
				Entity contestInfo = info.size() != 0 ? info.get(0) : new Entity("contestInfo");

				contestInfo.setProperty("endDate", params.get("endDate")[0]);
				contestInfo.setProperty("startDate", params.get("startDate")[0]);
				contestInfo.setProperty("email", params.get("email")[0]);
				contestInfo.setProperty("account", params.get("account")[0]);
				contestInfo.setProperty("price", Integer.parseInt(params.get("price")[0]));
				contestInfo.setProperty("complete", params.get("complete") != null);
				contestInfo.setProperty("testingMode", testingMode);
				contestInfo.setProperty("hideFullNames", params.get("fullnames") != null);
				contestInfo.setProperty("levels", params.get("levels")[0]);
				contestInfo.setProperty("title", params.get("title")[0]);
				contestInfo.setProperty("publicKey", params.get("publicKey")[0]);
				contestInfo.setProperty("privateKey", params.get("privateKey")[0]);

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