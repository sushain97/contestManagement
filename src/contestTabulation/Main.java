package contestTabulation;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;
import com.google.gdata.client.Service;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

@SuppressWarnings("serial")
public class Main extends HttpServlet
{
	static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		List<Test> testsGraded = new ArrayList<Test>();
		final SpreadsheetEntry middle, high;
		
		final List<Student> middleStudents = new ArrayList<Student>();
		final Map<String, School> middleSchools = new HashMap<String, School>(); //School Name, School
		final Map<Test, List<Student>> middleCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Character, List<School>> middleCategorySweepstakesWinners = new HashMap<Character, List<School>>(); //Test topic {N, M, S, C}, School array
		final List<School> middleSweepstakesWinners = new ArrayList<School>();

		final List<Student> highStudents = new ArrayList<Student>();
		final Map<String, School> highSchools = new HashMap<String, School>(); //School Name, School
		final Map<Test, List<Student>> highCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Character, List<School>> highCategorySweepstakesWinners = new HashMap<Character, List<School>>(); //Test topic {N, M, S, C}, School array
		final List<School> highSweepstakesWinners = new ArrayList<School>();
		
		try
		{
			//Authenticate to Google Documents Service using account details from Administration Panel
			Map<String, String[]> params = req.getParameterMap();
			String user = params.get("docAccount")[0];
			String password = params.get("docPassword")[0];
			SpreadsheetService service = new SpreadsheetService("contestTabulation");
			service.setUserCredentials(user, password);

			//Populate base data structures by traversing Google Documents Spreadsheets
			middle = getSpreadSheet(params.get("docMiddle")[0], service);
			high = getSpreadSheet(params.get("docHigh")[0], service);
			updateDatabase(middle, middleStudents, middleSchools, testsGraded, service);
			updateDatabase(high, highStudents, highSchools, testsGraded, service);

			//Populate categoryWinners maps with top 20 scorers 
			tabulateCategoryWinners("middle", middleStudents, middleCategoryWinners, testsGraded);
			tabulateCategoryWinners("high", highStudents, highCategoryWinners, testsGraded);

			//Calculate school fields with sweepstakes scores and populate sorted sweekstakes maps & arrays with all schools
			for(School school : middleSchools.values())
				school.calculateScores();
			for(School school : highSchools.values())
				school.calculateScores();
			tabulateCategorySweepstakesWinners(middleSchools, middleCategorySweepstakesWinners);
			tabulateCategorySweepstakesWinners(highSchools, highCategorySweepstakesWinners);
			tabulateSweepstakesWinners(middleSchools, middleSweepstakesWinners);
			tabulateSweepstakesWinners(highSchools, highSweepstakesWinners);

			//Generate and store HTML in Datastore
			storeHTML("middle", middleStudents, middleSchools, middleCategoryWinners, middleCategorySweepstakesWinners, middleSweepstakesWinners);
			storeHTML("high", highStudents, highSchools, highCategoryWinners, highCategorySweepstakesWinners, highSweepstakesWinners);

			//Update Datastore by modifying registrations to include actual number of tests taken
			updateRegistrations("middle", middleSchools);
			updateRegistrations("high", highSchools);
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	private static SpreadsheetEntry getSpreadSheet(String docString, Service service) throws AuthenticationException, MalformedURLException, IOException, ServiceException
	{
		SpreadsheetFeed feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full"), SpreadsheetFeed.class);
		List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		for(SpreadsheetEntry spreadsheet : spreadsheets)
			if(spreadsheet.getTitle().getPlainText().equals(docString))
				return spreadsheet;
		return null;
	}

	private static void updateDatabase(SpreadsheetEntry spreadsheet, List<Student> students, Map<String, School> schools, List<Test> testsGraded, Service service) throws IOException, ServiceException
	{
		WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
		List<WorksheetEntry> worksheets = worksheetFeed.getEntries();

		for(WorksheetEntry worksheet : worksheets)
		{
			int grade = Integer.parseInt(worksheet.getTitle().getPlainText().split(" ")[0]);
			char subject = worksheet.getTitle().getPlainText().split(" ")[1].charAt(0);

			URL listFeedUrl = worksheet.getListFeedUrl();
			ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);
			if(listFeed.getEntries().size() > 0)
				testsGraded.add(Test.valueOf(Character.toString(subject) + grade));

			for (ListEntry r : listFeed.getEntries())
			{
				CustomElementCollection row = r.getCustomElements();
				String name = row.getValue("nameofstudent");
				if(name == null)
					break;
				else
					name = name.trim();
				String schoolName = row.getValue("school").trim();
				String score = row.getValue("score").trim();

				if(!schools.containsKey(schoolName))
					schools.put(schoolName, new School(schoolName, (grade > 8 ? "high" : "middle")));
				School school = schools.get(schoolName);

				Student temp = new Student(name, school, grade);
				if(!students.contains(temp))
				{
					school.addStudent(temp);
					students.add(temp);
				}
				else
					temp = students.get(students.indexOf(temp));

				temp.setScore(subject, new Score(score));
			}

			try
			{
				URL cellFeedUrl = new URI(worksheet.getCellFeedUrl().toString() + "?min-row=23&min-col=2&max-col=3").toURL();
				CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

				for(int i = 0; i < cellFeed.getEntries().size(); i+=2)
				{
					List<CellEntry> entries = cellFeed.getEntries();
					School school = schools.get(entries.get(i).getCell().getInputValue());
					String[] scores = entries.get(i+1).getCell().getInputValue().split(" ");
					ArrayList<Score> scoresArr = new ArrayList<Score>();
					for(String score : scores)
						if(score.length() != 0)
							scoresArr.add(new Score(score));
					school.addAnonScores(Test.valueOf(Character.toString(subject) + grade), scoresArr);
				}
			}
			catch(Exception e) { e.printStackTrace(); }
		}

	}

