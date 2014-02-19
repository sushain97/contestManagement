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

package util;

import java.util.HashMap;

public class PropNames
{
	public static HashMap<String, String> names = new HashMap<String, String>();
	static {
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
		for (int i = 6; i <= 12; i++) {
			for (int j = 0; j < 4; j++) {
				names.put(String.valueOf(i) + subjects[j], i + "th Grade " + subjectNames.get(subjects[j]) + " Registrations");
			}
		}
	}
}
