package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

@SuppressWarnings("serial")
public class Registration extends HttpServlet
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
		boolean loggedIn = userCookie != null && userCookie.authenticate();

		context.put("loggedIn", loggedIn);
		String cookieContent = "";
		if(loggedIn)
		{
			cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
			context.put("user", cookieContent.split("\\$")[0]);
			context.put("registrationError", "You are already registered.");
		}
		if(loggedIn && cookieContent.split("\\$")[0].equals("admin"))
			context.put("admin", true);

		String endDateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
		String startDateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("contestInfo");
		List<Entity> infos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		if(infos.size() != 0)
		{
			Entity info = infos.get(0);
			endDateStr = (String) info.getProperty("endDate");
			startDateStr = (String) info.getProperty("startDate");

			Date endDate = new Date();
			Date startDate = new Date();
			try
			{
				endDate = new SimpleDateFormat("MM/dd/yyyy").parse(endDateStr);
				startDate = new SimpleDateFormat("MM/dd/yyyy").parse(startDateStr);
			}
			catch(Exception e) { e.printStackTrace(); }

			if(new Date().after(endDate) || new Date().before(startDate) || new Date().equals(endDate) || new Date().equals(startDate))
				context.put("registrationError", "Registration is closed, please try again next year.");
			else
				context.put("registrationError", "");

			if(info.getProperty("price") != null)
				context.put("price", (Long) info.getProperty("price"));
			else
				context.put("price", 5);
		}
		else
		{
			context.put("registrationError", "Registration is closed, please try again next year.");
			context.put("price", 5);
		}

		try
		{
			Captcha captcha = new Captcha();
			context.put("captcha", captcha.getQuestion());
			context.put("hash", captcha.getHashedAnswer());
			context.put("salt", captcha.getSalt());
		}
		catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
		
		String[] subjects = {"N", "C", "M", "S"};
		String[] numbers = { "", "one", "two", "three", "four", "five", "six", "seven",
				"eight", "nine", "ten", "eleven", "twelve" };
		
		String userError = req.getParameter("userError");
		String passwordError = req.getParameter("passwordError");
		
		HttpSession sess = req.getSession(false);
		if(sess != null && ("1".equals(userError) || "1".equals(passwordError)))
		{
			String numString = (String) sess.getAttribute("nums");
			String[] nums = numString.split(",");
			for(int i = 6; i <= 12; i++)
				for(int j = 0; j < 4; j++)
					context.put(numbers[i] + subjects[j], Integer.parseInt(nums[(i-6)*4+j]));
			
			if(((String) sess.getAttribute("registrationType")).equals("coach"))
				context.put("coach", true);
			else
				context.put("student", true);
			
			if(((String) sess.getAttribute("schoolLevel")).equals("middle"))
				context.put("middle", true);
			else
				context.put("high", true);
			
			if(((String) sess.getAttribute("account")).equals("yes"))
				context.put("account", true);
			else
				context.put("account", false);
			
			context.put("schoolName", (String) sess.getAttribute("schoolName"));
			context.put("aliases", (String) sess.getAttribute("aliases"));
			context.put("name", (String) sess.getAttribute("name"));
			context.put("email", (String) sess.getAttribute("email"));
			context.put("updated", (String) sess.getAttribute("updated"));
		}
		else
		{
			for(int i = 6; i <= 12; i++)
				for(int j = 0; j < 4; j++)
					context.put(numbers[i] + subjects[j], 0);
			
			context.put("coach", true);
			context.put("middle", true);
			context.put("account", true);
			context.put("schoolName", "");
			context.put("aliases", "");
			context.put("name", "");
			context.put("email", "");
		}
		
		context.put("updated", req.getParameter("updated"));
		context.put("userError", userError);
		context.put("passwordError", passwordError);
		if(userError != null || passwordError != null)
			context.put("error", true);

		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("registration.html", context, sw);
		sw.close();
		resp.getWriter().print(sw);
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Map<String, String[]> params = req.getParameterMap();
		String registrationType = params.get("registrationType")[0];
		String account = params.get("account")[0];
		String aliases = params.get("aliases")[0];
		String email = params.get("email")[0];
		String schoolLevel = params.get("schoolLevel")[0];
		String schoolName = params.get("schoolName")[0];
		String name = params.get("name")[0];
		String password = null;
		String confPassword = null;

		try
		{
			String plaintext = params.get("salt")[0] + params.get("captcha")[0];
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(plaintext.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			String answer = bigInt.toString(16);
			while(answer.length() < 32)
				answer = "0" + answer;

			if(!answer.equals(params.get("hash")[0]))
			{
				resp.sendRedirect("/");
				return;
			}
		}
		catch(Exception e)
		{
			resp.sendRedirect("/");
			return;
		}

		if(account.equals("yes"))
		{
			password = params.get("password")[0];
			confPassword = params.get("confPassword")[0];
		}

		HashMap<String, Integer> nums = new HashMap<String, Integer>();
		String[] subjects = {"n", "c", "m", "s"};
		if(schoolLevel.equals("middle"))
			for(int i = 6; i <= 8; i++)
				for(int j = 0; j < 4; j++)
					nums.put(i + subjects[j], new Integer(Integer.parseInt(params.get(i + subjects[j])[0])));
		else
			for(int i = 9; i <= 12; i++)
				for(int j = 0; j < 4; j++)
					nums.put(i + subjects[j], new Integer(Integer.parseInt(params.get(i + subjects[j])[0])));


		Query query = new Query("registration").addFilter("email", FilterOperator.EQUAL, email);
		List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));

		if(users.size() != 0 || (account.equals("yes") && !confPassword.equals(password)))
		{
			HttpSession sess = req.getSession(true);
			sess.setAttribute("registrationType", registrationType);
			sess.setAttribute("account", account);
			sess.setAttribute("aliases", aliases);
			sess.setAttribute("account", account);
			sess.setAttribute("name", name);
			sess.setAttribute("schoolName", schoolName);
			sess.setAttribute("schoolLevel", schoolLevel);
			sess.setAttribute("email", email);

			String numString = "";
			if(schoolLevel.equals("middle"))
			{
				for(int i = 6; i <= 8; i++)
					for(int j = 0; j < 4; j++)
						numString += nums.get(i + subjects[j]) + ",";
				for(int i = 0; i < 16; i++)
					numString += "0,";
			}
			else
			{
				for(int i = 0; i < 12; i++)
					numString += "0,";
				for(int i = 9; i <= 12; i++)
					for(int j = 0; j < 4; j++)
						numString += nums.get(i + subjects[j]) + ",";
			}
			sess.setAttribute("nums", numString);

			if(users.size() != 0)
				resp.sendRedirect("/registration?userError=1");
			else if(!params.get("confPassword")[0].equals(params.get("password")[0]))
				resp.sendRedirect("/registration?passwordError=1");
			else
				resp.sendRedirect("/registration?updated=1");
		}
		else
		{
			HttpSession sess = req.getSession(false);
			if (sess != null)
				sess.invalidate();

			resp.sendRedirect("/registration?updated=1");

			Entity registration = new Entity("registration");
			registration.setProperty("registrationType", registrationType);
			registration.setProperty("account", account);
			registration.setProperty("schoolName", schoolName);
			registration.setProperty("schoolLevel", schoolLevel);
			registration.setProperty("name", name);
			registration.setProperty("email", email);
			registration.setProperty("paid", "");
			if(registrationType.equals("student"))
				registration.setProperty("aliases", aliases);

			long price = 5;
			query = new Query("contestInfo");
			List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(info.size() != 0 && info.get(0).getProperty("price") != null)
				price = (Long) info.get(0).getProperty("price");


			int cost = 0;
			for(Entry<String,Integer> test : nums.entrySet())
			{
				int num = test.getValue();
				if(num >= 0)
				{
					registration.setProperty(test.getKey(), num);
					cost += num * price;
				}
			}
			registration.setProperty("cost", cost);

			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try
			{
				datastore.put(registration);

				if(params.get("account")[0].equals("yes"))
				{
					Entity user = new Entity("user");
					String hash = Password.getSaltedHash(password);
					user.setProperty("name", name);
					user.setProperty("school", schoolName);
					user.setProperty("schoolLevel", schoolLevel);
					user.setProperty("user-id", email);
					user.setProperty("salt", hash.split("\\$")[0]);
					user.setProperty("hash", hash.split("\\$")[1]);
					datastore.put(user);
				}

				txn.commit();
			}
			catch (Exception e) { e.printStackTrace(); }
			finally
			{
				if(txn.isActive())
					txn.rollback();
			}

			Session session = Session.getDefaultInstance(new Properties(), null);
			query = new Query("contestInfo");
			String appEngineEmail = "";
			if(info.size() != 0)
				appEngineEmail = (String) info.get(0).getProperty("account");

			String url = req.getRequestURL().toString();
			url = url.substring(0, url.indexOf(".com") + 4);

			try
			{
				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(appEngineEmail, "Tournament Website Admin"));
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email, name));
				msg.setSubject("Thank you for your registration!");

				String text = "Thank you for registering for the Dulles TMSCA Tournament, " + name + ". " +
						"Your registration has been recorded in our database. Your total registration fees are: <b>$" + cost + ".</b> ";

				if(account.equals("yes"))
					text += "If you desire to make changes to your registration please login at our <a href=\"" + url + "\"> tournament website</a> with your e-mail address and visit the contact us page. "
							+ " Your account also allows you to check the scores of your students during and after the competition. ";
				else
					text += "If you desire to make changes to your registration please visit the contact us page at our tournament website. </br>";
				text += " More information including directions to Dulles and a competition schedule can also be found at <a href=\"" + url + "\">our website</a>.";

				msg.setContent(text, "text/html");
				Transport.send(msg);
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}
}