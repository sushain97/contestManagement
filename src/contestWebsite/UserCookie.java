package contestWebsite;

import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

public class UserCookie extends Cookie
{
	public UserCookie(String name, String value)
	{
		super(name, value);
	}

	public UserCookie(Cookie cookie)
	{
		super(cookie.getName(), cookie.getValue());
		setComment(cookie.getComment());
		String domain = cookie.getDomain();
		if(domain != null)
			setDomain(domain);
		setPath(cookie.getPath());
		setMaxAge(cookie.getMaxAge());
		setSecure(cookie.getSecure());
		setVersion(cookie.getVersion());
	}
	
	public static UserCookie getCookie(HttpServletRequest req)
	{
		Cookie[] cookies = req.getCookies();
		if(cookies != null)
			for(Cookie cookie : cookies)
				if(cookie.getName().equals("user-id"))
					return new UserCookie(cookie);
		return null;
	}

	public boolean authenticate()
	{
		try
		{
			String cookieContent = URLDecoder.decode(getValue(), "UTF-8");
			if(cookieContent.split("\\$").length != 2)
				return false;
			String user = cookieContent.split("\\$")[0];
			String hash = cookieContent.split("\\$")[1];
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			@SuppressWarnings("deprecation")
			Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, user);
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
			return users.size() != 0 && users.get(0).getProperty("hash").equals(hash);
		}
		catch(Exception e)
		{
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public Entity authenticateUser()
	{
		try
		{
			String cookieContent = URLDecoder.decode(getValue(), "UTF-8");
			if(cookieContent.split("\\$").length != 2)
				return null;
			String user = cookieContent.split("\\$")[0];
			String hash = cookieContent.split("\\$")[1];
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, user);
			List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
			if(users.size() != 0 && users.get(0).getProperty("hash").equals(hash))
				return users.get(0);
			else
				return null;
		}
		catch(Exception e)
		{
			return null;
		}
	}

}
