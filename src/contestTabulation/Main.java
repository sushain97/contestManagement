package contestTabulation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

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
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

@SuppressWarnings("serial")
public class Main extends HttpServlet
{
	static SpreadsheetService service;
	static SpreadsheetFeed feed;
	static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	static Scanner input = new Scanner(new InputStreamReader(System.in));

	static ArrayList<Test> testsGraded = new ArrayList<Test>();

	static SpreadsheetEntry middle;
	static ArrayList<Student> middleStudents = new ArrayList<Student>();
	static HashMap<String, School> middleSchools = new HashMap<String, School>(); //School Name, School
	static HashMap<Test, ArrayList<Student>> middleCategoryWinners = new HashMap<Test, ArrayList<Student>>();
	static HashMap<Character, ArrayList<School>> middleCategorySweepstakesWinners = new HashMap<Character, ArrayList<School>>(); //Test topic {N, M, S, C}, School array
	static ArrayList<School> middleSweepstakesWinners = new ArrayList<School>();

	static SpreadsheetEntry high;
	static ArrayList<Student> highStudents = new ArrayList<Student>();
	static HashMap<String, School> highSchools = new HashMap<String, School>(); //School Name, School
	static HashMap<Test, ArrayList<Student>> highCategoryWinners = new HashMap<Test, ArrayList<Student>>();
	static HashMap<Character, ArrayList<School>> highCategorySweepstakesWinners = new HashMap<Character, ArrayList<School>>(); //Test topic {N, M, S, C}, School array
	static ArrayList<School> highSweepstakesWinners = new ArrayList<School>();

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		try
		{
			//Authenticate to Google Documents Service using account details from Administration Panel
			Map<String, String[]> params = req.getParameterMap();
			String user = params.get("docAccount")[0];
			String password = params.get("docPassword")[0];
			service = new SpreadsheetService("contestTabulation");
			service.setUserCredentials(user, password);

			//Populate base data structures by traversing Google Documents Spreadsheets
			getSpreadSheets(params.get("docMiddle")[0], params.get("docHigh")[0]);
			updateDatabase("middle");
			updateDatabase("high");

			//Populate categoryWinners maps with top 20 scorers 
			tabulateCategoryWinners("middle");
			tabulateCategoryWinners("high");
			
			//Calculate school fields with sweepstakes scores and populate sorted sweekstakes maps & arrays with all schools
			for(School school : middleSchools.values())
				school.calculateScores();
			for(School school : highSchools.values())
				school.calculateScores();
			tabulateCategorySweepstakesWinners("middle");
			tabulateCategorySweepstakesWinners("high");
			tabulateSweepstakesWinners("middle");
			tabulateSweepstakesWinners("high");
			
			//Generate and store HTML in Datastore
			storeHTML("middle");
			storeHTML("high");
			
			//Update Datastore by modifying registrations to include actual number of tests taken
			updateRegistrations("middle");
			updateRegistrations("high");
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	private static void getSpreadSheets(String docMid, String docHigh) throws AuthenticationException, MalformedURLException, IOException, ServiceException
	{
		feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full"), SpreadsheetFeed.class);
		List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		for(SpreadsheetEntry spreadsheet : spreadsheets)
			if(spreadsheet.getTitle().getPlainText().equals(docMid))
				middle = spreadsheet;
			else if(spreadsheet.getTitle().getPlainText().equals(docHigh))
				high = spreadsheet;
	}
	
	private static void updateDatabase(String level) throws IOException, ServiceException
	{
		try
		{
			SpreadsheetEntry spreadsheet;
			ArrayList<Student> students;
			HashMap<String, School> schools;
			if(level.equals("middle"))
			{
				spreadsheet = middle;
				students = middleStudents;
				schools = middleSchools;
			}
			else
			{
				spreadsheet = high;
				students = highStudents;
				schools = highSchools;
			}
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
				for (ListEntry row : listFeed.getEntries())
				{
					String name = row.getCustomElements().getValue("nameofstudent");
					if(name == null)
						break;
					else
						name = name.trim();
					String schoolName = row.getCustomElements().getValue("school").trim();
					String score = row.getCustomElements().getValue("score").trim();
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
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	private static void tabulateCategoryWinners(String level)
	{
		for(Test test : testsGraded)
		{
			ArrayList<Student> winners = new ArrayList<Student>();
			int grade = test.grade();
			final String subject = test.test();

			ArrayList<Student> students;
			HashMap<Test, ArrayList<Student>> categoryWinners;
			if(level.equals("middle"))
			{
				students = middleStudents;
				categoryWinners = middleCategoryWinners;
			}
			else
			{
				students = highStudents;
				categoryWinners = highCategoryWinners;
			}

			for(Student student : students)
				if(student.getGrade() == grade && student.getScore(subject) != null)
					winners.add(student);

			Collections.sort(winners, new Comparator<Student>() { public int compare(Student s1, Student s2) { return s1.getScore(subject).compareTo(s2.getScore(subject)); }});
			Collections.reverse(winners);
			winners = new ArrayList<Student>(winners.subList(0, (winners.size() >= 20 ? 20 : winners.size())));
			if(level.equals("middle") && grade <= 8)
				categoryWinners.put(test, winners);
			else if(level.equals("high") && grade >= 9)
				categoryWinners.put(test, winners);
		}
	}
	
	static void tabulateCategorySweepstakesWinners(String level)
	{
		HashMap<String, School> schools;
		HashMap<Character, ArrayList<School>> sweepstakeCategoryWinners;
		if(level.equals("middle"))
		{
			schools = middleSchools;
			sweepstakeCategoryWinners = middleCategorySweepstakesWinners;
		}
		else
		{
			schools = highSchools;
			sweepstakeCategoryWinners = highCategorySweepstakesWinners;
		}
		char[] topics = {'S', 'C', 'N', 'M'};
		for(final char topic : topics)
		{
			List<School> schoolList = new ArrayList<School>(schools.values());
			Collections.sort(schoolList, new Comparator<School>() { public int compare(School s1, School s2) { return s1.getScore(topic) - s2.getScore(topic); }});
			Collections.reverse(schoolList);
			sweepstakeCategoryWinners.put(topic, (ArrayList<School>) schoolList);
		}
	}
	
	private static void tabulateSweepstakesWinners(String level)
	{
		HashMap<String, School> schools;
		ArrayList<School> sweepstakeWinners;
		if(level.equals("middle"))
		{
			schools = middleSchools;
			sweepstakeWinners = middleSweepstakesWinners;
		}
		else
		{
			schools = highSchools;
			sweepstakeWinners = highSweepstakesWinners;
		}
		List<School> schoolList = new ArrayList<School>(schools.values());
		Collections.sort(schoolList, new Comparator<School>() { public int compare(School s1, School s2) { return s1.getTotalScore() - s2.getTotalScore(); }});
		Collections.reverse(schoolList);
		for(School school : schoolList)
			sweepstakeWinners.add(school);
	}

	@SuppressWarnings("deprecation")
	private static void storeHTML(String level) throws IOException
	{
		ArrayList<Student> students;
		HashMap<String, School> schools;
		HashMap<Test, ArrayList<Student>> categoryWinners;
		HashMap<Character, ArrayList<School>> categorySweepstakesWinners;
		ArrayList<School> sweepstakesWinners;
		if(level.equals("middle"))
		{
			students = middleStudents;
			schools = middleSchools;
			categoryWinners = middleCategoryWinners;
			categorySweepstakesWinners = middleCategorySweepstakesWinners;
			sweepstakesWinners = middleSweepstakesWinners;
		}
		else
		{
			students = highStudents;
			schools = highSchools;
			categoryWinners = highCategoryWinners;
			categorySweepstakesWinners = highCategorySweepstakesWinners;
			sweepstakesWinners = highSweepstakesWinners;
		}

		Properties p = new Properties();
		p.setProperty("file.resource.loader.path", "html");
		Velocity.init(p);
		StringWriter sw;
		VelocityContext context;
		//TODO: Convert to Transaction
		Entity html;
		LinkedList<Entity> htmlEntries = new LinkedList<Entity>();

		try
		{
			for(School school : schools.values())
			{
				if(!school.getName().equals("?"))
				{
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
					html.setProperty("html", new Text(sw.toString()));
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
				html.setProperty("html", new Text(sw.toString()));
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
			html.setProperty("html", new Text(sw.toString()));
			htmlEntries.add(html);
			sw.close();

			context = new VelocityContext();
			context.put("winners", sweepstakesWinners);
			sw = new StringWriter();
			Velocity.mergeTemplate("sweepstakesWinners.html", context, sw);
			html = new Entity("html", "sweep_" + level);
			html.setProperty("type", "sweep");
			html.setProperty("level", level);
			html.setProperty("html", new Text(sw.toString()));
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
			html.setProperty("html", new Text(sw.toString()));
			htmlEntries.add(html);
			sw.close();

			datastore.put(htmlEntries);
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	@SuppressWarnings("deprecation")
	private static void updateRegistrations(String level)
	{
		HashMap<String, School> schools;
		if(level.equals("middle"))
			schools = middleSchools;
		else
			schools = highSchools;
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		for(School school : schools.values())
		{
			Query query = new Query("registration").addFilter("schoolName", FilterOperator.EQUAL, school.getName());
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