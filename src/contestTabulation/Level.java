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

public enum Level {
	ELEMENTARY("elementary", 4, 5), MIDDLE("middle", 6, 8), HIGH("high", 9, 12);

	private final int lowGrade, highGrade;
	private final String stringLevel;

	private Level(String stringLevel, int lowGrade, int highGrade) {
		this.stringLevel = stringLevel;
		this.lowGrade = lowGrade;
		this.highGrade = highGrade;
	}

	public static Level fromString(String level) {
		return Level.valueOf(Objects.requireNonNull(level).toUpperCase());
	}

	public static Level fromGrade(int grade) {
		for (Level level : Level.values()) {
			if (grade <= level.getHighGrade() && grade >= level.getLowGrade()) {
				return level;
			}
		}
		throw new IllegalArgumentException();
	}

	public int[] getGrades() {
		int[] grades = new int[highGrade - lowGrade + 1];
		for (int i = lowGrade; i < grades.length + lowGrade; i++) {
			grades[i - lowGrade] = i;
		}
		return grades;
	}

	public int getHighGrade() {
		return highGrade;
	}

	public int getLowGrade() {
		return lowGrade;
	}

	public String getName() {
		return stringLevel.substring(0, 1).toUpperCase() + stringLevel.substring(1);
	}

	@Override
	public String toString() {
		return stringLevel;
	}
}
