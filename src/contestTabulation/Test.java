/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

package contestTabulation;

public enum Test
{
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
	public String toString() { return grade + "" + test; }
	
	public static String[] tests() { return new String[] {"N", "C", "M", "S"}; }
	public static int[] grades(String level) { return level.equals("middle") ? new int[] {6, 7, 8} : new int[] {9, 10, 11, 12}; }
	public static Test[] middleTests() { return new Test[] {N6, C6, M6, S6, N7, C7, M7, S7, N8, C8, M8, S8}; }
	public static Test[] highTests() { return new Test[] {N9, C9, M9, S9, N10, C10, M10, S10, N11, C11, M11, S11, N12, C12, M12, S12}; }
}
