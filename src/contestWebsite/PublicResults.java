package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@SuppressWarnings("serial")
public class PublicResults extends HttpServlet
{
	@SuppressWarnings("deprecation")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		resp.setContentType("text/html");
		Cookie[] cookies = req.getCookies();
		UserCookie userCookie = null;
		if(cookies != null)
			for(Cookie cookie : cookies)
				if(cookie.getName().equals("user-id"))
					userCookie = new UserCookie(cookie);
		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		if(!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1"))
			resp.sendRedirect("/?refresh=1");

		String cookieContent = "";
		Properties p = new Properties();
		p.setProperty("file.resource.loader.path", "html");
		Velocity.init(p);
		VelocityContext context = new VelocityContext();
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
		context.put("loggedIn", loggedIn);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		if(loggedIn)
		{
			cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
			if(!cookieContent.split("\\$")[0].equals("admin"))
			{
				String name = (String) user.getProperty("name");
				context.put("name", name);
				context.put("user", user.getProperty("user-id"));
			}
			else
			{
				context.put("user", user.getProperty("user-id"));
				context.put("name", "Contest Administrator");
				context.put("admin", true);
			}
		}

		Query query = new Query("contestInfo");
		List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		if(info.size() != 0)
		{
			Object complete = info.get(0).getProperty("complete");
			if((complete != null && (Boolean) complete) || cookieContent.split("\\$")[0].equals("admin"))
			{
				context.put("complete", true);

				String type = req.getParameter("type");
				if(type != null)
				{
					String[] types = type.split("_");
					Filter levelFilter = new FilterPredicate("level", FilterOperator.EQUAL, types[0]);
					if(types.length == 2)
					{
						Filter typeFilter = new FilterPredicate("type", FilterOperator.EQUAL, types[1]);
						Filter filter = CompositeFilterOperator.and(typeFilter, levelFilter);
						query = new Query("html").setFilter(filter);
						List<Entity> html = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
						if(!html.isEmpty())
							context.put("html", ((com.google.appengine.api.datastore.Text) html.get(0).getProperty("html")).getValue());
					}
					else
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
				
				context.put("date", info.get(0).getProperty("updated"));
			}
			else
				context.put("complete", false);
		}
		else
			context.put("complete", false);
		
		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("publicResults.html", context, sw);
		sw.close();

		resp.getWriter().print(sw);
	}
}