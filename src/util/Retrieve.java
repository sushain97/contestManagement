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

package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.jdo.JDOFatalUserException;
import javax.jdo.PersistenceManager;

import org.yaml.snakeyaml.Yaml;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
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

	public static Pair<School, List<Student>> schoolStudents(String schoolName, Level level) {
		javax.jdo.Query q = pm.newQuery(School.class);
		q.setFilter("name == :schoolName && level == :schoolLevel");
		List<School> schools = (List<School>) q.execute(schoolName, level);

		if (!schools.isEmpty()) {
			School school = schools.get(0);
			List<Student> students = new ArrayList<Student>();
			students.addAll(school.getStudents());
			Collections.sort(students, Student.getNameComparator());
			return new Pair<School, List<Student>>(school, students);
		}
		return null;
	}

	public static List<String> schoolNames(Level level) {
		javax.jdo.Query q = pm.newQuery("select name from " + School.class.getName());
		q.setFilter("level == :schoolLevel");
		return (List<String>) q.execute(level);
	}

	public static Pair<School, Map<Test, Statistics>> schoolOverview(String schoolName, Level level) {
		String schoolGroupsNamesString = ((Text) Retrieve.contestInfo().getProperty(level.toString() + "SchoolGroupsNames")).getValue();
		if (schoolGroupsNamesString != null) {
			Map<String, String> schoolGroupsNames = (Map<String, String>) new Yaml().load(schoolGroupsNamesString);
			if (schoolGroupsNames.containsKey(schoolName)) {
				schoolName = schoolGroupsNames.get(schoolName);
			}
		}

		javax.jdo.Query q = pm.newQuery(School.class);
		q.setFilter("name == :schoolName && level == :schoolLevel");
		List<School> schools = (List<School>) q.execute(schoolName, level);

		if (!schools.isEmpty()) {
			School school = schools.get(0);

			HashMap<Test, List<Integer>> scores = new HashMap<Test, List<Integer>>();
			for (Test test : Test.getTests(school.getLevel())) {
				scores.put(test, new ArrayList<Integer>());
			}

			for (Student student : school.getStudents()) {
				for (Entry<Subject, Score> scoreEntry : student.getScores().entrySet()) {
					if (scoreEntry.getValue().isNumeric()) {
						scores.get(Test.fromSubjectAndGrade(student.getGrade(), scoreEntry.getKey())).add(scoreEntry.getValue().getScoreNum());
					}
				}
			}

			Map<Test, Statistics> statistics = new HashMap<Test, Statistics>();
			for (Entry<Test, List<Integer>> scoreEntry : scores.entrySet()) {
				statistics.put(scoreEntry.getKey(), new Statistics(scoreEntry.getValue()));
			}

			return new Pair<School, Map<Test, Statistics>>(school, statistics);
		}
		return null;
	}

	public static List<Student> categoryWinners(String category, Level level) {
		try {
			Key key = KeyFactory.createKey(KeyFactory.createKey("Level", level.getName()), "CategoryWinners", category + "_" + level.toString());
			Entity categoryWinnersEntity = datastore.get(key);
			List<Key> categoryWinnersKeys = (List<Key>) categoryWinnersEntity.getProperty("students");
			javax.jdo.Query q = pm.newQuery("select from " + Student.class.getName() + " where :keys.contains(key)");
			List<Student> students = (List<Student>) q.execute(categoryWinnersKeys);
			students.isEmpty();
			return students;
		}
		catch (EntityNotFoundException | JDOFatalUserException e) {
			return null;
		}
	}

	public static Map<Subject, List<School>> categorySweepstakesWinners(Level level) {
		List<Key> categorySweepstakesWinnersEntityKeys = new ArrayList<Key>();
		for (Subject subject : Subject.values()) {
			Key key = KeyFactory.createKey(KeyFactory.createKey("Level", level.getName()), "CategorySweepstakesWinners", subject + "_" + level.toString());
			categorySweepstakesWinnersEntityKeys.add(key);
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

	public static Map<Test, Statistics> visualizations(Level level) throws JSONException {
		List<Key> visualizationKeys = new ArrayList<Key>();
		for (Test test : Test.getTests(level)) {
			Key key = KeyFactory.createKey(KeyFactory.createKey("Level", level.getName()), "Visualization", test.toString());
			visualizationKeys.add(key);
		}
		Map<Key, Entity> visualizationEntities = datastore.get(visualizationKeys);

		Map<Test, Statistics> statistics = new HashMap<Test, Statistics>();

		for (Entry<Key, Entity> visualizationEntry : visualizationEntities.entrySet()) {
			Test test = Test.fromString(visualizationEntry.getKey().getName());
			List<Long> scores = (List<Long>) visualizationEntry.getValue().getProperty("scores");
			if (scores != null) {
				List<Integer> scoreInts = new ArrayList<Integer>();
				for (Long score : scores) {
					scoreInts.add(score.intValue());
				}
				statistics.put(test, new Statistics(scoreInts));
			}
		}

		return statistics;
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

	public static int numUnresolvedQuestions() {
		Query query = new Query("feedback").setFilter(new FilterPredicate("resolved", FilterOperator.NOT_EQUAL, true)).setKeysOnly();
		return datastore.prepare(query).asList(FetchOptions.Builder.withDefaults()).size();
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
