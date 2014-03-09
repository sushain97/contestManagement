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
	MIDDLE("middle", 6, 8), HIGH("high", 9, 12);

	private final int lowGrade, highGrade;
	private final String stringLevel;

	private Level(String stringLevel, int lowGrade, int highGrade) {
		this.stringLevel = stringLevel;
		this.lowGrade = lowGrade;
		this.highGrade = highGrade;
	}

	public static Level fromString(String level) {
		if ("middle".compareToIgnoreCase(Objects.requireNonNull(level)) == 0) {
			return Level.MIDDLE;
		}
		else if ("high".compareToIgnoreCase(level) == 0) {
			return Level.HIGH;
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public int[] getGrades() {
		if (this == MIDDLE) {
			return new int[] {6, 7, 8};
		}
		else {
			return new int[] {9, 10, 11, 12};
		}
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
