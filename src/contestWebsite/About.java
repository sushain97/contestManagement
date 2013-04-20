package contestWebsite;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class About extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		resp.setContentType("text/html");
		resp.getWriter().println("<b>The world is ending, what are you going to do about it?</b>");

	}
}
