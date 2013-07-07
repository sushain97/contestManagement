package contestWebsite;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.Password;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

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
	}
}