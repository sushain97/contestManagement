package contestTabulation;

public enum Level
{
	MIDDLE("middle", 6, 8), HIGH("high", 9, 12);

	private final String stringLevel;
	private final int lowGrade, highGrade;

	private Level(String stringLevel, int lowGrade, int highGrade)
	{
		this.stringLevel = stringLevel;
		this.lowGrade = lowGrade;
		this.highGrade = highGrade;
	}

	public String toString() { return stringLevel; }
	public Level fromString(String level) {	return level.toLowerCase().equals("middle") ? Level.MIDDLE : Level.HIGH; }

	public int getLowGrade() { return lowGrade; }
	public int getHighGrade() { return highGrade; }
}
