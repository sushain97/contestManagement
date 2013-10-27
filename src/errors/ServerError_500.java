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

package errors;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.HTMLCompressor;
import util.RequestPrinter;
import util.UserCookie;

@SuppressWarnings("serial")
public class ServerError_500 extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/errors, html/snippets");
		ve.init();
		Template t = ve.getTemplate("error500.html");
		VelocityContext context = new VelocityContext();
		
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		
		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
		context.put("loggedIn", loggedIn);
		context.put("error", req.getAttribute("javax.servlet.error.message"));
		context.put("uri", req.getAttribute("javax.servlet.error.request_uri"));
		context.put("servlet", req.getAttribute("javax.servlet.error.servlet_name"));
		context.put("diaginfo", RequestPrinter.debugString(req, true).replaceAll("\n", "<br>"));
		context.put("date", new Date().toString());
		if(loggedIn)
		{
			context.put("user", userCookie.getUsername());
			context.put("admin", userCookie.isAdmin());
		}
		
		StringWriter sw = new StringWriter();
		t.merge(context, sw);
		sw.close();
		resp.setContentType("text/html");
		resp.setHeader("X-Frame-Options", "SAMEORIGIN");
		resp.getWriter().print(HTMLCompressor.customCompress(sw));
	}
}
