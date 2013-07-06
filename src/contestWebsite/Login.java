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
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		if(loggedIn && !URLDecoder.decode(userCookie.getValue(), "UTF-8").split("\\$")[0].equals("admin"))
			resp.sendRedirect("/signout");
		else
		{
			Properties p = new Properties();
			p.setProperty("file.resource.loader.path", "html");
			Velocity.init(p);
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

			StringWriter sw = new StringWriter();
			Velocity.mergeTemplate("login.html", context, sw);
			sw.close();

			resp.getWriter().print(HTMLCompressor.compressor.compress(sw.toString()));
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
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			finally
			{
				if(txn.isActive())
					txn.rollback();
			}
		}
	}
}