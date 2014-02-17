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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

import util.Pair;

public class School {
	public static Comparator<School> getScoreComparator(final Subject subject) {
		return new Comparator<School>() {
			@Override
			public int compare(School s1, School s2) {
				return s1.getScore(subject) - s2.getScore(subject);
			}
		};
	}

	public static Comparator<School> getTotalScoreComparator() {
		return new Comparator<School>() {
			@Override
			public int compare(School s1, School s2) {
				return s1.getTotalScore() - s2.getTotalScore();
			}
		};
	}

	private final HashMap<Test, ArrayList<Score>> anonScores = new HashMap<Test, ArrayList<Score>>();
	private final Level level;
	private final int lowGrade, highGrade;
	private final String name;
	private final HashMap<Test, Integer> numTests = new HashMap<Test, Integer>();
	private final ArrayList<Student> students = new ArrayList<Student>();
	private final HashMap<Subject, Pair<Student[], Integer>> topScores = new HashMap<Subject, Pair<Student[], Integer>>();

	private int totalScore;

	School(String name, Level level) {
		this.name = Objects.requireNonNull(name);
		this.level = Objects.requireNonNull(level);
		this.lowGrade = level.getLowGrade();
		this.highGrade = level.getHighGrade();
	}

	protected void addAnonScores(Test test, ArrayList<Score> scores) {
		anonScores.put(test, scores);
		if (!numTests.containsKey(test)) {
			numTests.put(test, scores.size());
		}
		else {
			numTests.put(test, numTests.get(test) + scores.size());
		}
	}

	protected void addStudent(Student student) {
		students.add(Objects.requireNonNull(student));
	}

	private HashMap<Student, Score> calculateScore(final Subject subject) {
		ArrayList<Student> subjectStudents = new ArrayList<Student>();

		for (Student student : students) {
			if (student.hasScore(subject) && student.getScore(subject).getScoreNum() >= 0) {
				subjectStudents.add(student);
			}
		}

		for (int grade = lowGrade; grade <= highGrade; grade++) {
			ArrayList<Score> scores = anonScores.get(Test.valueOf(subject + Integer.toString(grade)));
			if (scores != null) {
				for (Score score : scores) {
					if (score.getScoreNum() > 0) {
						Student tempStudent = new Student(grade, this);
						tempStudent.setScore(subject, score);
						subjectStudents.add(tempStudent);
					}
				}
			}
		}

		Collections.sort(subjectStudents, Collections.reverseOrder(new Comparator<Student>() {
			@Override
			public int compare(Student s1, Student s2) {
				return s1.getScore(subject).compareTo(s2.getScore(subject));
			}
		}));

		int inHighGrade = 0;
		HashMap<Student, Score> top4 = new HashMap<Student, Score>();
		for (Student student : subjectStudents) {
			if (top4.size() < 4) {
				Score score = student.getScore(subject);
				if (highGrade == student.getGrade() && inHighGrade < 3) {
					top4.put(student, score);
					inHighGrade++;
				}
				else if (highGrade != student.getGrade()) {
					top4.put(student, score);
				}
			}
		}

		int totalScore = 0;
		for (Score score : top4.values()) {
			if (score != null) {
				totalScore += score.getScoreNum();
			}
		}
		topScores.put(subject, new Pair<Student[], Integer>(top4.keySet().toArray(new Student[top4.keySet().size()]), totalScore));

		return top4;
	}

	public void calculateScores() {
		for (Subject subject : Subject.getSubjects()) {
			calculateScore(subject);
		}
		if (level == Level.MIDDLE) {
			totalScore = topScores.get(Subject.N).y + topScores.get(Subject.C).y + (int) Math.round(topScores.get(Subject.M).y * 8.0 / 5.0 + topScores.get(Subject.S).y * 8.0 / 5.0);
		}
		else {
			totalScore = topScores.get(Subject.N).y + (int) Math.round(topScores.get(Subject.M).y * 10.0 / 9.0 + topScores.get(Subject.S).y * 10.0 / 9.0 + topScores.get(Subject.C).y * 8.0 / 7.0);
		}
	}

	public void calculateTestNums() {
		for (Student student : students) {
			int grade = student.getGrade();
			for (Subject s : student.getScores().keySet()) {
				Test test = Test.fromSubjectAndGrade(grade, s);
				if (!numTests.containsKey(test)) {
					numTests.put(test, 1);
				}
				else {
					numTests.put(test, numTests.get(test) + 1);
				}
			}

		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		School other = (School) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
			else if (!name.equals(other.name)) {
				return false;
			}
		}
		return true;
	}

	public HashMap<Test, ArrayList<Score>> getAnonScores() {
		return anonScores;
	}

	public ArrayList<Score> getAnonScores(Test test) {
		return anonScores.get(Objects.requireNonNull(test));
	}

	public Level getLevel() {
		return level;
	}

	public String getName() {
		return name;
	}

	public int getNumStudents() {
		return students.size();
	}

	public HashMap<Test, Integer> getNumTests() {
		return numTests;
	}

	public int getScore(Subject subject) {
		return topScores.get(Objects.requireNonNull(subject)).y;
	}

	public Student[] getScoreStudents(Subject subject) {
		return topScores.get(Objects.requireNonNull(subject)).x;
	}

	public ArrayList<Student> getStudents() {
		return students;
	}

	public int getTotalScore() {
		return totalScore;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "School [name=" + name + ", level=" + level + ", totalScore=" + totalScore + "]";
	}
}
