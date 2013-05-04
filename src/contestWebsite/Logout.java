package contestWebsite;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class Logout extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		UserCookie userCookie = UserCookie.getCookie(req);
		if(userCookie != null)
		{
			userCookie.setMaxAge(0);
			userCookie.setValue("");
			resp.addCookie(userCookie);
		}

		resp.sendRedirect("/");
	}
}
