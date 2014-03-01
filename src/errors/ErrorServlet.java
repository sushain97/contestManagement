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

package errors;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.EscapeTool;

import util.BaseHttpServlet;
import util.Pair;
import util.RequestPrinter;
import util.UserCookie;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class ErrorServlet extends BaseHttpServlet {
	@Override
	public Pair<Entity, UserCookie> init(VelocityContext context, HttpServletRequest req) throws UnsupportedEncodingException {
		Pair<Entity, UserCookie> infoAndCookie = super.init(context, req);

		context.put("error", req.getAttribute("javax.servlet.error.exception"));
		context.put("errorType", req.getAttribute("javax.servlet.error.exception_type"));
		context.put("errorMessage", req.getAttribute("javax.servlet.error.message"));
		context.put("uri", req.getAttribute("javax.servlet.error.request_uri"));
		context.put("servlet", req.getAttribute("javax.servlet.error.servlet_name"));
		context.put("diaginfo", RequestPrinter.debugString(req, true).replaceAll("\n", "<br>"));
		context.put("date", new Date().toString());
		context.put("esc", new EscapeTool());

		return infoAndCookie;
	}
}
