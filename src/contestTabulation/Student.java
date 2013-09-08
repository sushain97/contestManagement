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

import java.util.HashMap;

public class Student
{
	final private String name;
	final private School school;
	final private int grade;
	private static int anonCounter = 0;
	private HashMap<Character,Score> scores = new HashMap<Character,Score>();

	Student(String name, School school, int grade) { this.name = name; this.grade = grade; this.school = school; }
	Student(int grade, School school) { this("Anonymous" + anonCounter, school, grade); anonCounter++; }

	public void setScore(char subject, Score score) { scores.put(subject, score); }

	public Score getScore(char subject) { return scores.get(subject); }
	public Score getScore(String subject) { return scores.get(subject.charAt(0)); }
	public boolean hasScore(char subject) { return scores.get(subject) != null; }
	public boolean hasScore(String subject) { return scores.get(subject.charAt(0)) != null; }
	public HashMap<Character,Score> getScores() { return scores; }

	public String getName() { return name; }
	public School getSchool() { return school; }
	public int getGrade() { return grade; }
	public String getPublicName()
	{
		if(name.indexOf(" ") != -1)
			return name.substring(0, name.indexOf(" ") + 2) + ".";
		else
			return name;
	}
	
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + grade;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((school == null) ? 0 : school.hashCode());
		return result;
	}

	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Student other = (Student) obj;
		if (grade != other.grade)
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (school == null)
		{
			if (other.school != null)
				return false;
		}
		else if (!school.equals(other.school))
			return false;
		return true;
	}

	public String toString()
	{
		return "Student [name=" + name + ", school=" + school + ", grade="
				+ grade + ", mScore=" + scores.get('M') + ", cScore=" + scores.get('C')
				+ ", nScore=" + scores.get('N') + ", sScore=" + scores.get('S') + "]";
	}
}