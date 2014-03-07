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

package contestWebsite;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

@SuppressWarnings("serial")
public class Logout extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserCookie userCookie = UserCookie.getCookie(req);

		if (userCookie != null) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			try {
				userCookie.authenticate();

				Query query = new Query("authToken").setKeysOnly();

				if (req.getParameterMap().containsKey("all")) {
					query.setFilter(new FilterPredicate("user-id", FilterOperator.EQUAL, userCookie.getUsername()));
				}
				else {
					Filter tokenFilter = new FilterPredicate("token", FilterOperator.EQUAL, URLDecoder.decode(userCookie.getValue(), "UTF-8"));
					Filter expiredFilter = new FilterPredicate("expires", FilterOperator.LESS_THAN, new Date());
					query.setFilter(CompositeFilterOperator.or(tokenFilter, expiredFilter));
				}

				List<Entity> tokens = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5));
				for (Entity token : tokens) {
					datastore.delete(token.getKey());
				}

				txn.commit();
			}
			catch (Exception e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
				return;
			}
			finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}

			userCookie.setMaxAge(0);
			userCookie.setValue("");
			resp.addCookie(userCookie);
		}

		resp.sendRedirect("/");
	}
}
