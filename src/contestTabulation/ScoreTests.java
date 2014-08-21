package contestTabulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import util.Pair;

public class ScoreTests {
	@Test
	public void testZero() {
		String[] zeroes = {"0", "+0", "-0", "00", "0.0", "0.0", "+0.0", "-0.0", "-00.00"};

		for (String zero : zeroes) {
			assertEquals(zero + " must result in (0, 0)", new Pair<Integer, Integer>(0, 0), new Score(zero).getScore());
		}
	}

	@Test
	public void testInts() {
		assertEquals("0 must result in (0, 0)", new Pair<Integer, Integer>(0, 0), new Score("0").getScore());
		assertEquals("25 must result in (25, 0)", new Pair<Integer, Integer>(25, 0), new Score("25").getScore());
		assertEquals("400 must result in (400, 0)", new Pair<Integer, Integer>(400, 0), new Score("400").getScore());
		assertEquals("-25 must result in (-25, 0)", new Pair<Integer, Integer>(-25, 0), new Score("-25").getScore());
		assertEquals("-400 must result in (-400, 0)", new Pair<Integer, Integer>(-400, 0), new Score("-400").getScore());
		assertEquals("+25 must result in (25, 0)", new Pair<Integer, Integer>(25, 0), new Score("+25").getScore());
		assertEquals("+400 must result in (400, 0)", new Pair<Integer, Integer>(400, 0), new Score("+400").getScore());
	}

	@Test
	public void testLetters() {
		assertEquals("0A must result in (0, 1)", new Pair<Integer, Integer>(0, 1), new Score("0A").getScore());
		assertEquals("25A must result in (25, 1)", new Pair<Integer, Integer>(25, 1), new Score("25A").getScore());
		assertEquals("25a must result in (25, 1)", new Pair<Integer, Integer>(25, 1), new Score("25a").getScore());
		assertEquals("400z must result in (25, 26)", new Pair<Integer, Integer>(400, 26), new Score("400z").getScore());
		assertEquals("25F must result in (25, 6)", new Pair<Integer, Integer>(25, 6), new Score("25F").getScore());
		assertEquals("25f must result in (25, 6)", new Pair<Integer, Integer>(25, 6), new Score("25f").getScore());
		assertEquals("+25F must result in (25, 6)", new Pair<Integer, Integer>(25, 6), new Score("+25F").getScore());
		assertEquals("025F must result in (25, 6)", new Pair<Integer, Integer>(25, 6), new Score("025F").getScore());
		assertEquals("-25F must result in (-25, 6)", new Pair<Integer, Integer>(-25, 6), new Score("-25F").getScore());
		assertEquals("-025F must result in (-25, 6)", new Pair<Integer, Integer>(-25, 6), new Score("-025F").getScore());
	}

	@Test
	public void testDecimals() {
		assertEquals("0.0 must result in (0, 0)", new Pair<Integer, Integer>(0, 0), new Score("0.0").getScore());
		assertEquals("0.1 must result in (0, 1)", new Pair<Integer, Integer>(0, 1), new Score("0.1").getScore());
		assertEquals("25.1 must result in (25, 1)", new Pair<Integer, Integer>(25, 1), new Score("25.1").getScore());
		assertEquals("-25.1 must result in (-25, 1)", new Pair<Integer, Integer>(-25, 1), new Score("-25.1").getScore());
		assertEquals("-25.0 must result in (-25, 0)", new Pair<Integer, Integer>(-25, 0), new Score("-25.0").getScore());
		assertEquals("25.12 must result in (25, 12)", new Pair<Integer, Integer>(25, 12), new Score("25.12").getScore());
		assertEquals("-025.12 must result in (-25, 12)", new Pair<Integer, Integer>(-25, 12), new Score("-025.12").getScore());
		assertEquals("400.8 must result in (400, 8)", new Pair<Integer, Integer>(400, 8), new Score("400.8").getScore());
		assertEquals("0400.12 must result in (400, 12)", new Pair<Integer, Integer>(400, 12), new Score("0400.12").getScore());
	}

	@Test
	public void testFlags() {
		assertEquals("NS must result in (-401, 0)", new Pair<Integer, Integer>(-401, 0), new Score("NS").getScore());
		assertFalse(new Score("NS").isNumeric());
		assertEquals("NG must result in (-401, 0)", new Pair<Integer, Integer>(-401, 0), new Score("NG").getScore());
		assertFalse(new Score("NG").isNumeric());
		assertEquals("DQ must result in (-402, 0)", new Pair<Integer, Integer>(-402, 0), new Score("DQ").getScore());
		assertFalse(new Score("DQ").isNumeric());
	}

	@Test
	public void testTrim() {
		assertEquals("-00400.012 must result in (-400, 12)", new Pair<Integer, Integer>(-400, 12), new Score("-00400.012").getScore());
		assertEquals("-00400.0010 must result in (-400, 10)", new Pair<Integer, Integer>(-400, 10), new Score("-00400.0010").getScore());
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testMissingScoreWithLetterModifier() {
		new Score("A");
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testMissingScoreWithNumberModifier() {
		new Score(".11");
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testScoreTooHigh() {
		new Score("402");
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testScoreTooLow() {
		new Score("-402");
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testModifierLengthExceeded() {
		new Score("123.101");
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testBadModifier() {
		new Score("124%");
	}

	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testInvalidModifier() {
		new Score("124AA");
	}
}
