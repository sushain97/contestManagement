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
 * GNU General Public License for more destails.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [http://www.gnu.org/licenses/].
 */

package contestTabulation;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.gdata.client.Service;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
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
public class Main extends HttpServlet {
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	private static final HttpTransport httpTransport = new NetHttpTransport();
	private static final JacksonFactory jsonFactory = new JacksonFactory();

	@Override
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// TODO: Add Logging
		final Set<Test> testsGraded = new HashSet<Test>();
		final SpreadsheetEntry middle, high;

		final Map<String, Integer> awardCriteria = new HashMap<String, Integer>();

		final List<Student> middleStudents = new ArrayList<Student>();
		final Map<String, School> middleSchools = new HashMap<String, School>();
		final Map<Test, List<Student>> middleCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Subject, List<School>> middleCategorySweepstakesWinners = new HashMap<Subject, List<School>>();
		final List<School> middleSweepstakesWinners = new ArrayList<School>();
		final Map<Test, List<Score>> middleAnonScores = new HashMap<Test, List<Score>>();

		final List<Student> highStudents = new ArrayList<Student>();
		final Map<String, School> highSchools = new HashMap<String, School>();
		final Map<Test, List<Student>> highCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Subject, List<School>> highCategorySweepstakesWinners = new HashMap<Subject, List<School>>();
		final List<School> highSweepstakesWinners = new ArrayList<School>();
		final Map<Test, List<Score>> highAnonScores = new HashMap<Test, List<Score>>();

		try {
			// Authenticate to Google Documents Service using OAuth 2.0 Authentication Token from Datastore
			Map<String, String[]> params = req.getParameterMap();
			SpreadsheetService service = new SpreadsheetService("contestTabulation");
			authService(service);

			// Populate base data structures by traversing Google Documents Spreadsheets
			middle = getSpreadSheet(params.get("docMiddle")[0], service);
			high = getSpreadSheet(params.get("docHigh")[0], service);
			updateDatabase(Level.MIDDLE, middle, middleStudents, middleSchools, middleAnonScores, testsGraded, service);
			updateDatabase(Level.HIGH, high, highStudents, highSchools, highAnonScores, testsGraded, service);

			// Populate categoryWinners maps with top 20 scorers
			tabulateCategoryWinners(Level.MIDDLE, middleStudents, middleCategoryWinners, testsGraded);
			tabulateCategoryWinners(Level.HIGH, highStudents, highCategoryWinners, testsGraded);

			// Calculate school fields with sweepstakes scores and populate sorted sweekstakes maps & arrays with all schools
			for (School school : middleSchools.values()) {
				school.calculateScores();
			}
			for (School school : highSchools.values()) {
				school.calculateScores();
			}
			tabulateCategorySweepstakesWinners(middleSchools, middleCategorySweepstakesWinners);
			tabulateCategorySweepstakesWinners(highSchools, highCategorySweepstakesWinners);
			tabulateSweepstakesWinners(middleSchools, middleSweepstakesWinners);
			tabulateSweepstakesWinners(highSchools, highSweepstakesWinners);

			// Get award criteria from Datastore
			getAwardCriteria(awardCriteria);

			// Generate and store HTML in Datastore
			storeHTML(Level.MIDDLE, middleStudents, middleSchools, middleCategoryWinners, middleCategorySweepstakesWinners, middleSweepstakesWinners, middleAnonScores, awardCriteria);
			storeHTML(Level.HIGH, highStudents, highSchools, highCategoryWinners, highCategorySweepstakesWinners, highSweepstakesWinners, highAnonScores, awardCriteria);

			// Update Datastore by modifying registrations to include actual number of tests taken
			updateRegistrations(Level.MIDDLE, middleSchools);
			updateRegistrations(Level.HIGH, highSchools);

			// Update Datastore by modifying contest information entity to include tests graded
			updateContestInfo(testsGraded);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void authService(SpreadsheetService service) throws IOException {
		Query query = new Query("contestInfo");
		Entity contestInfo = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);

		String clientSecret = (String) contestInfo.getProperty("OAuth2ClientSecret");
		String clientId = (String) contestInfo.getProperty("OAuth2ClientId");
		String authToken = ((Text) contestInfo.getProperty("OAuth2Token")).getValue();

		GoogleCredential credential = new GoogleCredential.Builder()
			.setJsonFactory(jsonFactory)
			.setTransport(httpTransport)
			.setClientSecrets(clientId, clientSecret)
			.build()
			.setFromTokenResponse(new JacksonFactory().fromString(authToken, GoogleTokenResponse.class));

		service.setOAuth2Credentials(credential);
	}

	private static SpreadsheetEntry getSpreadSheet(String docString, Service service) throws AuthenticationException, MalformedURLException, IOException, ServiceException {
		SpreadsheetFeed feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full"), SpreadsheetFeed.class);
		List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		for (SpreadsheetEntry spreadsheet : spreadsheets) {
			if (spreadsheet.getTitle().getPlainText().equals(docString)) {
				return spreadsheet;
			}
		}
		return null;
	}

	private static void updateDatabase(Level level, SpreadsheetEntry spreadsheet, List<Student> students, Map<String, School> schools, Map<Test, List<Score>> anonScores, Set<Test> testsGraded, Service service) throws IOException, ServiceException {
		WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
		List<WorksheetEntry> worksheets = worksheetFeed.getEntries();

		for (WorksheetEntry worksheet : worksheets) {
			String schoolName = worksheet.getTitle().getPlainText();
			School school = new School(schoolName, level);
			schools.put(schoolName, school);

			URL listFeedUrl = worksheet.getListFeedUrl();
			ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);

			for (ListEntry r : listFeed.getEntries()) {
				try {
					CustomElementCollection row = r.getCustomElements();

					String name = row.getValue("name").trim();
					int grade = Integer.parseInt(row.getValue("grade").trim());

					Student student = new Student(name, school, grade);
					students.add(student);

					for (Subject subject : Subject.getSubjects()) {
						String score = row.getValue(subject.toString());
						if (score != null && Score.isScore(score.trim())) {
							student.setScore(subject, new Score(score));
							testsGraded.add(Test.fromSubjectAndGrade(grade, subject));
						}
					}

					school.addStudent(student);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void tabulateCategoryWinners(Level level, List<Student> students, Map<Test, List<Student>> categoryWinners, Set<Test> testsGraded) {
		for (Test test : testsGraded) {
			ArrayList<Student> winners = new ArrayList<Student>();
			int grade = test.getGrade();
			final Subject subject = test.getSubject();

			for (Student student : students) {
				if (student.getGrade() == grade && student.getScore(subject) != null) {
					winners.add(student);
				}
			}

			Collections.sort(winners, Student.getScoreComparator(subject));
			Collections.reverse(winners);
			winners = new ArrayList<Student>(winners.subList(0, winners.size() >= 20 ? 20 : winners.size()));
			if (level == Level.MIDDLE && grade <= level.getHighGrade() || level == Level.HIGH && grade >= level.getLowGrade()) {
				categoryWinners.put(test, winners);
			}
		}
	}

	static void tabulateCategorySweepstakesWinners(Map<String, School> schools, Map<Subject, List<School>> highCategorySweepstakesWinners) {
		for (final Subject subject : Subject.getSubjects()) {
			ArrayList<School> schoolList = new ArrayList<School>(schools.values());
			Collections.sort(schoolList, School.getScoreComparator(subject));
			Collections.reverse(schoolList);
			highCategorySweepstakesWinners.put(subject, schoolList);
		}
	}

	private static void tabulateSweepstakesWinners(Map<String, School> schools, List<School> sweepstakeWinners) {
		ArrayList<School> schoolList = new ArrayList<School>(schools.values());
		Collections.sort(schoolList, School.getTotalScoreComparator());
		Collections.reverse(schoolList);
		for (School school : schoolList) {
			sweepstakeWinners.add(school);
		}
	}

	@SuppressWarnings("unchecked")
	private static void getAwardCriteria(Map<String, Integer> awardCriteria) {
		Query query = new Query("contestInfo");
		Entity contestInfo = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);

		JSONObject awardCriteriaJSON = null;
		try {
			awardCriteriaJSON = new JSONObject(((Text) contestInfo.getProperty("awardCriteria")).getValue());
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		Iterator<String> awardCountKeyIter = awardCriteriaJSON.keys();
		while (awardCountKeyIter.hasNext()) {
			String awardCountType = awardCountKeyIter.next();
			try {
				awardCriteria.put(awardCountType, (Integer) awardCriteriaJSON.get(awardCountType));
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private static void storeHTML(Level level, List<Student> students, Map<String, School> schools, Map<Test, List<Student>> categoryWinners, Map<Subject, List<School>> middleCategorySweepstakesWinners, List<School> sweepstakesWinners, Map<Test, List<Score>> anonScores, Map<String, Integer> awardCriteria) throws IOException {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/templates, html/snippets");
		ve.init();
		Template t;
		StringWriter sw;
		VelocityContext context;
		HtmlCompressor compressor = new HtmlCompressor();
		Entity html;
		LinkedList<Entity> htmlEntries = new LinkedList<Entity>();

		for (Entry<String, School> schoolEntry : schools.entrySet()) {
			if (!schoolEntry.getKey().equals("?")) {
				School school = schoolEntry.getValue();
				context = new VelocityContext();
				context.put("schoolLevel", Character.toString(school.getLevel().toString().charAt(0)).toUpperCase() + school.getLevel().toString().substring(1));
				ArrayList<Student> schoolStudents = school.getStudents();
				Collections.sort(schoolStudents, Student.getNameComparator());

				Test[] tests = level == Level.MIDDLE ? Test.middleTests() : Test.highTests();
				HashMap<Test, List<Integer>> scores = new HashMap<Test, List<Integer>>();
				for (Test test : tests) {
					scores.put(test, new ArrayList<Integer>());
				}

				for (Student student : school.getStudents()) {
					for (Entry<Subject, Score> scoreEntry : student.getScores().entrySet()) {
						scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
					}
				}

				for (Entry<Test, ArrayList<Score>> anonScoreEntry : school.getAnonScores().entrySet()) {
					for (Score score : anonScoreEntry.getValue()) {
						scores.get(anonScoreEntry.getKey()).add(score.getScoreNum());
					}
				}

				HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
				HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
				for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
					Pair<List<Integer>, List<Integer>> stats = calculateStats(scoreEntry.getValue());
					summaryStats.put(scoreEntry.getKey(), stats.x);
					outliers.put(scoreEntry.getKey(), stats.y);
				}

				context.put("summaryStats", summaryStats);
				context.put("outliers", outliers);
				context.put("tests", tests);
				context.put("subjects", Subject.getSubjects());
				context.put("school", school);
				context.put("level", level.toString());

				sw = new StringWriter();
				t = ve.getTemplate("schoolOverview.html");
				t.merge(context, sw);
				html = new Entity("html", "school_" + level + "_" + school.getName());
				html.setProperty("level", level.toString());
				html.setProperty("type", "school");
				html.setProperty("school", school.getName());
				html.setProperty("html", new Text(compressor.compress(sw.toString())));
				htmlEntries.add(html);
				sw.close();
			}
		}

		for (Test test : categoryWinners.keySet()) {
			context = new VelocityContext();
			context.put("winners", categoryWinners.get(test));
			context.put("test", test);
			context.put("trophy", awardCriteria.get("category_" + level + "_trophy"));
			context.put("medal", awardCriteria.get("category_" + level + "_medal"));
			sw = new StringWriter();
			t = ve.getTemplate("categoryWinners.html");
			t.merge(context, sw);
			html = new Entity("html", "category_" + level + "_" + test);
			html.setProperty("type", "category");
			html.setProperty("level", level.toString());
			html.setProperty("test", test.toString());
			html.setProperty("html", new Text(compressor.compress(sw.toString())));
			htmlEntries.add(html);
			sw.close();
		}

		context = new VelocityContext();
		context.put("winners", middleCategorySweepstakesWinners);
		context.put("trophy", awardCriteria.get("categorySweep_" + level));
		sw = new StringWriter();
		t = ve.getTemplate("categorySweepstakes.html");
		t.merge(context, sw);
		html = new Entity("html", "categorySweep_" + level);
		html.setProperty("type", "categorySweep");
		html.setProperty("level", level.toString());
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();

		context = new VelocityContext();
		context.put("winners", sweepstakesWinners);
		context.put("trophy", awardCriteria.get("sweepstakes_" + level));
		context.put("subjects", Subject.getSubjects());
		sw = new StringWriter();
		t = ve.getTemplate("sweepstakesWinners.html");
		t.merge(context, sw);
		html = new Entity("html", "sweep_" + level);
		html.setProperty("type", "sweep");
		html.setProperty("level", level.toString());
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();

		context = new VelocityContext();
		Collections.sort(students, Student.getNameComparator());
		context.put("students", students);
		context.put("subjects", Subject.getSubjects());
		sw = new StringWriter();
		t = ve.getTemplate("studentsOverview.html");
		t.merge(context, sw);
		html = new Entity("html", "students_" + level);
		html.setProperty("type", "students");
		html.setProperty("level", level.toString());
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();

		HashMap<Test, List<Integer>> scores = new HashMap<Test, List<Integer>>();
		Test[] tests = level == Level.MIDDLE ? Test.middleTests() : Test.highTests();
		for (Test test : tests) {
			scores.put(test, new ArrayList<Integer>());
		}

		for (School school : schools.values()) {
			for (Student student : school.getStudents()) {
				for (Entry<Subject, Score> scoreEntry : student.getScores().entrySet()) {
					scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
				}
			}

			for (Entry<Test, ArrayList<Score>> anonScoreEntry : school.getAnonScores().entrySet()) {
				for (Score score : anonScoreEntry.getValue()) {
					scores.get(anonScoreEntry.getKey()).add(score.getScoreNum());
				}
			}
		}

		for (Entry<Test, List<Score>> scoreEntry : anonScores.entrySet()) {
			for (Score score : scoreEntry.getValue()) {
				scores.get(scoreEntry.getKey()).add(score.getScoreNum());
			}
		}

		HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
		HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
		for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
			Pair<List<Integer>, List<Integer>> stats = calculateStats(scoreEntry.getValue());
			summaryStats.put(scoreEntry.getKey(), stats.x);
			outliers.put(scoreEntry.getKey(), stats.y);
		}

		context = new VelocityContext();
		context.put("summaryStats", summaryStats);
		context.put("outliers", outliers);
		context.put("tests", tests);
		context.put("subjects", Subject.getSubjects());
		context.put("level", level);
		sw = new StringWriter();
		t = ve.getTemplate("visualizations.html");
		t.merge(context, sw);
		html = new Entity("html", "visualizations_" + level);
		html.setProperty("type", "visualizations");
		html.setProperty("level", level.toString());
		html.setProperty("html", new Text(compressor.compress(sw.toString())));
		htmlEntries.add(html);
		sw.close();

		datastore.put(htmlEntries); // TODO: Convert to Transaction
	}

	private static Pair<List<Integer>, List<Integer>> calculateStats(List<Integer> list) {
		double[] data = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			data[i] = list.get(i);
		}
		DescriptiveStatistics dStats = new DescriptiveStatistics(data);

		List<Integer> summary = new ArrayList<Integer>(5);
		summary.add((int) dStats.getMin()); // Minimum
		summary.add((int) dStats.getPercentile(25)); // Lower Quartile (Q1)
		summary.add((int) dStats.getPercentile(50)); // Middle Quartile (Median - Q2)
		summary.add((int) dStats.getPercentile(75)); // High Quartile (Q3)
		summary.add((int) dStats.getMax()); // Maxiumum

		List<Integer> outliers = new ArrayList<Integer>();
		if (list.size() > 5 && dStats.getStandardDeviation() > 0) // Only remove outliers if relatively normal
		{
			double mean = dStats.getMean();
			double stDev = dStats.getStandardDeviation();
			NormalDistribution normalDistribution = new NormalDistribution(mean, stDev);

			Iterator<Integer> listIterator = list.iterator();
			double significanceLevel = .50 / list.size(); // Chauvenet's Criterion for Outliers
			while (listIterator.hasNext()) {
				int num = listIterator.next();
				double pValue = normalDistribution.cumulativeProbability(num);
				if (pValue < significanceLevel) {
					outliers.add(num);
					listIterator.remove();
				}
			}

			if (list.size() != dStats.getN()) // If and only if outliers have been removed
			{
				double[] significantData = new double[list.size()];
				for (int i = 0; i < list.size(); i++) {
					significantData[i] = list.get(i);
				}
				dStats = new DescriptiveStatistics(significantData);
				summary.set(0, (int) dStats.getMin());
				summary.set(4, (int) dStats.getMax());
			}
		}

		return new Pair<List<Integer>, List<Integer>>(summary, outliers);
	}

	@SuppressWarnings("deprecation")
	private static void updateRegistrations(Level level, Map<String, School> schools) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		for (School school : schools.values()) {
			Query query = new Query("registration")
				.addFilter("schoolName", FilterOperator.EQUAL, school.getName())
				.addFilter("schoolLevel", FilterOperator.EQUAL, level.toString())
				.addFilter("registrationType", FilterOperator.EQUAL, "coach");
			List<Entity> registrations = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

			if (registrations.size() > 0) {
				school.calculateTestNums();
				Entity registration = registrations.get(0);
				for (Entry<Test, Integer> numTest : school.getNumTests().entrySet()) {
					registration.setProperty(numTest.getKey().toString(), numTest.getValue());
				}
				datastore.put(registration);
			}
		}
	}

	private static void updateContestInfo(Set<Test> testsGraded) {
		Query query = new Query("contestInfo");
		Entity info = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
		SimpleDateFormat isoFormat = new SimpleDateFormat("hh:mm:ss a EEEE MMMM d, yyyy zzzz");
		isoFormat.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
		info.setProperty("updated", isoFormat.format(new Date()).toString());
		info.setProperty("testsGraded", Arrays.asList(testsGraded.toArray()).toString());
		datastore.put(info);
	}
}
