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

import java.util.HashMap;

public class Student {
	final private String name;
	final private School school;
	final private int grade;
	private final HashMap<Subject, Score> scores = new HashMap<Subject, Score>();

	private static int anonCounter = 0;

	Student(String name, School school, int grade) {
		this.name = name;
		this.grade = grade;
		this.school = school;
	}

	Student(int grade, School school) {
		this("Anonymous" + anonCounter, school, grade);
		anonCounter++;
	}

	public void setScore(Subject subject, Score score) {
		scores.put(subject, score);
	}

	public Score getScore(Subject subject) {
		return scores.get(subject);
	}

	public boolean hasScore(Subject subject) {
		return scores.get(subject) != null;
	}

	public HashMap<Subject, Score> getScores() {
		return scores;
	}

	public String getName() {
		return name;
	}

	public School getSchool() {
		return school;
	}

	public int getGrade() {
		return grade;
	}

	public String getPublicName() {
		if (name.indexOf(" ") != -1) {
			return name.substring(0, name.indexOf(" ") + 2) + ".";
		}
		else {
			return name;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + grade;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (school == null ? 0 : school.hashCode());
		result = prime * result + (scores == null ? 0 : scores.hashCode());
		return result;
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
		Student other = (Student) obj;
		if (grade != other.grade) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		}
		else if (!name.equals(other.name)) {
			return false;
		}
		if (school == null) {
			if (other.school != null) {
				return false;
			}
		}
		else if (!school.equals(other.school)) {
			return false;
		}
		if (scores == null) {
			if (other.scores != null) {
				return false;
			}
		}
		else if (!scores.equals(other.scores)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Student [name=" + name + ", school=" + school + ", grade=" + grade + ", mScore=" + scores.get('M') + ", cScore=" + scores.get('C')
				+ ", nScore=" + scores.get('N') + ", sScore=" + scores.get('S') + "]";
	}
}
