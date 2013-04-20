package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

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

@SuppressWarnings("serial")
public class ContactUs extends HttpServlet
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

		Entity user = null;
		if(userCookie != null)
			user = userCookie.authenticateUser();
		boolean loggedIn = userCookie != null && user != null;

		context.put("loggedIn", loggedIn);
		String cookieContent = "";
		if(loggedIn)
		{
			cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
			if(cookieContent.split("\\$")[0].equals("admin"))
			{
				context.put("admin", true);
				context.put("user", user.getProperty("user-id"));
			}
			else
			{
				context.put("user", user.getProperty("user-id"));
				context.put("name", user.getProperty("name"));
				context.put("email", user.getProperty("user-id"));
				context.put("school", user.getProperty("school"));
			}
		}
		else
		{
			context.put("email", null);
			context.put("name", null);
			context.put("school", null);
		}

		try
		{
			if(!loggedIn || cookieContent.split("\\$")[0].equals("admin"))
			{
				Captcha captcha = new Captcha();
				context.put("captcha", captcha.getQuestion());
				context.put("hash", captcha.getHashedAnswer());
				context.put("salt", captcha.getSalt());
				context.put("nocaptcha", false);
			}
			else
				context.put("nocaptcha", true);
		}
		catch (NoSuchAlgorithmException e) { e.printStackTrace(); }


		context.put("updated", req.getParameter("updated"));
		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("contactus.html", context, sw);
		sw.close();

		resp.getWriter().print(sw);
	}

	@SuppressWarnings("deprecation")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("user").addFilter("name", FilterOperator.EQUAL, req.getParameter("name"));
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
		Entity feedback = new Entity("feedback");
		if(users.size() != 0)
			feedback.setProperty("user-id", users.get(0).getProperty("user-id"));

		try
		{
			if(req.getParameter("nocaptcha").equals("false"))
			{
				String plaintext = req.getParameter("salt") + req.getParameter("captcha");
				MessageDigest m = MessageDigest.getInstance("MD5");
				m.reset();
				m.update(plaintext.getBytes());
				byte[] digest = m.digest();
				BigInteger bigInt = new BigInteger(1,digest);
				String answer = bigInt.toString(16);
				while(answer.length() < 32 ){
				  answer = "0" + answer;
				}
				
				if(!answer.equals(req.getParameter("hash")))
				{
					resp.sendRedirect("/");
					return;
				}
			}
		}
		catch(Exception e) { e.printStackTrace(); }

		String name = req.getParameter("name");
		String school = req.getParameter("school");
		String comment = req.getParameter("text");
		String email = req.getParameter("email");
		feedback.setProperty("name", name);
		feedback.setProperty("school", school);
		feedback.setProperty("email", email);
		feedback.setProperty("comment", comment);
		feedback.setProperty("resolved", false);

		Transaction txn = datastore.beginTransaction();
		try
		{
			datastore.put(feedback);
			txn.commit();
		}
		catch(Exception e) { e.printStackTrace(); }
		finally
		{
			if(txn.isActive())
				txn.rollback();
		}

		resp.sendRedirect("/contactUs?updated=1");

		Session session = Session.getDefaultInstance(new Properties(), null);
		query = new Query("contestInfo");
		List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		String appEngineEmail = "";
		if(info.size() != 0)
			appEngineEmail = (String) info.get(0).getProperty("account");

		try
		{
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress((String) info.get(0).getProperty("email"), "Contest Administrator"));
			msg.setSubject("Question about Tournament from " + name);
			msg.setContent("This question is from <b>" + name + "</b> from <b>" + school + "</b> with email address " + email + ". His/her message is as follows: <b>" + comment + "</b>.", "text/html");
			Transport.send(msg);
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
