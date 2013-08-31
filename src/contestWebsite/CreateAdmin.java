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
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.Password;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class CreateAdmin extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity admin = new Entity("user");
		admin.setProperty("user-id", "admin");

		try
		{
			String salthash = Password.getSaltedHash("password");
			admin.setProperty("hash", salthash.split("\\$")[1]);
			admin.setProperty("salt", salthash.split("\\$")[0]);
			admin.setProperty("name", "Admin");
			admin.setProperty("email", "admin");
			admin.setProperty("school", "Admin School");
		}
		catch(Exception e) { }

		datastore.put(admin);
		
		Query query = new Query("contestInfo");
		List<Entity> infos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		Entity info;
		if(infos.size() > 0)
			info = infos.get(0);
		else
			info = new Entity("contestInfo");
		info.setProperty("testingMode", true);
		datastore.put(info);
	}
}