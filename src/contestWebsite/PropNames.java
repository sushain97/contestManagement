package contestWebsite;

import java.util.HashMap;

public class PropNames
{
	public static HashMap<String, String> names = new HashMap<String, String>();
	static
	{
		names.put("schoolName", "School Name");
		names.put("registrationType", "Registration Type");
		names.put("aliases", "School Aliases");
		names.put("cost", "Registration Fees");
		names.put("username", "Account Username");
		names.put("email", "E-Mail Address");
		names.put("schoolLevel", "School Level");
		names.put("name", "Name");
		names.put("account", "Account");
		names.put("comment", "Comment");
		names.put("resolved", "Resolution Status");
		names.put("user-id", "Username");
		names.put("school", "School");

		String[] subjects = {"n", "c", "m", "s"};
		HashMap<String, String> subjectNames = new HashMap<String, String>();
		subjectNames.put("n", "Number Sense");
		subjectNames.put("c", "Calculator");
		subjectNames.put("m", "Math");
		subjectNames.put("s", "Science");
		for(int i = 6; i <= 12; i++)
			for(int j = 0; j < 4; j++)
				names.put(String.valueOf(i) + subjects[j], i + "th Grade " + subjectNames.get(subjects[j]) + " Registrations");
	}
}