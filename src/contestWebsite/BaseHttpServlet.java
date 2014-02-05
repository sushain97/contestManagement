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
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import util.HTMLCompressor;
import util.Pair;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class BaseHttpServlet extends HttpServlet
{
	public Pair<Entity, UserCookie> init(VelocityContext context, HttpServletRequest req) throws UnsupportedEncodingException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("contestInfo");
		List<Entity> info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		Entity contestInfo = null;
		if(info.size() > 0) {
			contestInfo = info.get(0);
			context.put("title", contestInfo.hasProperty("title") ? contestInfo.getProperty("title") : "");
		}
		
		UserCookie userCookie = UserCookie.getCookie(req);
		boolean loggedIn = userCookie != null && userCookie.authenticate();
		
		context.put("loggedIn", loggedIn);
		if(loggedIn)
		{
			context.put("user", userCookie.getUsername());
			context.put("admin", userCookie.isAdmin());
		}
		
		return new Pair<Entity, UserCookie>(contestInfo, userCookie);
	}
	
	public void close(VelocityContext context, Template template, HttpServletResponse resp) throws IOException {
		StringWriter sw = new StringWriter();
		template.merge(context, sw);
		sw.close();
		resp.setContentType("text/html");
		resp.setHeader("X-Frame-Options", "SAMEORIGIN");
		resp.getWriter().print(HTMLCompressor.customCompress(sw));
	}
}