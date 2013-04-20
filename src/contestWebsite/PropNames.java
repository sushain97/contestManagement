package contestWebsite;

import java.util.HashMap;

public class PropNames
{
	public HashMap<String, String> propNames = new HashMap<String, String>();
	public PropNames()
	{
		//TODO: Change to ENUM or equivalent
		propNames.put("schoolName", "School Name");
		propNames.put("registrationType", "Registration Type");
		propNames.put("aliases", "School Aliases");
		propNames.put("cost", "Registration Fees");
		propNames.put("username", "Account Username");
		propNames.put("email", "E-Mail Address");
		propNames.put("schoolLevel", "School Level");
		propNames.put("name", "Name");
		propNames.put("account", "Account");
		propNames.put("comment", "Comment");
		propNames.put("resolved", "Resolution Status");
		propNames.put("user-id", "Username");
		propNames.put("school", "School");

		String[] subjects = {"n", "c", "m", "s"};
		HashMap<String, String> subjectNames = new HashMap<String, String>();
		subjectNames.put("n", "Number Sense");
		subjectNames.put("c", "Calculator");
		subjectNames.put("m", "Math");
		subjectNames.put("s", "Science");
		for(int i = 6; i <= 12; i++)
			for(int j = 0; j < 4; j++)
				propNames.put(String.valueOf(i) + subjects[j], i + "th Grade " + subjectNames.get(subjects[j]) + " Registrations");
	}
}
