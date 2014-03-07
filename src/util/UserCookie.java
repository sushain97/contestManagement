/*
 * Component of GAE Project for TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [http://www.gnu.org/licenses/].
 */

package util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class UserCookie extends Cookie {
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	private String username;

	private UserCookie(String name, String value) {
		super(name, value);
	}

	private UserCookie(Cookie cookie) {
		super(cookie.getName(), cookie.getValue());
		setComment(cookie.getComment());
		String domain = cookie.getDomain();
		if (domain != null) {
			setDomain(domain);
		}
		setPath(cookie.getPath());
		setMaxAge(cookie.getMaxAge());
		setSecure(cookie.getSecure());
		setVersion(cookie.getVersion());
	}

	public static UserCookie getCookie(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("authToken")) {
					return new UserCookie(cookie);
				}
			}
		}
		return null;
	}

	public String getUsername() {
		return username;
	}

	public boolean isAdmin() {
		return "admin".equals(username);
	}

	public boolean authenticate() throws UnsupportedEncodingException {
		Entity token = getToken(URLDecoder.decode(getValue(), "UTF-8"));

		if (token != null) {
			username = (String) token.getProperty("user-id");
			return true;
		}
		else {
			return false;
		}
	}

	public Entity authenticateUser() throws UnsupportedEncodingException {
		Entity token = getToken(URLDecoder.decode(getValue(), "UTF-8"));

		if (token != null) {
			username = (String) token.getProperty("user-id");

			Query query = new Query("user").setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, username));
			return datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
		}
		else {
			return null;
		}
	}

	private static Entity getToken(String token) {
		Filter tokenFilter = new FilterPredicate("token", FilterOperator.EQUAL, token);
		Filter expireFilter = new FilterPredicate("expires", FilterOperator.GREATER_THAN, new Date());
		Query query = new Query("authToken").setFilter(CompositeFilterOperator.and(tokenFilter, expireFilter));

		List<Entity> tokens = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		return !tokens.isEmpty() ? tokens.get(0) : null;
	}
}
