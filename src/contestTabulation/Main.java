/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */
	
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.Pair;

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
		//TODO: Add Logging
		List<Test> testsGraded = new ArrayList<Test>();
		final SpreadsheetEntry middle, high;
		
		final List<Student> middleStudents = new ArrayList<Student>();
		final Map<String, School> middleSchools = new HashMap<String, School>(); //School Name, School
		final Map<Test, List<Student>> middleCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Character, List<School>> middleCategorySweepstakesWinners = new HashMap<Character, List<School>>(); //Test topic {N, M, S, C}, School array
		final List<School> middleSweepstakesWinners = new ArrayList<School>();
		final Map<Test, List<Score>> middleAnonScores = new HashMap<Test, List<Score>>();

		final List<Student> highStudents = new ArrayList<Student>();
		final Map<String, School> highSchools = new HashMap<String, School>(); //School Name, School
		final Map<Test, List<Student>> highCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Character, List<School>> highCategorySweepstakesWinners = new HashMap<Character, List<School>>(); //Test topic {N, M, S, C}, School array
		final List<School> highSweepstakesWinners = new ArrayList<School>();
		final Map<Test, List<Score>> highAnonScores = new HashMap<Test, List<Score>>();
		
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
			updateDatabase(middle, middleStudents, middleSchools, middleAnonScores, testsGraded, service);
			updateDatabase(high, highStudents, highSchools, highAnonScores, testsGraded, service);

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
			storeHTML("middle", middleStudents, middleSchools, middleCategoryWinners, middleCategorySweepstakesWinners, middleSweepstakesWinners, middleAnonScores);
			storeHTML("high", highStudents, highSchools, highCategoryWinners, highCategorySweepstakesWinners, highSweepstakesWinners, highAnonScores);

			//Update Datastore by modifying registrations to include actual number of tests taken
			updateRegistrations("middle", middleSchools);
			updateRegistrations("high", highSchools);
			
			//Update Datastore by modifying contest information entity to include tests graded
			updateContestInfo(testsGraded);
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

	private static void updateDatabase(SpreadsheetEntry spreadsheet, List<Student> students, Map<String, School> schools, Map<Test, List<Score>> anonScores, List<Test> testsGraded, Service service) throws IOException, ServiceException
	{
		WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
		List<WorksheetEntry> worksheets = worksheetFeed.getEntries();

		for(WorksheetEntry worksheet : worksheets)
		{
			int grade = Integer.parseInt(worksheet.getTitle().getPlainText().split(" ")[0]);
			char subject = worksheet.getTitle().getPlainText().split(" ")[1].charAt(0);
			Test test = Test.valueOf(Character.toString(subject) + grade);

			URL listFeedUrl = worksheet.getListFeedUrl();
			ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);
			if(listFeed.getEntries().size() > 0)
				testsGraded.add(Test.valueOf(Character.toString(subject) + grade));

			for (ListEntry r : listFeed.getEntries())
			{
				CustomElementCollection row = r.getCustomElements();
				String name = row.getValue("nameofstudent");
				if(name == null)
				{
					if(anonScores.containsKey(test) && row.getValue("score") != null)
						anonScores.get(test).add(new Score(row.getValue("score").trim()));
					else if(row.getValue("score") != null)
					{
						ArrayList<Score> scoreList = new ArrayList<Score>();
						scoreList.add(new Score(row.getValue("score").trim()));
						anonScores.put(test, scoreList);
					}
					break;
				}
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
					
					String schoolName = entries.get(i).getCell().getInputValue();
					if(!schools.containsKey(schoolName))
						schools.put(schoolName, new School(schoolName, (grade > 8 ? "high" : "middle")));
					School school = schools.get(schoolName);
					
					String[] scores = entries.get(i+1).getCell().getInputValue().split(" ");
					ArrayList<Score> scoresArr = new ArrayList<Score>();
					for(String score : scores)
						if(score != null && score.length() != 0)
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

	private static void storeHTML(String level, List<Student> students, Map<String, School> schools, Map<Test, List<Student>> categoryWinners, Map<Character, List<School>> categorySweepstakesWinners, List<School> sweepstakesWinners, Map<Test, List<Score>> anonScores) throws IOException
	{
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/templates, html/snippets");
		ve.init();
		Template t;
		StringWriter sw;
		VelocityContext context;
		HtmlCompressor compressor = new HtmlCompressor();
		Entity html;
		LinkedList<Entity> htmlEntries = new LinkedList<Entity>();
		
		for(Entry<String, School> schoolEntry : schools.entrySet())
		{
			if(!schoolEntry.getKey().equals("?"))
			{
				School school = schoolEntry.getValue();
				context = new VelocityContext();
				context.put("schoolLevel", Character.toString(school.getLevel().charAt(0)).toUpperCase() + school.getLevel().substring(1));
				ArrayList<Student> schoolStudents = school.getStudents();
				Collections.sort(schoolStudents, new Comparator<Student>() { public int compare(Student s1,Student s2) { return s1.getName().compareTo(s2.getName()); }});
				
				Test[] tests = level.equals("middle") ? Test.middleTests() : Test.highTests();
				HashMap<Test,List<Integer>> scores = new HashMap<Test,List<Integer>>();
				for(Test test : tests)
					scores.put(test, new ArrayList<Integer>());
				
				for(Student student : school.getStudents())
						for(Entry<Character, Score> scoreEntry : student.getScores().entrySet())
							scores.get(Test.valueOf(scoreEntry.getKey().toString() + student.getGrade())).add(scoreEntry.getValue().getScoreNum());
				
				for(Entry<Test, ArrayList<Score>> anonScoreEntry : school.getAnonScores().entrySet())
					for(Score score : anonScoreEntry.getValue())
						scores.get(anonScoreEntry.getKey()).add(score.getScoreNum());
				
				HashMap<Test,List<Integer>> summaryStats = new HashMap<Test,List<Integer>>();
				HashMap<Test,List<Integer>> outliers = new HashMap<Test,List<Integer>>();
				for(Entry<Test,List<Integer>> scoreEntry : scores.entrySet())
				{
					Pair<List<Integer>,List<Integer>> stats = calculateStats(scoreEntry.getValue());
					summaryStats.put(scoreEntry.getKey(), stats.x);
					outliers.put(scoreEntry.getKey(), stats.y);
				}
					
				context.put("summaryStats", summaryStats);
				context.put("outliers", outliers);
				context.put("tests", tests);
				context.put("subjects", Test.tests());
				context.put("school", school);
				context.put("level", level);
				
				sw = new StringWriter();
				t = ve.getTemplate("schoolOverview.html");
				t.merge(context, sw);
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
			t = ve.getTemplate("categoryWinners.html");
			t.merge(context, sw);
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
		t = ve.getTemplate("categorySweepstakes.html");
		t.merge(context, sw);
		html = new Entity("html", "categorySweep_" + level);
		html.setProperty("type", "categorySweep");
		html.setProperty("level", level);
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();

		context = new VelocityContext();
		context.put("winners", sweepstakesWinners);
		sw = new StringWriter();
		t = ve.getTemplate("sweepstakesWinners.html");
		t.merge(context, sw);
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
		t = ve.getTemplate("studentsOverview.html"); //TODO: Display Anonymous Scores here
		t.merge(context, sw);
		html = new Entity("html", "students_" + level);
		html.setProperty("type", "students");
		html.setProperty("level", level);
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();
		
		HashMap<Test,List<Integer>> scores = new HashMap<Test,List<Integer>>();
		Test[] tests = level.equals("middle") ? Test.middleTests() : Test.highTests();
		for(Test test : tests)
			scores.put(test, new ArrayList<Integer>());
		
		for(School school : schools.values())
		{
			for(Student student : school.getStudents())
				for(Entry<Character, Score> scoreEntry : student.getScores().entrySet())
					scores.get(Test.valueOf(scoreEntry.getKey().toString() + student.getGrade())).add(scoreEntry.getValue().getScoreNum());
			
			for(Entry<Test, ArrayList<Score>> anonScoreEntry : school.getAnonScores().entrySet())
				for(Score score : anonScoreEntry.getValue())
					scores.get(anonScoreEntry.getKey()).add(score.getScoreNum());
		}
		
		for(Entry<Test,List<Score>> scoreEntry : anonScores.entrySet())
			for(Score score : scoreEntry.getValue())
				scores.get(scoreEntry.getKey()).add(score.getScoreNum());
		
		HashMap<Test,List<Integer>> summaryStats = new HashMap<Test,List<Integer>>();
		HashMap<Test,List<Integer>> outliers = new HashMap<Test,List<Integer>>();
		for(Entry<Test,List<Integer>> scoreEntry : scores.entrySet())
		{
			Pair<List<Integer>,List<Integer>> stats = calculateStats(scoreEntry.getValue());
			summaryStats.put(scoreEntry.getKey(), stats.x);
			outliers.put(scoreEntry.getKey(), stats.y);
		}
			
		context = new VelocityContext();
		context.put("summaryStats", summaryStats);
		context.put("outliers", outliers);
		context.put("tests", tests);
		context.put("subjects", Test.tests());
		context.put("level", level);
		sw = new StringWriter();
		t = ve.getTemplate("visualizations.html");
		t.merge(context, sw);
		html = new Entity("html", "visualizations_" + level);
		html.setProperty("type", "visualizations");
		html.setProperty("level", level);
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();
		
		datastore.put(htmlEntries); //TODO: Convert to Transaction
	}

	private static Pair<List<Integer>,List<Integer>> calculateStats(List<Integer> list)
	{
		double[] data = new double[list.size()];
		for(int i = 0; i < list.size(); i++)
			data[i] = list.get(i);
		DescriptiveStatistics dStats = new DescriptiveStatistics(data);
		
		List<Integer> summary = new ArrayList<Integer>(5);
		summary.add((int) dStats.getMin()); //Minimum
		summary.add((int) dStats.getPercentile(25)); //Lower Quartile (Q1)
		summary.add((int) dStats.getPercentile(50)); //Middle Quartile (Median - Q2)
		summary.add((int) dStats.getPercentile(75)); //High Quartile (Q3)
		summary.add((int) dStats.getMax()); //Maxiumum
		
		List<Integer> outliers = new ArrayList<Integer>();
		if(list.size() > 5 && dStats.getStandardDeviation() > 0) //Only remove outliers if relatively normal
		{
			double mean = dStats.getMean();
			double stDev = dStats.getStandardDeviation();
			NormalDistribution normalDistribution = new NormalDistribution(mean, stDev);
			
			Iterator<Integer> listIterator = list.iterator();
			double significanceLevel = .50 / list.size(); //Chauvenet's Criterion for Outliers
			while(listIterator.hasNext())
			{
				int num = listIterator.next();
				double pValue = normalDistribution.cumulativeProbability(num);
				if(pValue < significanceLevel)
				{
					outliers.add(num);
					listIterator.remove();
				}
			}
			
			if(list.size() != dStats.getN()) //If and only if outliers have been removed
			{
				double[] significantData = new double[list.size()];
				for(int i = 0; i < list.size(); i++)
					significantData[i] = list.get(i);
				dStats = new DescriptiveStatistics(significantData);
				summary.set(0, (int) dStats.getMin());
				summary.set(4, (int) dStats.getMax());
			}
		}
		
		return new Pair<List<Integer>,List<Integer>>(summary, outliers);
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
	
	private static void updateContestInfo(List<Test> testsGraded)
	{
		Query query = new Query("contestInfo");
		Entity info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
		SimpleDateFormat isoFormat = new SimpleDateFormat("hh:mm:ss a EEEE MMMM d, yyyy zzzz");
		isoFormat.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
		info.setProperty("updated", isoFormat.format(new Date()).toString());
		info.setProperty("testsGraded", testsGraded.toString());
		datastore.put(info);
	}
}
