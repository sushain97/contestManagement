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
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;
import org.yaml.snakeyaml.Yaml;

import util.BaseHttpServlet;
import util.Pair;
import util.UserCookie;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;

@SuppressWarnings("serial")
public class Directions extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();

		init(context, req);
		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		Yaml yaml = new Yaml();
		HashMap<String, String> directions = (HashMap<String, String>) yaml.load(((Text) infoAndCookie.x.getProperty("directions")).getValue());
		context.put("directions", directions);
		context.put("school", infoAndCookie.x.getProperty("school"));
		context.put("location", infoAndCookie.x.getProperty("location"));
		context.put("address", infoAndCookie.x.getProperty("address"));
		context.put("esc", new EscapeTool());

		close(context, ve.getTemplate("directions.html"), resp);
	}
}
