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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import util.PMF;
import util.Pair;
import util.Retrieve;
import util.Statistics;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;
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

		final Entity contestInfo;
		final Map<String, Integer> awardCriteria;

		final Set<Student> middleStudents = new HashSet<Student>();
		final Map<String, School> middleSchools = new HashMap<String, School>();
		final Map<Test, List<Student>> middleCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Subject, List<School>> middleCategorySweepstakesWinners = new HashMap<Subject, List<School>>();
		final List<School> middleSweepstakesWinners = new ArrayList<School>();

		final Set<Student> highStudents = new HashSet<Student>();
		final Map<String, School> highSchools = new HashMap<String, School>();
		final Map<Test, List<Student>> highCategoryWinners = new HashMap<Test, List<Student>>();
		final Map<Subject, List<School>> highCategorySweepstakesWinners = new HashMap<Subject, List<School>>();
		final List<School> highSweepstakesWinners = new ArrayList<School>();

		try {
			// Retrieve contest information from Datastore
			contestInfo = Retrieve.contestInfo();

			// Authenticate to Google Documents Service using OAuth 2.0 Authentication Token from Datastore
			Map<String, String[]> params = req.getParameterMap();
			SpreadsheetService service = new SpreadsheetService("contestTabulation");
			authService(service, contestInfo);

			// Populate base data structures by traversing Google Documents Spreadsheets
			middle = getSpreadSheet(params.get("docMiddle")[0], service);
			high = getSpreadSheet(params.get("docHigh")[0], service);
			updateDatabase(Level.MIDDLE, middle, middleStudents, middleSchools, testsGraded, service);
			updateDatabase(Level.HIGH, high, highStudents, highSchools, testsGraded, service);

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

			// Persist JDOs in Datastore
			persistData(Level.MIDDLE, middleSchools.values(), middleCategoryWinners, middleCategorySweepstakesWinners, middleSweepstakesWinners);
			persistData(Level.HIGH, highSchools.values(), highCategoryWinners, highCategorySweepstakesWinners, highSweepstakesWinners);

			// Get award criteria from Datastore
			awardCriteria = Retrieve.awardCriteria(contestInfo);

			// Generate and store HTML in Datastore
			storeHTML(Level.MIDDLE, middleStudents, middleSchools, middleCategoryWinners, middleCategorySweepstakesWinners, middleSweepstakesWinners, awardCriteria);
			storeHTML(Level.HIGH, highStudents, highSchools, highCategoryWinners, highCategorySweepstakesWinners, highSweepstakesWinners, awardCriteria);

			// Update Datastore by modifying registrations to include actual number of tests taken
			updateRegistrations(Level.MIDDLE, middleSchools);
			updateRegistrations(Level.HIGH, highSchools);

			// Update Datastore by modifying contest information entity to include tests graded
			updateContestInfo(testsGraded, contestInfo);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void authService(SpreadsheetService service, Entity contestInfo) throws IOException {
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

	private static void updateDatabase(Level level, SpreadsheetEntry spreadsheet, Set<Student> students, Map<String, School> schools, Set<Test> testsGraded, Service service) throws IOException, ServiceException {
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

					for (Subject subject : Subject.values()) {
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

	private static void tabulateCategoryWinners(Level level, Set<Student> students, Map<Test, List<Student>> categoryWinners, Set<Test> testsGraded) {
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

	private static void tabulateCategorySweepstakesWinners(Map<String, School> schools, Map<Subject, List<School>> categorySweepstakesWinners) {
		for (final Subject subject : Subject.values()) {
			ArrayList<School> schoolList = new ArrayList<School>(schools.values());
			Collections.sort(schoolList, School.getScoreComparator(subject));
			Collections.reverse(schoolList);
			categorySweepstakesWinners.put(subject, schoolList);
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

	private static void persistData(Level level, Collection<School> schools, Map<Test, List<Student>> categoryWinners, Map<Subject, List<School>> categorySweepstakesWinners, List<School> sweepstakesWinners) {
		PersistenceManager pm = PMF.get().getPersistenceManager();

		try {
			pm.makePersistent(schools.toArray()[0]);
			pm.makePersistentAll(schools);
		}
		finally {
			pm.close();
		}

		List<Entity> categoryWinnersEntities = new ArrayList<Entity>();
		for (Entry<Test, List<Student>> categoryWinnerEntry : categoryWinners.entrySet()) {
			String entityKey = categoryWinnerEntry.getKey().toString() + "_" + level.toString();
			Entity categoryWinnersEntity = new Entity("CategoryWinners", entityKey);

			List<Key> studentKeys = new ArrayList<Key>();
			for (Student student : categoryWinnerEntry.getValue()) {
				studentKeys.add(student.getKey());
			}
			categoryWinnersEntity.setProperty("students", studentKeys);

			categoryWinnersEntities.add(categoryWinnersEntity);
		}
		datastore.put(categoryWinnersEntities);

		List<Entity> categorySweepstakesWinnersEntities = new ArrayList<Entity>();
		for (Entry<Subject, List<School>> categorySweepstakesWinnerEntry : categorySweepstakesWinners.entrySet()) {
			String entityKey = categorySweepstakesWinnerEntry.getKey().toString() + "_" + level.toString();
			Entity categoryWinnersEntity = new Entity("CategorySweepstakesWinners", entityKey);

			List<Key> schoolKeys = new ArrayList<Key>();
			for (School school : categorySweepstakesWinnerEntry.getValue()) {
				schoolKeys.add(school.getKey());
			}
			categoryWinnersEntity.setProperty("schools", schoolKeys);

			categorySweepstakesWinnersEntities.add(categoryWinnersEntity);
		}
		datastore.put(categorySweepstakesWinnersEntities);

		List<Entity> visualizationEntities = new ArrayList<Entity>();

		Entity sweepstakesWinnerEntity = new Entity("SweepstakesWinners", level.toString());
		List<Key> schoolKeys = new ArrayList<Key>();
		for (School school : sweepstakesWinners) {
			schoolKeys.add(school.getKey());
		}
		sweepstakesWinnerEntity.setProperty("schools", schoolKeys);
		datastore.put(sweepstakesWinnerEntity);

		HashMap<Test, List<Integer>> scores = new HashMap<Test, List<Integer>>();
		Test[] tests = Test.getTests(level);
		for (Test test : tests) {
			scores.put(test, new ArrayList<Integer>());
		}

		for (School school : schools) {
			for (Student student : school.getStudents()) {
				for (Entry<Subject, Score> scoreEntry : student.getScores().entrySet()) {
					scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
				}
			}
		}

		HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
		HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
		for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
			Pair<List<Integer>, List<Integer>> stats = Statistics.calculateStats(scoreEntry.getValue());
			summaryStats.put(scoreEntry.getKey(), stats.x);
			outliers.put(scoreEntry.getKey(), stats.y);
		}

		for (Test test : tests) {
			Entity visualizationsEntity = new Entity("Visualization", test.toString());
			visualizationsEntity.setProperty("summaryStats", summaryStats.get(test));
			visualizationsEntity.setProperty("outliers", outliers.get(test));
			visualizationEntities.add(visualizationsEntity);
		}
		datastore.put(visualizationEntities);
	}

	private static void storeHTML(Level level, Set<Student> students, Map<String, School> schools, Map<Test, List<Student>> categoryWinners, Map<Subject, List<School>> categorySweepstakesWinners, List<School> sweepstakesWinners, Map<String, Integer> awardCriteria) throws IOException {
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

				HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
				HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
				for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
					Pair<List<Integer>, List<Integer>> stats = Statistics.calculateStats(scoreEntry.getValue());
					summaryStats.put(scoreEntry.getKey(), stats.x);
					outliers.put(scoreEntry.getKey(), stats.y);
				}

				context.put("summaryStats", summaryStats);
				context.put("outliers", outliers);
				context.put("tests", tests);
				context.put("subjects", Subject.values());
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
		context.put("winners", categorySweepstakesWinners);
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
		context.put("subjects", Subject.values());
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
		List<Student> studentsList = new ArrayList<Student>(students);
		Collections.sort(studentsList, Student.getNameComparator());
		context.put("students", studentsList);
		context.put("subjects", Subject.values());
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
		}

		HashMap<Test, List<Integer>> summaryStats = new HashMap<Test, List<Integer>>();
		HashMap<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
		for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
			Pair<List<Integer>, List<Integer>> stats = Statistics.calculateStats(scoreEntry.getValue());
			summaryStats.put(scoreEntry.getKey(), stats.x);
			outliers.put(scoreEntry.getKey(), stats.y);
		}

		context = new VelocityContext();
		context.put("summaryStats", summaryStats);
		context.put("outliers", outliers);
		context.put("tests", tests);
		context.put("subjects", Subject.values());
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

	@SuppressWarnings("deprecation")
	private static void updateRegistrations(Level level, Map<String, School> schools) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		for (School school : schools.values()) {
			Query query = new Query("registration").addFilter("schoolName", FilterOperator.EQUAL, school.getName())
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

	private static void updateContestInfo(Set<Test> testsGraded, Entity contestInfo) {
		SimpleDateFormat isoFormat = new SimpleDateFormat("hh:mm:ss a EEEE MMMM d, yyyy zzzz");
		isoFormat.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
		contestInfo.setProperty("updated", isoFormat.format(new Date()).toString());

		List<String> testsGradedList = new ArrayList<String>();
		for (Test test : testsGraded) {
			testsGradedList.add(test.toString());
		}
		contestInfo.setProperty("testsGraded", testsGradedList);

		datastore.put(contestInfo);
	}
}
