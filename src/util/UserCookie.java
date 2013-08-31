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

package util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

public class UserCookie extends Cookie
{
	public UserCookie(String name, String value)
	{
		super(name, value);
	}

	public UserCookie(Cookie cookie)
	{
		super(cookie.getName(), cookie.getValue());
		setComment(cookie.getComment());
		String domain = cookie.getDomain();
		if(domain != null)
			setDomain(domain);
		setPath(cookie.getPath());
		setMaxAge(cookie.getMaxAge());
		setSecure(cookie.getSecure());
		setVersion(cookie.getVersion());
	}
	
	public static UserCookie getCookie(HttpServletRequest req)
	{
		Cookie[] cookies = req.getCookies();
		if(cookies != null)
			for(Cookie cookie : cookies)
				if(cookie.getName().equals("user-id"))
					return new UserCookie(cookie);
		return null;
	}

	public String getUsername() throws UnsupportedEncodingException
	{
		String cookieContent = URLDecoder.decode(getValue(), "UTF-8");
		return cookieContent.split("\\$")[0];
	}
	
	public boolean isAdmin() throws UnsupportedEncodingException
	{
		String cookieContent = URLDecoder.decode(getValue(), "UTF-8");
		return "admin".equals(cookieContent.split("\\$")[0]);
	}
	
	public boolean authenticate()
	{
		try
		{
			String cookieContent = URLDecoder.decode(getValue(), "UTF-8");
			if(cookieContent.split("\\$").length != 2)
				return false;
			String user = cookieContent.split("\\$")[0];
			String hash = cookieContent.split("\\$")[1];
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			@SuppressWarnings("deprecation")
			Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, user);
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
			return users.size() != 0 && users.get(0).getProperty("hash").equals(hash);
		}
		catch(UnsupportedEncodingException e)
		{
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public Entity authenticateUser()
	{
		try
		{
			String cookieContent = URLDecoder.decode(getValue(), "UTF-8");
			if(cookieContent.split("\\$").length != 2)
				return null;
			String user = cookieContent.split("\\$")[0];
			String hash = cookieContent.split("\\$")[1];
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, user);
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(users.size() != 0 && users.get(0).getProperty("hash").equals(hash))
				return users.get(0);
			else
				return null;
		}
		catch(UnsupportedEncodingException e)
		{
			return null;
		}
	}

}