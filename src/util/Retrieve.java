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

package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.jdo.PersistenceManager;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import contestTabulation.Level;
import contestTabulation.School;
import contestTabulation.Score;
import contestTabulation.Student;
import contestTabulation.Subject;
import contestTabulation.Test;

public class Retrieve {
	private static final PersistenceManager pm = PMF.get().getPersistenceManager();
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	public static List<Student> allStudents(Level level) {
		javax.jdo.Query q = pm.newQuery(Student.class);
		q.setFilter("grade >= :lowGrade && grade <= :highGrade");
		List<Student> students = (List<Student>) q.execute(level.getLowGrade(), level.getHighGrade());
		Collections.sort(students, Student.getNameComparator());
		return students;
	}

	public static Pair<School, Pair<Map<Test, Map<String, Double>>, Map<Test, List<Integer>>>> schoolOverview(String schoolName) {
		javax.jdo.Query q = pm.newQuery(School.class);
		q.setFilter("name == :schoolName");
		List<School> schools = (List<School>) q.execute(schoolName);
		if (!schools.isEmpty()) {
			School school = schools.get(0);

			HashMap<Test, List<Integer>> scores = new HashMap<Test, List<Integer>>();
			for (Test test : Test.getTests(school.getLevel())) {
				scores.put(test, new ArrayList<Integer>());
			}

			for (Student student : school.getStudents()) {
				for (Entry<Subject, Score> scoreEntry : student.getScores().entrySet()) {
					scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
				}
			}

			Map<Test, Map<String, Double>> summaryStats = new HashMap<Test, Map<String, Double>>();
			Map<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();
			for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
				Pair<Map<String, Double>, List<Integer>> stats = Statistics.calculateStats(scoreEntry.getValue());
				summaryStats.put(scoreEntry.getKey(), stats.x);
				outliers.put(scoreEntry.getKey(), stats.y);
			}

			return new Pair<School, Pair<Map<Test, Map<String, Double>>, Map<Test, List<Integer>>>>(school,
					new Pair<Map<Test, Map<String, Double>>, Map<Test, List<Integer>>>(summaryStats, outliers));
		}
		return null;
	}

	public static List<Student> categoryWinners(String category, Level level) {
		try {
			Entity categoryWinnersEntity = datastore.get(KeyFactory.createKey("CategoryWinners", category + "_" + level.toString()));
			List<Key> categoryWinnersKeys = (List<Key>) categoryWinnersEntity.getProperty("students");
			javax.jdo.Query q = pm.newQuery("select from " + Student.class.getName() + " where :keys.contains(key)");
			return (List<Student>) q.execute(categoryWinnersKeys);
		}
		catch (EntityNotFoundException e) {
			return null;
		}
	}

	public static Map<Subject, List<School>> categorySweepstakesWinners(Level level) {
		List<Key> categorySweepstakesWinnersEntityKeys = new ArrayList<Key>();
		for (Subject subject : Subject.values()) {
			categorySweepstakesWinnersEntityKeys.add(KeyFactory.createKey("CategorySweepstakesWinners", subject + "_" + level.toString()));
		}

		Map<Key, Entity> categorySweepstakesWinnersEntityMap = datastore.get(categorySweepstakesWinnersEntityKeys);

		Map<Subject, List<School>> categorySweepstakesWinners = new HashMap<Subject, List<School>>();
		for (Entry<Key, Entity> categorySweepstakesWinnersEntityEntry : categorySweepstakesWinnersEntityMap.entrySet()) {
			Subject category = Subject.valueOf(categorySweepstakesWinnersEntityEntry.getKey().getName().split("_")[0]);
			List<Key> categorySweepstakesWinnersKeys = (List<Key>) categorySweepstakesWinnersEntityEntry.getValue().getProperty("schools");
			javax.jdo.Query q = pm.newQuery("select from " + School.class.getName() + " where :keys.contains(key)");
			categorySweepstakesWinners.put(category, (List<School>) q.execute(categorySweepstakesWinnersKeys));
		}

		return categorySweepstakesWinners;
	}

	public static List<School> sweepstakesWinners(Level level) {
		javax.jdo.Query q = pm.newQuery(School.class);
		q.setOrdering("totalScore desc");
		q.setFilter("level == :schoolLevel");
		return (List<School>) q.execute(level);
	}

	public static Pair<Map<Test, Map<String, Double>>, Map<Test, List<Integer>>> visualizations(Level level) throws JSONException {
		List<Key> visualizationKeys = new ArrayList<Key>();
		for (Test test : Test.getTests(level)) {
			visualizationKeys.add(KeyFactory.createKey("Visualization", test.toString()));
		}
		Map<Key, Entity> visualizationEntities = datastore.get(visualizationKeys);

		Map<Test, Map<String, Double>> summaryStats = new HashMap<Test, Map<String, Double>>();
		Map<Test, List<Integer>> outliers = new HashMap<Test, List<Integer>>();

		for (Entry<Key, Entity> visualizationEntry : visualizationEntities.entrySet()) {
			Test test = Test.fromString(visualizationEntry.getKey().getName());
			outliers.put(test, (List<Integer>) visualizationEntry.getValue().getProperty("outliers"));

			Map<String, Double> testSummaryStats = new HashMap<String, Double>();
			JSONObject summaryStatsJSON = new JSONObject((String) visualizationEntry.getValue().getProperty("summaryStats"));
			Iterator<String> summaryStatsIterator = summaryStatsJSON.keys();
			while (summaryStatsIterator.hasNext()) {
				String summaryStatsKey = summaryStatsIterator.next();
				testSummaryStats.put(summaryStatsKey, summaryStatsJSON.getDouble(summaryStatsKey));
			}
			summaryStats.put(test, testSummaryStats);
		}

		return new Pair<Map<Test, Map<String, Double>>, Map<Test, List<Integer>>>(summaryStats, outliers);
	}

	public static Map<String, Integer> awardCriteria(Entity contestInfo) {
		return textToMap(contestInfo, "awardCriteria");
	}

	public static Map<String, Integer> qualifyingCriteria(Entity contestInfo) {
		return textToMap(contestInfo, "qualifyingCriteria");
	}

	public static Entity contestInfo() {
		Query query = new Query("contestInfo");
		List<Entity> contestInfos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		return !contestInfos.isEmpty() ? contestInfos.get(0) : null;
	}

	private static Map<String, Integer> textToMap(Entity contestInfo, String propertyName) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		JSONObject mapJSON = null;
		try {
			Object mapText = Objects.requireNonNull(contestInfo).getProperty(propertyName);
			if (mapText != null) {
				mapJSON = new JSONObject(((Text) mapText).getValue());
				Iterator<String> keyIter = mapJSON.keys();
				while (keyIter.hasNext()) {
					String key = keyIter.next();
					map.put(key, (Integer) mapJSON.get(key));
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return map;
	}
}
