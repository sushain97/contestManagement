package contestWebsite;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

@SuppressWarnings("serial")
public class AdminPanel extends HttpServlet
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

		String updated = req.getParameter("updated");
		if(updated != null && updated.equals("1") && !loggedIn)
			resp.sendRedirect("/adminPanel?updated=1");
		context.put("updated", req.getParameter("updated"));

		if(!loggedIn)
			resp.sendRedirect("/");
		else
		{
			String cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
			if(loggedIn && cookieContent.split("\\$")[0].equals("admin"))
			{
				context.put("user", cookieContent.split("\\$")[0]);
				context.put("admin", true);

				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

				Query query = new Query("contestInfo");
				String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
				String startDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
				String email = "";
				String account = "";
				String docHigh = "";
				String docMiddle = "";
				String docAccount = "";
				Object price = "";
				Boolean complete = null;
				List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
				if(info.size() != 0)
				{
					Entity contestInfo = info.get(0);
					endDate = (String) contestInfo.getProperty("endDate");
					startDate = (String) contestInfo.getProperty("startDate");
					email = (String) contestInfo.getProperty("email");
					account = (String) contestInfo.getProperty("account");
					docMiddle = (String) contestInfo.getProperty("docMiddle");
					docHigh = (String) contestInfo.getProperty("docHigh");
					docAccount = (String) contestInfo.getProperty("docAccount");
					price = contestInfo.getProperty("price");
					complete = (Boolean) contestInfo.getProperty("complete");
				}

				context.put("loggedIn", loggedIn);
				context.put("confPassError", req.getParameter("confPassError") != null && req.getParameter("confPassError").equals("1") ? "Those passwords didn't match, try again." : null);
				context.put("passError", req.getParameter("passError") != null && req.getParameter("passError").equals("1") ? "That password is incorrect, try again." : null);
				context.put("account", account == null ? "" : account);
				context.put("email", email == null ? "" : email);
				context.put("docAccount", docAccount == null ? "" : docAccount);
				context.put("docHigh", docHigh == null ? "" : docHigh);
				context.put("docMiddle", docMiddle == null ? "" : docMiddle);
				context.put("complete", complete);
				context.put("price", price);
				context.put("startDate", startDate == null ? new SimpleDateFormat("MM/dd/yyyy").format(new Date()) : startDate);
				context.put("endDate", endDate == null ? new SimpleDateFormat("MM/dd/yyyy").format(new Date()) : endDate);
				StringWriter sw = new StringWriter();
				Velocity.mergeTemplate("adminpanel.html", context, sw);
				sw.close();

				resp.getWriter().print(sw);
			}
			else
				resp.sendRedirect("/");
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
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
			resp.sendRedirect("/");
		else
		{
			Map<String, String[]> params = req.getParameterMap();

			String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
			String startDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
			String email = "";
			String account = "";
			String curPassword = "";
			String confPassword = "";
			String newPassword = "";
			String docHigh = "";
			String docMiddle = "";
			String docAccount = "";
			String docPassword = "";
			int price = 5;
			String complete[] = params.get("complete");

			try
			{
				startDate = params.get("startDate")[0];
				endDate = params.get("endDate")[0];
				email = params.get("email")[0];
				account = params.get("account")[0];
				curPassword = params.get("curPassword")[0];
				confPassword = params.get("confPassword")[0];
				newPassword = params.get("newPassword")[0];
				price = Integer.parseInt(params.get("price")[0]);
				if(params.get("updateScores")[0].equals("yes"))
				{
					docHigh = params.get("docHigh")[0];
					docMiddle = params.get("docMiddle")[0];
					docAccount = params.get("docAccount")[0];
					docPassword = params.get("docPassword")[0];
				}
			}
			catch(Exception e) { e.printStackTrace(); }

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try
			{
				Query query = new Query("contestInfo");
				List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
				Entity contestInfo;
				if(info.size() != 0)
					contestInfo = info.get(0);
				else
					contestInfo =  new Entity("contestInfo");

				contestInfo.setProperty("endDate", endDate);
				contestInfo.setProperty("startDate", startDate);
				contestInfo.setProperty("email", email);
				contestInfo.setProperty("account", account);
				contestInfo.setProperty("price", price);
				contestInfo.setProperty("complete", complete == null ? false : true);

				if(params.get("updateScores")[0].equals("yes"))
				{
					contestInfo.setProperty("docAccount", docAccount);
					contestInfo.setProperty("docHigh", docHigh);
					contestInfo.setProperty("docMiddle", docMiddle);

					Queue queue = QueueFactory.getDefaultQueue();
					queue.add(withUrl("/tabulate").param("docPassword", docPassword).param("docAccount", docAccount).param("docMiddle", docMiddle).param("docHigh", docHigh));
				}

				datastore.put(contestInfo);

				query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, "admin");
				Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
				String hash = (String) user.getProperty("hash");
				String salt = (String) user.getProperty("salt");

				if(params.get("changePass").equals("yes"))
				{
					if(Password.check(curPassword, salt + "$" + hash))
					{
						if(confPassword.equals(newPassword))
						{
							String newHash = Password.getSaltedHash(newPassword);
							resp.addCookie(new Cookie("user-id", URLEncoder.encode("admin" + "$" + newHash.split("\\$")[1], "UTF-8")));

							user.setProperty("salt", newHash.split("\\$")[0]);
							user.setProperty("hash", newHash.split("\\$")[1]);
							datastore.put(user);
							resp.sendRedirect("/adminPanel?updated=1");
						}
						else
							resp.sendRedirect("/adminPanel?confPassError=1");
					}
					else
						resp.sendRedirect("/adminPanel?passError=1");
				}
				else
					resp.sendRedirect("/adminPanel?updated=1");
				txn.commit();
			}
			catch(Exception e) { e.printStackTrace(); }
			finally
			{
				if(txn.isActive())
					txn.rollback();
			}
		}
	}
}