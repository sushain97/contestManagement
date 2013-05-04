package contestTabulation;

public enum Test
{
	//TODO: Integrate with rest of code
	N6(6, "N"), C6(6, "C"), M6(6, "M"), S6(6, "S"),
	N7(7, "N"), C7(7, "C"), M7(7, "M"), S7(7, "S"),
	N8(8, "N"), C8(8, "C"), M8(8, "M"), S8(8, "S"),
	N9(9, "N"), C9(9, "C"), M9(9, "M"), S9(9, "S"),
	N10(10, "N"), C10(10, "C"), M10(10, "M"), S10(10, "S"),
	N11(11, "N"), C11(11, "C"), M11(11, "M"), S11(11, "S"),
	N12(12, "N"), C12(12, "C"), M12(12, "M"), S12(12, "S");

	private final int grade;
	private final String test;
	
	private Test(int grade, String test)
	{
		this.grade = grade;
		this.test = test;
	}
	
	public int grade() { return grade; }
	public String test() { return test; }
	public String toString() { return test + "" + grade; }
	public Test get(String s)
	{
		for(Test test : Test.values())
			if(test.toString().equals(s))
				return test;
		return null;
	}
}
