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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.yaml.snakeyaml.Yaml;

import util.BaseHttpServlet;
import util.Pair;
import util.Retrieve;
import util.UserCookie;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;

import contestTabulation.Level;
import contestTabulation.Subject;
import contestTabulation.Test;

@SuppressWarnings("serial")
public class About extends BaseHttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
		ve.init();
		VelocityContext context = new VelocityContext();

		Pair<Entity, UserCookie> infoAndCookie = init(context, req);

		Yaml yaml = new Yaml();
		HashMap<String, String> schedule = (HashMap<String, String>) yaml.load(((Text) infoAndCookie.x.getProperty("schedule")).getValue());
		context.put("schedule", schedule);
		context.put("aboutText", ((Text) infoAndCookie.x.getProperty("aboutText")).getValue());

		context.put("qualifyingCriteria", Retrieve.qualifyingCriteria(infoAndCookie.x));
		context.put("Test", Test.class);
		context.put("Level", Level.class);
		context.put("Subject", Subject.class);

		close(context, ve.getTemplate("about.html"), resp);
	}
}
