package contestTabulation;

public class Score implements Comparable<Score>
{
	private String score;
	Score(String score) { this.score = score; }
	public String getScore() { return score; }

	int getScoreNum()
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