	private static void tabulateCategoryWinners(String level, List<Student> students, Map<Test, List<Student>> categoryWinners, List<Test> testsGraded)
	{
		for(Test test : testsGraded)
		{
			ArrayList<Student> winners = new ArrayList<Student>();
			int grade = test.grade();
			final String subject = test.test();
			
			for(Student student : students)
				if(student.getGrade() == grade && student.getScore(subject) != null)
					winners.add(student);

			Collections.sort(winners, new Comparator<Student>() { public int compare(Student s1, Student s2) { return s1.getScore(subject).compareTo(s2.getScore(subject)); }});
			Collections.reverse(winners);
			winners = new ArrayList<Student>(winners.subList(0, (winners.size() >= 20 ? 20 : winners.size())));
			if((level.equals("middle") && grade <= 8) || (level.equals("high") && grade >= 9))
				categoryWinners.put(test, winners);
		}
	}

	static void tabulateCategorySweepstakesWinners(Map<String, School> schools, Map<Character, List<School>> sweepstakeCategoryWinners)
	{
		char[] topics = {'S', 'C', 'N', 'M'};
		for(final char topic : topics)
		{
			ArrayList<School> schoolList = new ArrayList<School>(schools.values());
			Collections.sort(schoolList, new Comparator<School>() { public int compare(School s1, School s2) { return s1.getScore(topic) - s2.getScore(topic); }});
			Collections.reverse(schoolList);
			sweepstakeCategoryWinners.put(topic, schoolList);
		}
	}

	private static void tabulateSweepstakesWinners(Map<String, School> schools, List<School> sweepstakeWinners)
	{
		ArrayList<School> schoolList = new ArrayList<School>(schools.values());
		Collections.sort(schoolList, new Comparator<School>() { public int compare(School s1, School s2) { return s1.getTotalScore() - s2.getTotalScore(); }});
		Collections.reverse(schoolList);
		for(School school : schoolList)
			sweepstakeWinners.add(school);
	}

