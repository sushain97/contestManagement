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
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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

@SuppressWarnings({ "serial", "unused" })
public class ForgotPassword extends HttpServlet
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

		UserCookie userCookie = UserCookie.getCookie(req);

		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		String noise = req.getParameter("noise");
		String updatedPass = req.getParameter("updatedPass");
		String error = req.getParameter("error");

		context.put("loggedIn", loggedIn);
		String cookieContent = "";
		if(loggedIn && !URLDecoder.decode(userCookie.getValue(), "UTF-8").split("\\$")[0].equals("admin"))
			resp.sendRedirect("/signout");
		else if(noise == null && updatedPass == null && error == null)
		{
			context.put("updated", req.getParameter("updated"));
			StringWriter sw = new StringWriter();
			Velocity.mergeTemplate("forgotPass.html", context, sw);
			sw.close();
			resp.getWriter().print(sw);
		}
		else
		{
			context.put("noise", noise);
			context.put("updated", updatedPass);
			context.put("error", error);
			StringWriter sw = new StringWriter();
			Velocity.mergeTemplate("resetPass.html", context, sw);
			sw.close();
			resp.getWriter().print(sw);
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
					resp.sendRedirect("/forgotPass?updated=1");
					e.printStackTrace();
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
				url = url.substring(0, url.indexOf(".com") + 4);
				url = "<a href=\"" + url + "/forgotPass?noise=" + noise;
						
				try
				{
					Message msg = new MimeMessage(session);
					msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress((String) info.get(0).getProperty("email"), "Contest Administrator"));
					msg.setSubject("Password Reset for Dulles Tournament Website");
					msg.setContent("Please follow the link to reset the password for " + user.getProperty("user-id") + ": <a href=\"" + url + ">" + url + "</a>.", "text/html");
					
					Transport.send(msg);
				}
				catch (Exception e) { e.printStackTrace(); }
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
						String hash = Password.getSaltedHash(password);
						user.setProperty("salt", hash.split("\\$")[0]);
						user.setProperty("hash", hash.split("\\$")[1]);
						datastore.put(user);
						txn.commit();
					}
					catch(Exception e)
					{
						resp.sendRedirect("/forgotPass");
						e.printStackTrace();
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
				resp.sendRedirect("/");
		}
	}
}
