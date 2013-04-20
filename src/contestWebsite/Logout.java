package contestWebsite;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class Logout extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		Cookie[] cookies = req.getCookies();
		Cookie userCookie = null;
		if(cookies != null)
			for(Cookie cookie : cookies)
				if(cookie.getName().equals("user-id"))
					userCookie = cookie;
		if(userCookie != null)
		{
			userCookie.setMaxAge(0);
			userCookie.setValue("");
			resp.addCookie(userCookie);
		}

		resp.sendRedirect("/");
	}
}
