package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

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
import com.google.appengine.api.datastore.Query.FilterOperator;

@SuppressWarnings("serial")
public class MainPage extends HttpServlet
{
	@SuppressWarnings("deprecation")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		resp.setContentType("text/html");
		UserCookie userCookie = UserCookie.getCookie(req);
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
		if(loggedIn)
		{
			cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
			if(!cookieContent.split("\\$")[0].equals("admin"))
			{
				String username = (String) user.getProperty("user-id");
				String name = (String) user.getProperty("name");

				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Query query = new Query("registration").addFilter("email", FilterOperator.EQUAL, username);
				Entity reg = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
				ArrayList<String> regData = new ArrayList<String>();
				Map<String, Object> props = reg.getProperties();

				PropNames propNames = new PropNames();
				for(String key : props.keySet())
					if(!key.equals("account"))
						regData.add("<b>" + propNames.propNames.get(key) + "</b>: " + props.get(key));
				Collections.sort(regData);
				context.put("regData" , regData);
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
		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("main.html", context, sw);
		sw.close();

		resp.getWriter().print(sw);
		//TODO: Finish About page and integrate with main
	}
}