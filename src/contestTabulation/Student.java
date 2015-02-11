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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.datanucleus.annotations.Unowned;

@PersistenceCapable
public class Student implements Serializable {
	@Deprecated private static int anonCounter = 0;
	private static final long serialVersionUID = 8963228643353855438L;

	public static Comparator<Student> getNameComparator() {
		return new Comparator<Student>() {
			@Override
			public int compare(Student s1, Student s2) {
				return s1.getName().compareTo(s2.getName());
			}
		};
	}

	public static Comparator<Student> getScoreComparator(final Subject subject) {
		return new Comparator<Student>() {
			@Override
			public int compare(Student s1, Student s2) {
				return s1.getScore(subject).compareTo(s2.getScore(subject));
			}
		};
	}

	@Persistent private int grade;
	@Persistent private String name;
	@Persistent private School school;
	@Persistent(serialized = "true") @Unowned private Map<Subject, Score> scores = new HashMap<Subject, Score>();
	@Persistent private List<Subject> registeredSubjects = new ArrayList<Subject>();

	@PrimaryKey private Key key;

	@Deprecated
	Student(int grade, School school) {
		this("Anonymous" + anonCounter, school, grade);
		anonCounter++;
	}

	Student(String name, School school, int grade) {
		this.name = Objects.requireNonNull(name);
		this.grade = Objects.requireNonNull(grade);
		this.school = Objects.requireNonNull(school);
		key = KeyFactory.createKey(this.school.getKey(), this.getClass().getSimpleName(), name + "_" + grade + "_" + school.getName());
	}

	Student(String name, School school, int grade, List<Subject> registeredSubjects) {
		this(name, school, grade);
		registeredSubjects = Objects.requireNonNull(registeredSubjects);
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
		return true;
	}

	public int getGrade() {
		return grade;
	}

	public String getName() {
		return name;
	}

	public String getPublicName() {
		if (name.indexOf(" ") != -1) {
			return name.substring(0, name.indexOf(" ") + 2) + ".";
		}
		else {
			return name;
		}
	}

	public List<Subject> getRegisteredSubjects() {
		return registeredSubjects;
	}

	public School getSchool() {
		return school;
	}

	public Score getScore(Subject subject) {
		return scores.get(Objects.requireNonNull(subject));
	}

	public Map<Subject, Score> getScores() {
		return scores;
	}

	public Key getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + grade;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (school == null ? 0 : school.hashCode());
		return result;
	}

	public boolean hasScore(Subject subject) {
		return scores.get(Objects.requireNonNull(subject)) != null;
	}

	public void setScore(Subject subject, Score score) {
		scores.put(Objects.requireNonNull(subject), Objects.requireNonNull(score));
	}

	public boolean shouldHaveScore(Subject subject) {
		return registeredSubjects.contains(Objects.requireNonNull(subject));
	}

	public void setRegisteredSubjects(List<Subject> registeredSubjects) {
		this.registeredSubjects = registeredSubjects;
	}

	@Override
	public String toString() {
		return "Student [grade=" + grade + ", name=" + name + ", school=" + school + ", scores=" + scores + "]";
	}
}