	@SuppressWarnings("deprecation")
	private static void storeHTML(String level, List<Student> students, Map<String, School> schools, Map<Test, List<Student>> categoryWinners, Map<Character, List<School>> categorySweepstakesWinners, List<School> sweepstakesWinners) throws IOException
	{
		Properties p = new Properties();
		p.setProperty("file.resource.loader.path", "html");
		Velocity.init(p);
		StringWriter sw;
		VelocityContext context;
		HtmlCompressor compressor = new HtmlCompressor();
		//TODO: Convert to Transaction
		Entity html;
		LinkedList<Entity> htmlEntries = new LinkedList<Entity>();
		
		try
		{
			for(Entry<String, School> schoolEntry : schools.entrySet())
			{
				if(!schoolEntry.getKey().equals("?"))
				{
					School school = schoolEntry.getValue();
					context = new VelocityContext();
					context.put("schoolLevel", Character.toString(school.getLevel().charAt(0)).toUpperCase() + school.getLevel().substring(1));
					ArrayList<Student> schoolStudents = school.getStudents();
					Collections.sort(schoolStudents, new Comparator<Student>() { public int compare(Student s1,Student s2) { return s1.getName().compareTo(s2.getName()); }});
					context.put("school", school);
					context.put("tests", Test.values());
					sw = new StringWriter();
					Velocity.mergeTemplate("schoolOverview.html", context, sw);
					html = new Entity("html", "school_" + level + "_" + school.getName());
					html.setProperty("level", level);
					html.setProperty("type", "school");
					html.setProperty("school", school.getName());
					html.setProperty("html", new Text(compressor.compress(sw.toString())));
					htmlEntries.add(html);
					sw.close();
				}
			}

			for(Test test : categoryWinners.keySet())
			{
				context = new VelocityContext();
				context.put("winners", categoryWinners.get(test));
				context.put("subject", test);
				sw = new StringWriter();
				Velocity.mergeTemplate("categoryWinners.html", context, sw);
				html = new Entity("html", "category_" + level + "_" + test);
				html.setProperty("type", "category");
				html.setProperty("level", level);
				html.setProperty("test", test.toString());
				html.setProperty("html", new Text(compressor.compress(sw.toString())));
				htmlEntries.add(html);
				sw.close();
			}

			context = new VelocityContext();
			context.put("winners", categorySweepstakesWinners);
			sw = new StringWriter();
			Velocity.mergeTemplate("categorySweepstakes.html", context, sw);
			html = new Entity("html", "categorySweep_" + level);
			html.setProperty("type", "categorySweep");
			html.setProperty("level", level);
			html.setProperty("html", new Text(compressor.compress(sw.toString())));
			htmlEntries.add(html);
			sw.close();

			context = new VelocityContext();
			context.put("winners", sweepstakesWinners);
			sw = new StringWriter();
			Velocity.mergeTemplate("sweepstakesWinners.html", context, sw);
			html = new Entity("html", "sweep_" + level);
			html.setProperty("type", "sweep");
			html.setProperty("level", level);
			html.setProperty("html", new Text(compressor.compress(sw.toString())));
			htmlEntries.add(html);
			sw.close();

			context = new VelocityContext();
			Collections.sort(students, new Comparator<Student>() { public int compare(Student s1,Student s2) { return s1.getName().compareTo(s2.getName()); }});
			context.put("students", students);
			sw = new StringWriter();
			Velocity.mergeTemplate("studentsOverview.html", context, sw);
			html = new Entity("html", "students_" + level);
			html.setProperty("type", "students");
			html.setProperty("level", level);
			html.setProperty("html", new Text(compressor.compress(sw.toString())));
			htmlEntries.add(html);
			sw.close();

			datastore.put(htmlEntries);
			
			Query query = new Query("contestInfo");
			Entity info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
			SimpleDateFormat isoFormat = new SimpleDateFormat("hh:mm:ss a EEEE MMMM d, yyyy zzzz");
		    isoFormat.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
			info.setProperty("updated", isoFormat.format(new Date()).toString());
			datastore.put(info);
		}
		catch(Exception e) { e.printStackTrace(); }
	}

	@SuppressWarnings("deprecation")
	private static void updateRegistrations(String level, Map<String, School> schools)
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		for(School school : schools.values())
		{
			Query query = new Query("registration")
									.addFilter("schoolName", FilterOperator.EQUAL, school.getName())
									.addFilter("schoolLevel", FilterOperator.EQUAL, level);
			List<Entity> registrations = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

			if(registrations.size() > 0)
			{
				school.calculateTestNums();
				Entity registration = registrations.get(0);
				for(Entry<Test, Integer> numTest : school.getNumTests().entrySet())
					registration.setProperty(numTest.getKey().toString(), numTest.getValue());
				datastore.put(registration);
			}
		}
	}
}