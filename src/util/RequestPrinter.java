/*
 * Author: Pablo Santo Cruz (http://stackoverflow.com/users/67606/pablo-santa-cruz)
 * Source: http://blog.roshka.com/2011/07/logging-full-http-requests-in-java.html
 * Note: Slight modifications made to fit custom requirements.
 */

package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

public class RequestPrinter {
	
	public static String INDENT_UNIT = "&nbsp;&nbsp;&nbsp;&nbsp;";
	
	// Private helper methods
	
	@SuppressWarnings("rawtypes")
	private static String debugStringSession(HttpSession session, int indent)
	{
		String indentString = RequestPrinter.repeat(INDENT_UNIT, indent);
		if (session == null)
			return indentString + "{ }";
		StringBuilder sb = new StringBuilder();
		sb.append(indentString).append("{\n");
		sb.append(indentString).append(INDENT_UNIT).append("'id': '").append(session.getId()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'last_accessed_time': ").append(session.getLastAccessedTime()).append(", \n");
		sb.append(indentString).append(INDENT_UNIT).append("'max_inactive_interval': ").append(session.getMaxInactiveInterval()).append(", \n");
		sb.append(indentString).append(INDENT_UNIT).append("'is_new': '").append(session.isNew()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'attributes': {\n");
		Enumeration attributeNames = session.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			String attributeName = (String) attributeNames.nextElement();
			Object o = session.getAttribute(attributeName);
			sb.
				append(indentString).
				append(INDENT_UNIT).
				append("'").append(attributeName).append("': ").
				append("'").append(o.toString()).append("',\n");
		}
		sb.append(indentString).append(INDENT_UNIT).append("}\n");
		sb.append(indentString).append("}\n");
		return sb.toString();
	}
	
	private static String debugStringParameter(String indentString, String parameterName, String[] parameterValues)
	{
		StringBuilder sb = new StringBuilder();
		sb.
			append(indentString).
			append(INDENT_UNIT).
			append("'").append(parameterName).append("': ");
		if (parameterValues == null || parameterValues.length == 0) {
			sb.append("None");
		} else {
			if (parameterValues.length > 1) sb.append("[");
			sb.append(RequestPrinter.join(parameterValues, ","));
			if (parameterValues.length > 1) sb.append("]");
		}
		return sb.toString();
	}
	
	private static String debugStringHeader(String indentString, String headerName, List<String> headerValues)
	{
		StringBuilder sb = new StringBuilder();
		sb.
			append(indentString).
			append(INDENT_UNIT).
			append("'").append(headerName).append("': ");
		if (headerValues == null || headerValues.size() == 0) {
			sb.append("None");
		} else {
			if (headerValues.size() > 1) sb.append("[");
			sb.append(RequestPrinter.join(headerValues, ","));
			if (headerValues.size() > 1) sb.append("]");
		}
		return sb.toString();
	}
	
	@SuppressWarnings({ "rawtypes", "unused" })
	private static String debugStringParameters(HttpServletRequest request, int indent)
	{
		String indentString = RequestPrinter.repeat(INDENT_UNIT, indent);
		StringBuilder sb = new StringBuilder();
		sb.append(indentString).append("{\n");
		Enumeration parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String parameterName = (String) parameterNames.nextElement();
			String[] parameterValues = request.getParameterValues(parameterName);
			List<String> headerValuesList = new ArrayList<String>(); 
			sb.
				append(RequestPrinter.debugStringParameter(indentString, parameterName, parameterValues)).
				append(",\n");
		}
		sb.append(indentString).append("}\n");
		return sb.toString();
	}
	
	private static String debugStringCookie(Cookie cookie, String indentString)
	{
		if (cookie == null) return "";
		StringBuilder sb = new StringBuilder();
		sb.append(indentString).append("{ \n");
		sb.append(indentString).append(INDENT_UNIT).append("'name': '").append(cookie.getName()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'value': '").append(cookie.getValue()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'domain': '").append(cookie.getDomain()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'path': '").append(cookie.getPath()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'max_age': ").append(cookie.getMaxAge()).append(", \n");
		sb.append(indentString).append(INDENT_UNIT).append("'version': ").append(cookie.getVersion()).append(", \n");
		sb.append(indentString).append(INDENT_UNIT).append("'comment': '").append(cookie.getComment()).append("', \n");
		sb.append(indentString).append(INDENT_UNIT).append("'secure': '").append(cookie.getSecure()).append("',\n");
		sb.append(indentString).append("}");
		return sb.toString();
	}
	
	private static String debugStringCookies(HttpServletRequest request, int indent)
	{
		if (request.getCookies() == null)
			return "";
		String indentString = RequestPrinter.repeat(INDENT_UNIT, indent);
		StringBuilder sb = new StringBuilder();
		sb.append(indentString).append("[\n");
		int cookieCount = 0;
		for (Cookie cookie : request.getCookies()) {
			sb.append(RequestPrinter.debugStringCookie(cookie, indentString + INDENT_UNIT)).append(",\n");
			cookieCount++;
		}
		if (cookieCount > 0) {
			sb.delete(sb.length() - ",\n".length(), sb.length());
		}
		sb.append("\n").append(indentString).append("]\n");
		return sb.toString();
	}
	
	@SuppressWarnings("rawtypes")
	private static String debugStringHeaders(HttpServletRequest request, int indent)
	{
		String indentString = RequestPrinter.repeat(INDENT_UNIT, indent);
		StringBuilder sb = new StringBuilder();
		sb.append(indentString).append("{\n");
		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = (String) headerNames.nextElement();
			Enumeration headerValues = request.getHeaders(headerName);
			List<String> headerValuesList = new ArrayList<String>(); 
			while (headerValues.hasMoreElements()) {
				String headerValue = (String) headerValues.nextElement();
				headerValuesList.add(headerValue);
			}
			sb.
				append(RequestPrinter.debugStringHeader(indentString, headerName, headerValuesList)).
				append(",\n");
		}
		sb.append(indentString).append("}\n");
		return sb.toString();
	}

	public static String repeat(String what, int times)
	{
		return StringUtils.repeat(what, times);
	}
	
	public static String join(List<String> values, String conjuction)
	{
		return StringUtils.join(values, conjuction);
	}
	
	public static String join(String[] values, String conjuction)
	{
		return RequestPrinter.join(Arrays.asList(values), conjuction);
	}
	
	
	/**
	 * Debug request's headers
	 * @param request Request parameter.
	 * @return A string with debug information on Request's header
	 */
	public static String debugStringHeaders(HttpServletRequest request)
	{
		return RequestPrinter.debugStringHeaders(request, 0);
	}
	
	/**
	 * Debug request's parameters
	 * @param request Request parameter.
	 * @return A string with debug information on Request's header
	 */
	public static String debugStringParameters(HttpServletRequest request)
	{
		return RequestPrinter.debugStringParameters(request, 0);
	}
	
	/**
	 * Debug request's cookies
	 * @param request Request parameter
	 * @return A string with debug information on Request's cookies
	 */
	public static String debugStringCookies(HttpServletRequest request)
	{
		return RequestPrinter.debugStringCookies(request, 0);
	}

	/**
	 * 
	 * @param session
	 * @return
	 */
	public static String debugStringSession(HttpSession session)
	{
		return RequestPrinter.debugStringSession(session, 0);
	}
	
	/**
	 * Debug complete request
	 * @param request Request parameter.
	 * @param printSession Enable session information printing
	 * @return A string with debug information on Request's header
	 */
	public static String debugString(HttpServletRequest request, boolean printSession)
	{
		StringBuilder sb = new StringBuilder();
		
		// GENERAL INFO
		sb.append("<strong>PROTOCOL:</strong> ").append(request.getProtocol()).append("\n");
		sb.append("<strong>METHOD:</strong> ").append(request.getMethod()).append("\n");
		sb.append("<strong>QUERY STRING:</strong> ").append(request.getQueryString()).append("\n");
		sb.append("<strong>PATH INFO:</strong> ").append(request.getPathInfo()).append("\n");
		sb.append("<strong>PATH TRANSLATED:</strong> ").append(request.getPathTranslated()).append("\n");
		
		// COOKIES
		sb.append("<strong>COOKIES:</strong>\n");
		sb.append(RequestPrinter.debugStringCookies(request, 1));
		
		// PARAMETERS
		sb.append("<strong>PARAMETERS:</strong>\n");
		sb.append(RequestPrinter.debugStringParameters(request, 1));
		
		// HEADERS
		sb.append("<strong>HEADERS:</strong>\n");
		sb.append(RequestPrinter.debugStringHeaders(request, 1));
		
		// SESSION
		if (printSession) {
			sb.append("<strong>SESSION:</strong>\n");
			HttpSession session = request.getSession(false);
			if (session != null) {
				sb.append(RequestPrinter.debugStringSession(session, 1));
			} else {
				sb.append("NO SESSION AVAILABLE\n");
			}
		}
		
		return sb.toString();
	}

	/**
	 * Call debugString with 'false' value on session information
	 * @param request Request parameter.
	 * @return A string with debug information on Request's header but no information on session
	 */
	public static String debugString(HttpServletRequest request)
	{
		return RequestPrinter.debugString(request, false);
	}
}
