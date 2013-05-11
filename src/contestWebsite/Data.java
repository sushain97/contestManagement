package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.EscapeTool;

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

@SuppressWarnings("serial")
public class Data extends HttpServlet
{
	@SuppressWarnings("deprecation")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		resp.setContentType("text/html");
		Properties p = new Properties();
		p.setProperty("file.resource.loader.path", "html");
		Velocity.init(p);
		VelocityContext context = new VelocityContext();
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));

		Cookie[] cookies = req.getCookies();
		UserCookie userCookie = null;
		if(cookies != null)
			for(Cookie cookie : cookies)
				if(cookie.getName().equals("user-id"))
					userCookie = new UserCookie(cookie);
		boolean loggedIn = userCookie != null && userCookie.authenticate();

		if(!loggedIn)
		{
			resp.sendRedirect("/");
			return;
		}
		else
		{
			String cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
			if(loggedIn && cookieContent.split("\\$")[0].equals("admin"))
			{
				context.put("user", cookieContent.split("\\$")[0]);
				context.put("admin", true);
			}
			else
			{
				resp.sendRedirect("/");
				return;
			}
		}

		String choice = req.getParameter("choice");
		if(choice == null)
			resp.sendRedirect("/data?choice=overview");
		else if(choice.equals("overview"))
			context.put("overview", true);
		else if(choice.equals("registrations"))
		{
			context.put("registration", true);
			context.put("updated", req.getParameter("updated"));
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, "middle");
			List<Entity> middleRegs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
			query = new Query("registration").addFilter("schoolLevel", FilterOperator.EQUAL, "high");
			List<Entity> highRegs = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
			context.put("middleRegs" , middleRegs);
			context.put("highRegs" , highRegs);
		}
		else if(choice.equals("questions"))
		{
			context.put("questions", true);
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
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query;

			String type = req.getParameter("type");
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
			
			context.put("esc", new EscapeTool());
			context.put("scores", true);
		}
		else
		{
			resp.sendRedirect("/data?choice=overview");
			return;
		}

		context.put("loggedIn", loggedIn);
		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("data.html", context, sw);
		sw.close();

		resp.getWriter().print(sw);
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		Cookie[] cookies = req.getCookies();
		UserCookie userCookie = null;
		if(cookies != null)
			for(Cookie cookie : cookies)
				if(cookie.getName().equals("user-id"))
					userCookie = new UserCookie(cookie);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(!loggedIn || !URLDecoder.decode(userCookie.getValue(), "UTF-8").split("\\$")[0].equals("admin"))
		{
			resp.sendRedirect("/");
		}
		else
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
				Transaction txn = datastore.beginTransaction();
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
				catch (Exception e) { e.printStackTrace(); }
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
	}
}
