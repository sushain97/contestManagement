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

package contestTabulation;

public enum Subject {
	N("Number Sense"), C("Calculator"), M("Math"), S("Science");

	public static String[] getSubjectNames() {
		return new String[] {N.getName(), C.getName(), M.getName(), S.getName()};
	}

	private final String subjectName;

	private Subject(String subjectName) {
		this.subjectName = subjectName;
	}

	public String getName() {
		return subjectName;
	}

	@Override
	public String toString() {
		return this.name();
	}
}
