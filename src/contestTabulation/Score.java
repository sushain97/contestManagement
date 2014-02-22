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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import util.Pair;

@PersistenceCapable
public class Score implements Comparable<Score>, java.io.Serializable {
	private static final long serialVersionUID = 1631716116306090911L;

	private static final String LETTER_FORMAT = "^((?:[\\+-])?[0-9]+)([A-z])?$";
	private static final String DECIMAL_FORMAT = "^((?:[\\+-])?[0-9]+)(?:\\.([0-9]+))?$";

	public static boolean isScore(String str) {
		return str.matches(LETTER_FORMAT) || str.matches(DECIMAL_FORMAT);
	}

	@Persistent private Pair<Integer, Integer> score; // Number, Modifier

	Score(String score) {
		score = Objects.requireNonNull(score).trim();
		try {
			Integer scoreNum, scoreMod;
			if (score.matches(LETTER_FORMAT)) {
				Pattern pattern = Pattern.compile(LETTER_FORMAT);
				Matcher matcher = pattern.matcher(score);
				matcher.matches();

				scoreNum = Integer.parseInt(matcher.group(1), 10);
				if (matcher.group(2) != null) {
					char letter = matcher.group(2).toUpperCase().charAt(0);
					if (letter >= 'A' && letter <= 'Z') {
						scoreMod = letter - 64;
					}
					else {
						throw new IllegalArgumentException("Invalid letter modifier");
					}
				}
				else {
					scoreMod = 0;
				}
			}
			else if (score.matches(DECIMAL_FORMAT)) {
				Pattern pattern = Pattern.compile(DECIMAL_FORMAT);
				Matcher matcher = pattern.matcher(score);
				matcher.matches();

				scoreNum = Integer.parseInt(matcher.group(1), 10);
				if (matcher.group(2) != null) {
					scoreMod = Integer.parseInt(matcher.group(2), 10);
				}
				else {
					scoreMod = 0;
				}
			}
			else {
				throw new IllegalArgumentException("Invalid score format");
			}

			this.score = new Pair<Integer, Integer>(scoreNum, scoreMod);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse score");
		}

		if (this.score.x > 1000) {
			throw new IllegalArgumentException("Score larger than expected: " + this.score.x);
		}

		if (this.score.y > 100) {
			throw new IllegalArgumentException("Score modifier larger than expected: " + this.score.y);
		}
	}

	@Override
	public int compareTo(Score other) {
		if (other.equals(this)) {
			return 0;
		}
		else {
			Pair<Integer, Integer> otherScore = other.getScore();
			Pair<Integer, Integer> thisScore = this.getScore();

			if (thisScore.x != otherScore.x) {
				return thisScore.x - otherScore.x;
			}
			else {
				return thisScore.y - otherScore.y;
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Score other = (Score) obj;
		if (score == null) {
			if (other.score != null) {
				return false;
			}
		}
		else if (!score.equals(other.score)) {
			return false;
		}
		return true;
	}

	public Pair<Integer, Integer> getScore() {
		return score;
	}

	public int getScoreNum() {
		return score.x;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (score == null ? 0 : score.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return score.x.toString() + (score.y != 0 ? (char) (score.y + 64) : "");
	}
}
