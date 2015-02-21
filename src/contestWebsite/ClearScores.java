/*
 * Component of GAE Project for TMSCA Contest Automation
 * Copyright (C) 2015 Sushain Cherivirala
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
import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;

import util.BaseHttpServlet;
import util.PMF;
import util.Pair;
import util.UserCookie;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

import contestTabulation.School;

@SuppressWarnings("serial")
public class ClearScores extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityContext context = new VelocityContext();
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		UserCookie userCookie = infoAndCookie.y;
		boolean loggedIn = (boolean) context.get("loggedIn");

		if (loggedIn && userCookie.isAdmin()) {
			PersistenceManager pm = PMF.get().getPersistenceManager();
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

			javax.jdo.Query jdoQuery = pm.newQuery(School.class);
			jdoQuery.deletePersistentAll();

			String[] datastoreKinds = {"CategoryWinners", "CategorySweepstakesWinners", "SweepstakesWinners", "Visualization", "Student"};
			Query datastoreQuery;
			for (String kind : datastoreKinds) {
				while (true) {
					datastoreQuery = new Query(kind).setKeysOnly();
					List<Entity> entities = datastore.prepare(datastoreQuery).asList(FetchOptions.Builder.withLimit(50));
					if (entities.size() == 0) {
						break;
					}
					else {
						List<Key> keys = new ArrayList<Key>(entities.size());
						for (Entity entity : entities) {
							keys.add(entity.getKey());
						}

						datastore.delete(keys);
					}
				}
			}

			String[] contestInfoProperties = {"testsGraded", "testsGradedNums"};
			for (String propertyName : contestInfoProperties) {
				infoAndCookie.x.setProperty(propertyName, null);
			}
			datastore.put(infoAndCookie.x);
		}
		else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
		}
	}
}
