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
import java.io.InputStream;
import java.util.logging.Level;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.Retrieve;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class Favicon extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
		memcache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
		Object value = memcache.get("faviconKey");
		if (value == null) {
			String faviconKey = "default";
			Entity contestInfo = Retrieve.contestInfo();
			if (contestInfo.hasProperty("faviconKey")) {
				faviconKey = (String) contestInfo.getProperty("faviconKey");
			}

			memcache.put("faviconKey", faviconKey);
		}

		if (value == null || "default".equals(value)) {
			InputStream is = getServletContext().getResourceAsStream("/favicon.ico");
			resp.setContentType("image/x-icon");
			ServletOutputStream out = resp.getOutputStream();

			byte[] buffer = new byte[2048];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			is.close();
			out.close();
		}
		else {
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			blobstoreService.serve(new BlobKey((String) value), resp);
		}
	}
}
