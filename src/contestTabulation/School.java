package contestTabulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class School
{
	private String name;
	private String level;
	private ArrayList<Student> students = new ArrayList<Student>();

	private HashMap<Character,Integer> scores = new HashMap<Character,Integer>();
	private HashMap<String,ArrayList<Score>> anonScores = new HashMap<String,ArrayList<Score>>();
	private int totalScore;

	School(String name, String level) { this.name = name; this.level = level; }
	void addStudent(Student student) { students.add(student); }
	void addAnonScores(String test, ArrayList<Score> scores) { anonScores.put(test, scores); }
	public ArrayList<Student> getStudents() { return students; }
	public ArrayList<Score> getAnonScores(String test) { return anonScores.get(test); }
	public String getName() { return name; }
	public int getNumStudents() { return students.size(); }
	public String getLevel() { return level; }

	HashMap<Score,Student> calculateScore(char subject)
	{
		HashMap<Score,Student> top4 = new HashMap<Score,Student>();
		ArrayList<Score> top4Arr;
		for(Student student : students)
		{
			top4Arr = new ArrayList<Score>(top4.keySet());

			if(student.getScore(subject) != null && (top4Arr.size() < 4 || student.getScore(subject).compareTo(Collections.min(top4Arr)) > 0))
			{
				int highGrade = (level.equals("middle") ? 8 : 12);
				if(student.getGrade() == highGrade)
				{
					int inGrade = 0;
					Score low = new Score("1000");
					for(Score score : top4Arr)
					{
						if(top4.get(score).getGrade() == highGrade)
						{
							inGrade++;
							if(score.compareTo(low) < 0)
								low = score;
						}
					}

					if(inGrade == 3)
					{
						if(student.getScore(subject).compareTo(low) > 0)
						{
							top4.remove(top4.get(low));
							top4.put(student.getScore(subject), student);
						}
						else
							continue;
					}

				}

				if(top4.size() == 4)
					top4.remove(Collections.min(top4Arr));
				top4.put(student.getScore(subject), student);
			}
		}
		for(int grade = (level.equals("middle") ? 6 : 9); grade <= (level.equals("middle") ? 8 : 12); grade++)
			if(anonScores.get(Integer.toString(grade) + Character.toString(subject)) != null)
				for(Score score : anonScores.get(Integer.toString(grade) + Character.toString(subject)))
				{
					top4Arr = new ArrayList<Score>(top4.keySet());
					if(top4Arr.size() < 4 || score.compareTo(Collections.min(top4Arr)) > 0)
					{
						int highGrade = (level.equals("middle") ? 8 : 12);
						if(grade == highGrade)
						{
							int inGrade = 0;
							Score low = new Score("1000");
							for(Score highScore : top4Arr)
							{
								int tempGrade = top4.get(highScore) != null ? top4.get(highScore).getGrade() : grade;
								if(tempGrade == highGrade)
								{
									inGrade++;
									if(score.compareTo(low) < 0)
										low = score;
								}
							}

							if(inGrade == 3)
							{
								if(score.compareTo(low) > 0)
								{
									top4.remove(top4.get(low));
									top4.put(score, null);
								}
								else
									continue;
							}
						}

						if(top4.size() == 4)
							top4.remove(Collections.min(top4Arr));
						top4.put(score, null);
					}
				}

		int totalScore = 0;
		for(Score score : top4.keySet())
			if(score != null)
				totalScore += score.getScoreNum();

		scores.put(subject, totalScore);
		return top4;
	}

	void calculateScores()
	{
		calculateScore('N');
		calculateScore('S');
		calculateScore('C');
		calculateScore('M');
		if(level.equals("middle"))
			totalScore = scores.get('N') + scores.get('C') + (int) Math.round((scores.get('M') * 8.0/5.0) +(scores.get('S') * 8.0/5.0));
		else
			totalScore = scores.get('N') + (int) Math.round((scores.get('M') * 10.0/9.0) +(scores.get('S') * 10.0/9.0) + (scores.get('C') * 8.0/7.0));
	}

	public int getScore(char subject) { return scores.get(subject); }
	public int getScore(String subject) { return scores.get(subject.charAt(0)); }

	public int getTotalScore() { return totalScore; }

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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString()
	{
		return "School [name=" + name + ", level=" + level + ", totalScore=" + totalScore + "]";
	}
}