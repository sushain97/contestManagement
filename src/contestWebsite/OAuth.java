package contestWebsite;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.UserCookie;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;

@SuppressWarnings("serial")
public class OAuth extends HttpServlet
{
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("contestInfo");
		Entity contestInfo = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
		final String clientId = (String) contestInfo.getProperty("OAuth2ClientId");
		final String clientSecret = (String) contestInfo.getProperty("OAuth2ClientSecret");
		
		UserCookie userCookie = UserCookie.getCookie(req);
		if(userCookie.isAdmin())
		{
			ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
			getContent(req.getInputStream(), resultStream);
			String code = new String(resultStream.toByteArray(), "UTF-8");
			try
			{
				GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(), clientId, clientSecret, code, "postmessage").execute();
				contestInfo.setProperty("OAuth2Token", new Text(tokenResponse.toString()));
				datastore.put(contestInfo);
				
				resp.setStatus(HttpServletResponse.SC_OK);
			}
			catch(Exception e)
			{
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		else
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest Administrator privileges required for that operation");
	}
	
	private static void getContent(InputStream inputStream, ByteArrayOutputStream outputStream) throws IOException 
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		int readChar;
		while ((readChar = reader.read()) != -1) {
			outputStream.write(readChar);
		}
		reader.close();
	}
}
