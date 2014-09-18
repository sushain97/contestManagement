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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.Password;
import util.Retrieve;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class CreateAdmin extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity admin = new Entity("user");
		admin.setProperty("user-id", "admin");
		admin.setProperty("name", "Admin");
		admin.setProperty("school", "Admin School");

		try {
			String salthash = Password.getSaltedHash("password");
			admin.setProperty("hash", salthash.split("\\$")[1]);
			admin.setProperty("salt", salthash.split("\\$")[0]);
		}
		catch (Exception e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			return;
		}

		datastore.put(admin);

		Entity contestInfo = Retrieve.contestInfo();
		contestInfo = contestInfo != null ? contestInfo : new Entity("contestInfo");
		contestInfo.setProperty("testingMode", true);
		datastore.put(contestInfo);
	}
}
