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

import java.io.IOException;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.Captcha;
import util.HTMLCompressor;
import util.Password;
import util.PropNames;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

@SuppressWarnings("serial")
public class Registration extends HttpServlet
{
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		Template t = ve.getTemplate("registration.html");
		VelocityContext context = new VelocityContext();
		
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));

		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();

		context.put("loggedIn", loggedIn);
		if(loggedIn)
		{
			context.put("admin", userCookie.isAdmin());
			context.put("user", userCookie.getUsername());
			context.put("registrationError", "You are already registered.");
		}

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
			catch(ParseException e)
			{
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Incorrect date format");
			}

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

		HttpSession sess = req.getSession(true);
		try
		{
			Captcha captcha = new Captcha();
			sess.setAttribute("hash", captcha.getHashedAnswer());
			sess.setAttribute("salt", captcha.getSalt());
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
		if("1".equals(req.getParameter("updated")))
		{
			context.put("updated", true);
			if (sess != null)
			{
				Map<String, Object> props = (Map<String, Object>) sess.getAttribute("props");
				if(props != null)
				{
					ArrayList<String> regData = new ArrayList<String>();
					for(Entry<String, Object> prop : props.entrySet())
					{
						String key = prop.getKey();
						if(!key.equals("account") && PropNames.names.get(key) != null)
							regData.add("<dt>" + PropNames.names.get(key) + "</dt>\n<dd>" + prop.getValue() + "</dd>");
					}
					Collections.sort(regData);
					context.put("regData", regData);
					sess.invalidate();
				}
			}
		}
		context.put("userError", userError);
		context.put("passwordError", passwordError);
		if(userError != null || passwordError != null)
			context.put("error", true);

		StringWriter sw = new StringWriter();
		t.merge(context, sw);
		sw.close();
		resp.setContentType("text/html");
		resp.setHeader("X-Frame-Options", "SAMEORIGIN");
		resp.getWriter().print(HTMLCompressor.customCompress(sw));
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Map<String, String[]> params = new HashMap<String, String[]>(req.getParameterMap());
		for(Entry<String, String[]> param : params.entrySet())
			params.put(param.getKey(), new String[] { escapeHtml4(param.getValue()[0]) });
		
		String registrationType = params.get("registrationType")[0];
		String account = "no";
		if(params.containsKey("account"))
			account = "yes";
		String aliases = params.get("aliases")[0];
		String email = params.get("email")[0];
		String schoolLevel = params.get("schoolLevel")[0];
		String schoolName = params.get("schoolName")[0];
		String name = params.get("name")[0];
		String password = null;
		String confPassword = null;
		
		HttpSession sess = req.getSession(true);

		try
		{
			if(!Captcha.authCaptcha((String) sess.getAttribute("salt"), req.getParameter("captcha"), (String) sess.getAttribute("hash")))
				resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Invalid Captcha hash provided");
			else
			{
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
					Entity registration = new Entity("registration");
					registration.setProperty("registrationType", registrationType);
					registration.setProperty("account", account);
					registration.setProperty("schoolName", schoolName);
					registration.setProperty("schoolLevel", schoolLevel);
					registration.setProperty("name", name);
					registration.setProperty("email", email);
					registration.setProperty("paid", "");
					registration.setProperty("timestamp", new Date());
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

						if(account.equals("yes"))
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

						sess.setAttribute("props", registration.getProperties());
						resp.sendRedirect("/registration?updated=1");

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

							VelocityEngine ve = new VelocityEngine();
							ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/email");
							ve.init();
							Template t = ve.getTemplate("registration.html");
							VelocityContext context = new VelocityContext();
							
							context.put("name", name);
							context.put("url", url);
							context.put("cost", cost);
							context.put("account", account.equals("yes"));
							
							StringWriter sw = new StringWriter();
							t.merge(context, sw);
							msg.setContent(sw.toString(), "text/html");
							Transport.send(msg);
						}
						catch (MessagingException e)
						{
							e.printStackTrace();
							resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
						}
					}
					catch (Exception e)
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
			}
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
		}
	}
}