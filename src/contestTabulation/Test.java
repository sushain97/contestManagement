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

import java.util.Objects;

public enum Test {
	N4(4, Subject.N), C4(4, Subject.C), M4(4, Subject.M), S4(4, Subject.S),
	N5(5, Subject.N), C5(5, Subject.C), M5(5, Subject.M), S5(5, Subject.S),

	N6(6, Subject.N), C6(6, Subject.C), M6(6, Subject.M), S6(6, Subject.S),
	N7(7, Subject.N), C7(7, Subject.C), M7(7, Subject.M), S7(7, Subject.S),
	N8(8, Subject.N), C8(8, Subject.C), M8(8, Subject.M), S8(8, Subject.S),

	N9(9, Subject.N), C9(9, Subject.C), M9(9, Subject.M), S9(9, Subject.S),
	N10(10, Subject.N), C10(10, Subject.C), M10(10, Subject.M), S10(10, Subject.S),
	N11(11, Subject.N), C11(11, Subject.C), M11(11, Subject.M), S11(11, Subject.S),
	N12(12, Subject.N), C12(12, Subject.C), M12(12, Subject.M), S12(12, Subject.S);

	public static Test fromString(String testString) {
		if (Objects.requireNonNull(testString).length() == 2 || testString.length() == 3) {
			return Test.valueOf(testString.substring(testString.length() - 1) + testString.substring(0, testString.length() - 1));
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public static Test fromSubjectAndGrade(int grade, Subject subject) {
		return Test.valueOf(Objects.requireNonNull(subject).toString() + grade);
	}

	public static Test[] highTests() {
		return new Test[] {N9, C9, M9, S9, N10, C10, M10, S10, N11, C11, M11, S11, N12, C12, M12, S12};
	}

	public static Test[] middleTests() {
		return new Test[] {N6, C6, M6, S6, N7, C7, M7, S7, N8, C8, M8, S8};
	}

	public static Test[] elementaryTests() {
		return new Test[] {N4, C4, M4, S4, N5, C5, M5, S5};
	}

	public static Test[] getTests(Level level) {
		switch (Objects.requireNonNull(level)) {
			case ELEMENTARY:
				return elementaryTests();
			case MIDDLE:
				return middleTests();
			case HIGH:
				return highTests();
		}

		throw new IllegalArgumentException();
	}

	private final int grade;

	private final Subject subject;

	private Test(int grade, Subject subject) {
		this.grade = grade;
		this.subject = subject;
	}

	public int getGrade() {
		return grade;
	}

	public int getMaxTeamScore() {
		switch (subject) {
			case N:
				return 400;
			case C:
				switch (Level.fromGrade(grade)) {
					case ELEMENTARY:
						return 400;
					case MIDDLE:
						return 400;
					case HIGH:
						return 350;
				}
			case M:
				switch (Level.fromGrade(grade)) {
					case ELEMENTARY:
						return 250;
					case MIDDLE:
						return 250;
					case HIGH:
						return 360;
				}
			case S:
				switch (Level.fromGrade(grade)) {
					case ELEMENTARY:
						return 250;
					case MIDDLE:
						return 250;
					case HIGH:
						return 360;
				}
		}

		throw new IllegalArgumentException();
	}

	public Subject getSubject() {
		return subject;
	}

	@Override
	public String toString() {
		return grade + "" + subject;
	}
}
