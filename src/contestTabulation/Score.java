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

public class Score implements Comparable<Score>
{
	final private String score;
	Score(String score) { this.score = score; }
	public String getScore() { return score; }

	public int getScoreNum()
	{
		try { return Integer.parseInt(score); }
		catch(NumberFormatException e) { return Integer.parseInt(score.substring(0,score.length()-1)); }
	}

	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		return result;
	}

	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Score other = (Score) obj;
		if(score == null)
		{
			if (other.score != null)
				return false;
		}
		else if(!score.equals(other.score))
			return false;
		return true;
	}

	public int compareTo(Score other)
	{
		if(other.equals(this))
			return 0;

		int thisScore = 0;
		int otherScore = 0;

		if(isInteger(getScore()) && isInteger(other.getScore()))
			return Integer.parseInt(getScore()) - Integer.parseInt(other.getScore());
		else if(isInteger(getScore()) && !isInteger(other.getScore()))
		{
			thisScore = Integer.parseInt(getScore());
			otherScore = Integer.parseInt(other.getScore().substring(0, other.getScore().length()-1));
			if(thisScore == otherScore)
				return 0;
			else
				return thisScore - otherScore;
		}
		else if(!isInteger(getScore()) && isInteger(other.getScore()))
		{
			thisScore = Integer.parseInt(getScore().substring(0, getScore().length()-1));
			otherScore = Integer.parseInt(other.getScore());
			if(thisScore == otherScore)
				return 0;
			else
				return thisScore - otherScore;
		}
		else
		{
			thisScore = Integer.parseInt(getScore().substring(0, getScore().length()-1));
			otherScore = Integer.parseInt(other.getScore().substring(0, other.getScore().length()-1));
			if(thisScore == otherScore)
			{
				char thisScoreLetter = getScore().charAt(getScore().length()-1);
				char otherScoreLetter = other.getScore().charAt(other.getScore().length()-1);
				return otherScoreLetter - thisScoreLetter;
			}
			else
				return thisScore - otherScore;
		}
	}

	public String toString() { return "Score [score=" + score + "]"; }

	private static boolean isInteger(String str)
	{
		if(str == null)
			return false;
		int length = str.length();
		if(length == 0)
			return false;
		int i = 0;
		if(str.charAt(0) == '-')
		{
			if (length == 1)
				return false;
			i = 1;
		}
		for(; i < length; i++)
		{
			char c = str.charAt(i);
			if (c <= '/' || c >= ':')
				return false;
		}
		return true;
	}
}