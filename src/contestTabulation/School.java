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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import util.Pair;

public class School
{
	final private String name;
	final private String level;
	final private int lowGrade;
	final private int highGrade;
	private ArrayList<Student> students = new ArrayList<Student>();
	
	private HashMap<Test,Integer> numTests = new HashMap<Test,Integer>();

	private HashMap<Character,Pair<Student[],Integer>> scores = new HashMap<Character,Pair<Student[],Integer>>();
	private HashMap<Test,ArrayList<Score>> anonScores = new HashMap<Test,ArrayList<Score>>();
	private int totalScore;

	School(String name, String level)
	{
		this.name = name;
		this.level = level;
		this.lowGrade = level.equals("middle") ? 6 : 9;
		this.highGrade = level.equals("middle") ? 8 : 12;
	}
	public ArrayList<Student> getStudents() { return students; }
	public ArrayList<Score> getAnonScores(Test test) { return anonScores.get(test); }
	public String getName() { return name; }
	public int getNumStudents() { return students.size(); }
	public HashMap<Test,Integer> getNumTests() { return numTests; }
	public String getLevel() { return level; }
	
	public Student[] getScoreStudents(char subject) { return scores.get(subject).x; }
	public Student[] getScoreStudents(String subject) { return scores.get(subject.charAt(0)).x; }
	public int getScore(char subject) { return scores.get(subject).y; }
	public int getScore(String subject) { return scores.get(subject.charAt(0)).y; }
	public int getTotalScore() { return totalScore; }
	
	protected void addStudent(Student student) { students.add(student);	}
	protected void addAnonScores(Test test, ArrayList<Score> scores) 
	{
		anonScores.put(test, scores);
		if(!numTests.containsKey(test))
			numTests.put(test, scores.size());
		else
			numTests.put(test, numTests.get(test) + scores.size());
	}
	
	public void calculateTestNums()
	{
		for(Student student : students)
		{
			String grade = Integer.toString(student.getGrade());
			for(Character t : student.getScores().keySet())
			{
				Test test = Test.valueOf(Character.toUpperCase(t) + grade);
				if(!numTests.containsKey(test))
					numTests.put(test, 1);
				else
					numTests.put(test, numTests.get(test) + 1);
			}
			
		}
	}

	private HashMap<Student, Score> calculateScore(final char subject)
	{	
		ArrayList<Student> subjectStudents = new ArrayList<Student>();

		for(Student student : students)
			if(student.hasScore(subject))
				subjectStudents.add(student);
		
		for(int grade = lowGrade; grade <= highGrade; grade++)
		{
			ArrayList<Score> scores = anonScores.get(Test.valueOf(subject + Integer.toString(grade)));
			if(scores != null)
				for(Score score : scores)
				{
					Student tempStudent = new Student(grade, this);
					tempStudent.setScore(subject, score);
					subjectStudents.add(tempStudent);
				}
		}
		
		Collections.sort(subjectStudents, Collections.reverseOrder(new Comparator<Student>() 
			{ public int compare(Student s1, Student s2) { return s1.getScore(subject).compareTo(s2.getScore(subject)); } }
		));

		int inHighGrade = 0;
		HashMap<Student,Score> top4 = new HashMap<Student,Score>();
		for(Student student : subjectStudents)
			if(top4.size() < 4)
			{
				Score score = student.getScore(subject);
				if(highGrade == student.getGrade() && inHighGrade < 3)
				{
					top4.put(student, score);
					inHighGrade++;
				}
				else if(highGrade != student.getGrade())
					top4.put(student, score);
			}

		int totalScore = 0;
		for(Score score : top4.values())
			if(score != null)
				totalScore += score.getScoreNum();
		scores.put(subject, new Pair<Student[],Integer>(top4.keySet().toArray(new Student[top4.keySet().size()]), totalScore));
		
		return top4;
	}

	public void calculateScores()
	{
		calculateScore('N');
		calculateScore('S');
		calculateScore('C');
		calculateScore('M');
		if(level.equals("middle"))
			totalScore = scores.get('N').y + scores.get('C').y + (int) Math.round((scores.get('M').y * 8.0/5.0) +(scores.get('S').y * 8.0/5.0));
		else
			totalScore = scores.get('N').y + (int) Math.round((scores.get('M').y * 10.0/9.0) + (scores.get('S').y * 10.0/9.0) + (scores.get('C').y * 8.0/7.0));
	}

	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		School other = (School) obj;
		if (name == null)
			if (other.name != null)
				return false;
		else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString()
	{
		return "School [name=" + name + ", level=" + level + ", totalScore=" + totalScore + "]";
	}
}