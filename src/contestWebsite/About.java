package contestWebsite;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

@SuppressWarnings("serial")
public class About extends HttpServlet
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
			context.put("user", cookieContent.split("\\$")[0]);
		}
		if(loggedIn && cookieContent.split("\\$")[0].equals("admin"))
			context.put("admin", true);
		StringWriter sw = new StringWriter();
		Velocity.mergeTemplate("about.html", context, sw);
		sw.close();
		
		resp.getWriter().print(sw);
	}
}
