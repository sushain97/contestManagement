/* Component of GAE Project for TMSCA Contest Automation
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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;

import util.BaseHttpServlet;
import util.Pair;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

@SuppressWarnings("serial")
public class Data extends BaseHttpServlet
{
	@SuppressWarnings("deprecation")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();
		String template = null;
		
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		if(!loggedIn || !userCookie.isAdmin())
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		else
		{
			String choice = req.getParameter("choice");
			if(choice == null)
				resp.sendRedirect("/data?choice=overview");
			else if(choice.equals("overview"))
				template = "data.html";
			else if(choice.equals("registrations"))
			{
				template = "dataRegistrations.html";
				context.put("updated", req.getParameter("updated"));
				context.put("price", infoAndCookie.x.getProperty("price"));
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, "middle");
				List<Entity> middleRegs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, "high");
				List<Entity> highRegs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				context.put("middleRegs", middleRegs);
				context.put("highRegs", highRegs);
			}
			else if(choice.equals("questions"))
			{
				template = "dataQuestions.html";
				context.put("updated", req.getParameter("updated"));
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("feedback").addFilter("resolved", FilterOperator.EQUAL, true);
				List<Entity> resolvedQs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				query = new Query("feedback").addFilter("resolved", FilterOperator.NOT_EQUAL, true);
				List<Entity> unresolvedQs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				context.put("resolvedQs" , resolvedQs);
				context.put("unresolvedQs" , unresolvedQs);
			}
			else if(choice.equals("scores"))
			{
				template = "dataScores.html";
				
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query;

				String type = req.getParameter("type");
				context.put("type", type);
				if(type != null)
				{
					String[] types = type.split("_");
					Filter levelFilter = new FilterPredicate("level", FilterOperator.EQUAL, types[0]);
					if(types.length == 2 && !types[1].equals("category"))
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, types[1]);
						Filter filter = CompositeFilterOperator.and(typeFilter, levelFilter);
						query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
					}
					else if(types.length == 3 && types[1].equals("school"))
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, "school");
						Filter nameFilter = new FilterPredicate("school", FilterOperator.EQUAL, types[2]);
						Filter filter = CompositeFilterOperator.and(CompositeFilterOperator.and(typeFilter, levelFilter), nameFilter);
						query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
					}
					else if(types.length == 3 && types[1].equals("category"))
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, types[1]);
						Filter testFilter = new FilterPredicate("test", FilterOperator.EQUAL, types[2]);
						Filter filter = CompositeFilterOperator.and(CompositeFilterOperator.and(typeFilter, levelFilter), testFilter);
						query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
					}
				}
				else
					context.put("overview", true);

				Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, "school");
				Filter levelFilter = new FilterPredicate("level", FilterOperator.EQUAL, "middle");
				Filter filter = CompositeFilterOperator.and(typeFilter, levelFilter);
				query = new Query("html").setFilter(filter);
				List<Entity> middleSchools = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				if(!middleSchools.isEmpty())
					context.put("middleSchools", middleSchools);

				levelFilter = new FilterPredicate("level", FilterOperator.EQUAL, "high");
				filter = CompositeFilterOperator.and(typeFilter, levelFilter);
				query = new Query("html").setFilter(filter);
				List<Entity> highSchools = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
				if(!highSchools.isEmpty())
					context.put("highSchools", highSchools);

				context.put("date", infoAndCookie.x.getProperty("updated"));
				context.put("esc", new EscapeTool());
			}
			else
			{
				resp.sendRedirect("/data?choice=overview");
				return;
			}

			close(context, ve.getTemplate(template), resp);
		}
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(loggedIn && userCookie.isAdmin())
		{
			String choice = req.getParameter("choice");
			if(choice == null)
				resp.sendRedirect("/data?choice=overview");
			else if(choice.equals("overview"))
			{
				resp.sendRedirect("/data?choice=overview");
			}
			else if(choice.equals("registrations"))
			{
				String edit = req.getParameter("edit");
				if(edit != null)
					resp.sendRedirect("/editRegistration?key=" + req.getParameter("edit"));
				else
					resp.sendRedirect("/data?choice=registrations");
			}
			else if(choice.equals("questions"))
			{
				Map<String, String[]> params = req.getParameterMap();
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
				try
				{
					for(String paramName : params.keySet())
						if(!paramName.equals("choice") && !paramName.equals("updated"))
						{
							Key key = KeyFactory.createKey("feedback", Integer.parseInt(paramName));
							String option = params.get(paramName)[0];
							if(option.equals("r"))
							{
								Entity q = datastore.get(key);
								q.setProperty("resolved", true);
								datastore.put(q);
							}
							else if(option.equals("d"))
								datastore.delete(key);
							else
								throw new IllegalArgumentException();
						}
					txn.commit();
				}
				catch (Exception e)
				{ 
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
					return;
				}
				finally
				{
					if(txn.isActive())
						txn.rollback();
				}

				resp.sendRedirect("/data?choice=questions&updated=1");
			}
			else if(choice.equals("scores"))
			{
				resp.sendRedirect("/data?choice=overview");
			}
			else
				resp.sendRedirect("/data?choice=overview");
		}
		else
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
	}
}