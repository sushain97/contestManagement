package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;

@SuppressWarnings("serial")
public class Login extends HttpServlet
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
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(loggedIn && !URLDecoder.decode(userCookie.getValue(), "UTF-8").split("\\$")[0].equals("admin"))
		{
			resp.sendRedirect("/signout");
			return;
		}

		Properties p = new Properties();
		p.setProperty("file.resource.loader.path", "html");
		Velocity.init(p);
		VelocityContext context = new VelocityContext();
		String user = req.getParameter("user");
		String error = req.getParameter("error");
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
		context.put("username", user == null ? "" : user);
		context.put("loggedIn", false);
		context.put("error", error == null || error.equals("") ? null : "Invalid login");
		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("login.html", context, sw);
		sw.close();

		resp.getWriter().print(sw);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String user = req.getParameter("username");
		String password = req.getParameter("password");
		@SuppressWarnings("deprecation")
		Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, user);
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
		String hash = "";
		String salt = "";
		if(users.size() == 0)
			resp.sendRedirect("/login?user=" + user + "&error=" + "1");
		else
		{
			hash = (String) users.get(0).getProperty("hash");
			salt = (String) users.get(0).getProperty("salt");
		}

		Transaction txn = datastore.beginTransaction();
		try
		{
			if(Password.check(password, salt + "$" + hash))
			{
				String newHash = Password.getSaltedHash(password);
				resp.addCookie(new Cookie("user-id", URLEncoder.encode(user + "$" + newHash.split("\\$")[1], "UTF-8")));

				users.get(0).setProperty("salt", newHash.split("\\$")[0]);
				users.get(0).setProperty("hash", newHash.split("\\$")[1]);
				datastore.put(users.get(0));
				resp.sendRedirect("/?refresh=1");
			}
			else
				resp.sendRedirect("/login?user=" + user + "&error=" + "1");
			txn.commit();
		}
		catch(Exception e) { resp.sendRedirect("/login?user=" + user + "&error=" + "1"); }
		finally
		{
			if(txn.isActive())
				txn.rollback();
		}
	}
}
