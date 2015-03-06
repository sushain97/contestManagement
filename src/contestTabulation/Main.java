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

package contestTabulation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.yaml.snakeyaml.Yaml;

import util.PMF;
import util.Pair;
import util.Retrieve;

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
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
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
import com.google.gdata.util.ServiceException;

@SuppressWarnings("serial")
public class Main extends HttpServlet {
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	private static final HttpTransport httpTransport = new NetHttpTransport();
	private static final JacksonFactory jsonFactory = new JacksonFactory();
	private static final Logger logger = Logger.getLogger(Main.class.getName());
	private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("h:mm:ss a");
	private static final MemcacheService memCache = MemcacheServiceFactory.getMemcacheService();

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		final long startTimeMillis = System.currentTimeMillis();
		final Map<Test, Pair<Integer, Integer>> testsGraded = new HashMap<Test, Pair<Integer, Integer>>();
		final List<Test> tiesBroken = new ArrayList<Test>();

		final Entity contestInfo;
		final Map<String, Integer> awardCriteria;

		final Map<Level, Set<Student>> students = new HashMap<Level, Set<Student>>();
		final Map<Level, Map<String, School>> schools = new HashMap<Level, Map<String, School>>();
		final Map<Level, Map<Test, List<Student>>> categoryWinners = new HashMap<Level, Map<Test, List<Student>>>();
		final Map<Level, Map<Subject, List<School>>> categorySweepstakesWinners = new HashMap<Level, Map<Subject, List<School>>>();
		final Map<Level, List<School>> sweepstakesWinners = new HashMap<Level, List<School>>();

		final StringBuilder errorLog = new StringBuilder();

		memCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(java.util.logging.Level.INFO));
		memCache.put("tabulationTaskStatus", ("Running/Init_" + System.currentTimeMillis()).getBytes());

		try {
			// Retrieve contest information from Datastore
			contestInfo = Retrieve.contestInfo();

			// Get award criteria from Datastore
			awardCriteria = Retrieve.awardCriteria(contestInfo);

			// Initialize data structures
			for (Level level : Level.values()) {
				students.put(level, new HashSet<Student>());
				schools.put(level, new HashMap<String, School>());
				categoryWinners.put(level, new HashMap<Test, List<Student>>());
				categorySweepstakesWinners.put(level, new HashMap<Subject, List<School>>());
				sweepstakesWinners.put(level, new ArrayList<School>());
			}

			// Authenticate to Google Documents Service using OAuth 2.0 Authentication Token from Datastore
			Map<String, String[]> params = req.getParameterMap();
			SpreadsheetService service = new SpreadsheetService("contestTabulation");
			authService(service, contestInfo);

			// Retrieve enabled levels from Datastore
			String[] stringLevels = ((String) contestInfo.getProperty("levels")).split(Pattern.quote("+"));
			Level[] levels = new Level[stringLevels.length];
			for (int i = 0; i < stringLevels.length; i++) {
				levels[i] = Level.fromString(stringLevels[i]);
			}

			for (Level level : levels) {
				memCache.put("tabulationTaskStatus", ("Running/" + level.getName() + "_" + System.currentTimeMillis()).getBytes());
				Map<String, School> lSchools = schools.get(level);
				List<School> lsweepstakesWinners = sweepstakesWinners.get(level);
				Map<Test, List<Student>> lCategoryWinners = categoryWinners.get(level);
				Map<Subject, List<School>> lCategorySweepstakesWinners = categorySweepstakesWinners.get(level);

				// Populate base data structures by traversing Google Documents Spreadsheets
				SpreadsheetEntry spreadsheet = getSpreadSheet(params.get("doc" + level.getName())[0], service);
				Map<String, String> schoolGroups = getSchoolGroups(level, contestInfo);
				updateDatabase(level, spreadsheet, students.get(level), lSchools, schoolGroups, testsGraded, service, errorLog);

				// Populate category winners lists with top scorers (as defined by award criteria)
				tabulateCategoryWinners(level, students.get(level), lCategoryWinners, testsGraded, tiesBroken, awardCriteria, errorLog);

				// Calculate school sweepstakes scores and number of tests fields
				for (School school : lSchools.values()) {
					school.calculateScores();
					school.calculateTestNums();
				}

				// Populate category sweepstakes winners maps and sweepstakes winners lists with top scorers
				tabulateCategorySweepstakesWinners(lSchools, lCategorySweepstakesWinners);
				tabulateSweepstakesWinners(lSchools, lsweepstakesWinners);

				// Persist JDOs in Datastore
				persistData(level, lSchools.values(), lCategoryWinners, lCategorySweepstakesWinners, lsweepstakesWinners);

				// Update Datastore by modifying registrations to include actual number of tests taken
				updateRegistrations(level, lSchools);
			}

			// Update Datastore by modifying contest information entity to include tests graded, last updated timestamp and error logs
			updateContestInfo(testsGraded, tiesBroken, contestInfo, errorLog);

			long elapsedSeconds = TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTimeMillis, TimeUnit.MILLISECONDS);
			memCache.put("tabulationTaskStatus", ("Success/" + elapsedSeconds + " second" + (elapsedSeconds == 1 ? "" : "s") + "_" + System.currentTimeMillis()).getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			memCache.put("tabulationTaskStatus", ("Failed/" + e.getClass().getName() + "_" + System.currentTimeMillis()).getBytes());
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

	private static SpreadsheetEntry getSpreadSheet(String docString, Service service) throws ServiceException, MalformedURLException, IOException {
		SpreadsheetFeed feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full"), SpreadsheetFeed.class);
		List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		for (SpreadsheetEntry spreadsheet : spreadsheets) {
			if (spreadsheet.getTitle().getPlainText().equals(docString)) {
				return spreadsheet;
			}
		}
		return null;
	}

	private static Map<String, String> getSchoolGroups(Level level, Entity contestInfo) {
		String schoolGroupsNamesString = ((Text) contestInfo.getProperty(level.toString() + "SchoolGroupsNames")).getValue();
		if (schoolGroupsNamesString != null) {
			return (Map<String, String>) new Yaml().load(schoolGroupsNamesString);
		}
		else {
			return new HashMap<String, String>();
		}
	}

	private static void updateDatabase(Level level, SpreadsheetEntry spreadsheet, Set<Student> students, Map<String, School> schools, Map<String, String> schoolGroups, Map<Test, Pair<Integer, Integer>> testsGraded, Service service, StringBuilder errorLog) throws IOException, ServiceException {
		WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
		service.setReadTimeout(60000);
		service.setConnectTimeout(60000);
		List<WorksheetEntry> worksheets = worksheetFeed.getEntries();

		for (WorksheetEntry worksheet : worksheets) {
			String schoolName = worksheet.getTitle().getPlainText();

			School school;
			if (schoolGroups.containsKey(schoolName)) {
				String schoolGroupName = schoolGroups.get(schoolName);
				if (schools.containsKey(schoolGroupName)) {
					school = schools.get(schoolGroupName);
				}
				else {
					school = new School(schoolGroupName, level);
					schools.put(schoolGroupName, school);
				}
			}
			else {
				school = new School(schoolName, level);
				schools.put(schoolName, school);
			}

			URL listFeedUrl = worksheet.getListFeedUrl();
			ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);

			for (ListEntry r : listFeed.getEntries()) {
				try {
					CustomElementCollection row = r.getCustomElements();

					String name = row.getValue("name").trim();
					int grade = Integer.parseInt(row.getValue("grade").trim());

					Student student = new Student(name, school, grade);
					students.add(student);

					List<Subject> registeredSubjects = new ArrayList<Subject>();
					for (Subject subject : Subject.values()) {
						String score = row.getValue(subject.toString());
						if (score != null && Score.isScore(score.trim())) {
							student.setScore(subject, new Score(score));
							Test test = Test.fromSubjectAndGrade(grade, subject);
							if (testsGraded.containsKey(test)) {
								testsGraded.put(test, new Pair<Integer, Integer>(testsGraded.get(test).x + 1, testsGraded.get(test).y + 1));
							}
							else {
								testsGraded.put(test, new Pair<Integer, Integer>(1, 1));
							}
							registeredSubjects.add(subject);
						}
						else if (score == null) {
							registeredSubjects.add(subject);
							Test test = Test.fromSubjectAndGrade(grade, subject);
							if (testsGraded.containsKey(test)) {
								testsGraded.put(test, new Pair<Integer, Integer>(testsGraded.get(test).x, testsGraded.get(test).y + 1));
							}
							else {
								testsGraded.put(test, new Pair<Integer, Integer>(0, 1));
							}
						}
					}

					student.setRegisteredSubjects(registeredSubjects);

					if (!school.addStudent(student)) {
						String error = logDateFormat.format(new Date()) + " - " + "Duplicate student detected: " + student;
						logger.severe(error);
						errorLog.append(error + "\n");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void tabulateCategoryWinners(Level level, Set<Student> students, Map<Test, List<Student>> categoryWinners, Map<Test, Pair<Integer, Integer>> testsGraded, List<Test> tiesBroken, Map<String, Integer> awardCriteria, StringBuilder errorLog) {
		for (Test test : testsGraded.keySet()) {
			int grade = test.getGrade();
			final Subject subject = test.getSubject();

			ArrayList<Student> testStudents = new ArrayList<Student>();
			for (Student student : students) {
				if (student.getGrade() == grade) {
					Score score = student.getScore(subject);
					if (score != null && score.isNumeric() && score.getScoreNum() > 0) {
						testStudents.add(student);
					}
				}
			}
			Collections.sort(testStudents, Student.getScoreComparator(subject));
			Collections.reverse(testStudents);

			ArrayList<Student> winners = new ArrayList<Student>();
			int numWinners = awardCriteria.get("category_" + level + "_medal") + awardCriteria.get("category_" + level + "_trophy");
			int lastScoreNum = Integer.MIN_VALUE;
			for (Student student : testStudents) {
				Score score = student.getScore(subject);
				if (lastScoreNum == score.getScoreNum() || winners.size() < numWinners) {
					winners.add(student);
				}
				else if (winners.size() >= numWinners) {
					winners.add(student);
					break;
				}
				lastScoreNum = score.getScoreNum();
			}

			boolean testTiesBroken = true;

			Map<Integer, List<Student>> studentsByScore = new TreeMap<Integer, List<Student>>();
			for (int i = 0; i < winners.size(); i++) {
				Student student = winners.get(i);
				Score score = student.getScore(subject);
				if (!studentsByScore.containsKey(score.getScoreNum())) {
					List<Student> s = new ArrayList<Student>();
					s.add(student);
					studentsByScore.put(score.getScoreNum(), s);
				}
				else {
					studentsByScore.get(score.getScoreNum()).add(student);
				}
			}

			for (Entry<Integer, List<Student>> entry : studentsByScore.entrySet()) {
				if (entry.getValue().size() > 1 && entry.getKey() != test.getMaxTeamScore()) {
					boolean scoreModsPresent = true;
					for (Student st : entry.getValue()) {
						scoreModsPresent &= st.getScore(subject).getScoreMod() != 0;
					}

					if (!scoreModsPresent) {
						String error = logDateFormat.format(new Date()) + " - " + "Tie of " + entry.getKey() + " detected in " + test.toString() + ": ";
						for (Student st : entry.getValue()) {
							error += st.getName() + " (" + st.getGrade() + ", " + st.getSchool().getName() + ") ";
						}
						logger.severe(error);
						errorLog.append(error + "\n");
						testTiesBroken = false;
					}
				}
			}

			categoryWinners.put(test, winners);

			if (testTiesBroken) {
				tiesBroken.add(test);
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

	private static void persistData(Level level, Collection<School> schools, Map<Test, List<Student>> categoryWinners, Map<Subject, List<School>> categorySweepstakesWinners, List<School> sweepstakesWinners) throws JSONException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Transaction tx = pm.currentTransaction();
		try {
			tx.begin();
			pm.makePersistentAll(schools);
			tx.commit();
		}
		finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}

		List<Entity> categoryWinnersEntities = new ArrayList<Entity>();
		for (Entry<Test, List<Student>> categoryWinnerEntry : categoryWinners.entrySet()) {
			String entityKey = categoryWinnerEntry.getKey().toString() + "_" + level.toString();
			Entity categoryWinnersEntity = new Entity("CategoryWinners", entityKey, KeyFactory.createKey("Level", level.getName()));

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
			Entity categoryWinnersEntity = new Entity("CategorySweepstakesWinners", entityKey, KeyFactory.createKey("Level", level.getName()));

			List<Key> schoolKeys = new ArrayList<Key>();
			for (School school : categorySweepstakesWinnerEntry.getValue()) {
				schoolKeys.add(school.getKey());
			}
			categoryWinnersEntity.setProperty("schools", schoolKeys);

			categorySweepstakesWinnersEntities.add(categoryWinnersEntity);
		}
		datastore.put(categorySweepstakesWinnersEntities);

		List<Entity> visualizationEntities = new ArrayList<Entity>();

		Entity sweepstakesWinnerEntity = new Entity("SweepstakesWinners", level.toString(), KeyFactory.createKey("Level", level.getName()));
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
					if (scoreEntry.getValue().isNumeric()) {
						scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
					}
				}
			}
		}

		for (Test test : tests) {
			Entity visualizationsEntity = new Entity("Visualization", test.toString(), KeyFactory.createKey("Level", level.getName()));
			visualizationsEntity.setProperty("scores", scores.get(test));
			visualizationEntities.add(visualizationsEntity);
		}
		datastore.put(visualizationEntities);
	}

	private static void updateRegistrations(Level level, Map<String, School> schools) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		for (School school : schools.values()) {
			Filter schoolNameFilter = new FilterPredicate("schoolName", FilterOperator.EQUAL, school.getName());
			Filter schoolLevelFilter = new FilterPredicate("schoolLevel", FilterOperator.EQUAL, level.toString());
			Filter regTypeFilter = new FilterPredicate("registrationType", FilterOperator.EQUAL, "coach");

			Query query = new Query("registration").setFilter(CompositeFilterOperator.and(schoolNameFilter, schoolLevelFilter, regTypeFilter));
			List<Entity> registrations = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

			if (registrations.size() > 0) {
				Entity registration = registrations.get(0);
				for (Entry<Test, Integer> numTest : school.getNumTests().entrySet()) {
					registration.setProperty(numTest.getKey().toString(), numTest.getValue());
				}
				datastore.put(registration);
			}
		}
	}

	private static void updateContestInfo(Map<Test, Pair<Integer, Integer>> testsGraded, List<Test> tiesBroken, Entity contestInfo, StringBuilder errorLog) throws JSONException {
		SimpleDateFormat isoFormat = new SimpleDateFormat("hh:mm:ss a EEEE MMMM d, yyyy zzzz");
		isoFormat.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
		contestInfo.setProperty("updated", isoFormat.format(new Date()).toString());

		List<String> testsGradedList = new ArrayList<String>();
		for (Test test : testsGraded.keySet()) {
			if (testsGraded.get(test).x > 0 && tiesBroken.contains(test)) {
				testsGradedList.add(test.toString());
			}
		}
		contestInfo.setProperty("testsGraded", testsGradedList);

		JSONObject testsGradedJSON = new JSONObject();
		for (Entry<Test, Pair<Integer, Integer>> entry : testsGraded.entrySet()) {
			JSONArray numTests = new JSONArray().put(entry.getValue().x).put(entry.getValue().y);
			testsGradedJSON.put(entry.getKey().toString(), numTests);
		}

		contestInfo.setProperty("testsGradedNums", new Text(testsGradedJSON.toString()));

		contestInfo.setProperty("errorLog", new Text(errorLog.toString()));

		datastore.put(contestInfo);
	}
}
