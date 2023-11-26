package me.mrletsplay.shareclientcore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import me.mrletsplay.shareclientcore.document.Identifier;
import me.mrletsplay.shareclientcore.document.Util;

public class DecimalTest {

	@Test
	public void testSimpleSubtraction() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 0), new Identifier(3, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(1, 0), new Identifier(3, 0) };
		assertArrayEquals(new int[] { 0, 1, 0 }, Util.subtract(a, b, 0));
	}

	@Test
	public void testCarrySubtraction() {
		Identifier[] a = { new Identifier(2, 0), new Identifier(0, 0), new Identifier(4, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(0, 0), new Identifier(5, 0) };
		assertArrayEquals(new int[] { 0, Util.BASE - 1, Util.BASE - 1 }, Util.subtract(a, b, 0));
	}

	@Test
	public void testOffsetSubtraction() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(0, 0), new Identifier(5, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(0, 0), new Identifier(4, 0) };
		assertArrayEquals(new int[] { 0, 1 }, Util.subtract(a, b, 1));
	}

	@Test
	public void testReversedInputFails() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(1, 0), new Identifier(3, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(2, 0), new Identifier(3, 0) };
		assertThrows(RuntimeException.class, () -> Util.subtract(a, b, 0));
	}

	@Test
	public void testSimpleAdd1() {
		int[] a = { 0, 0 };
		Util.add1AtIndex(a, 1);
		assertArrayEquals(new int[] { 0, 1 }, a);
	}

	@Test
	public void testCarryAdd1() {
		int[] a = { 0, Util.BASE - 1 };
		Util.add1AtIndex(a, 1);
		assertArrayEquals(new int[] { 1, 0 }, a);
	}

	@Test
	public void testCarryAdd1_2() {
		int[] a = { 0, Util.BASE - 1, Util.BASE - 1 };
		Util.add1AtIndex(a, 2);
		assertArrayEquals(new int[] { 1, 0, 0 }, a);
	}

	@Test
	public void testCarryAdd1_3() {
		int[] a = { 0, Util.BASE - 1, Util.BASE - 1 };
		Util.add1AtIndex(a, 1);
		assertArrayEquals(new int[] { 1, 0, Util.BASE - 1 }, a);
	}

	@Test
	public void testIncrement_1() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(3, 0) };
		assertArrayEquals(new int[] { 1, 2, 1 }, Util.getIncremented(a, b, 1));
	}

	@Test
	public void testIncrement_2() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(2, 0), new Identifier(Util.BASE - 1, 0) };
		assertArrayEquals(new int[] { 1, 2, 0, 1 }, Util.getIncremented(a, b, 1));
	}

	@Test
	public void testCarryIncrement() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 0), new Identifier(Util.BASE - 1, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(4, 0) };
		assertArrayEquals(new int[] { 1, 3, 1 }, Util.getIncremented(a, b, 1));
	}

	@Test
	public void testConstructPosition_1() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 0), new Identifier(Util.BASE - 1, 0) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(4, 0) };
		int[] newIdent = Util.getIncremented(a, b, 1);
		Identifier[] expected = { new Identifier(1, 0), new Identifier(3, 1), new Identifier(1, 1) };
		assertArrayEquals(expected, Util.constructPosition(newIdent, a, b, 1));
	}

	@Test
	public void testConstructPosition_2() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 2), new Identifier(2, 2) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(4, 1), new Identifier(3, 1) };
		int[] newIdent = Util.getIncremented(a, b, 1);
		Identifier[] expected = { new Identifier(1, 0), new Identifier(2, 2), new Identifier(3, 3) };
		assertArrayEquals(expected, Util.constructPosition(newIdent, a, b, 3));
	}

	@Test
	public void testGeneratePosition_1() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 1) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(3, 2) };
		Identifier[] newIdent = Util.generatePositionBetween(a, b, 3);
		Identifier[] expected = { new Identifier(1, 0), new Identifier(2, 1), new Identifier(1, 3) };
		assertArrayEquals(expected, newIdent);
	}

	@Test
	public void testGeneratePosition_2() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 1) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(2, 2) };
		Identifier[] newIdent = Util.generatePositionBetween(a, b, 3);
		Identifier[] expected = { new Identifier(1, 0), new Identifier(2, 1), new Identifier(1, 3) };
		assertArrayEquals(expected, newIdent);
	}

	@Test
	public void testGeneratePositionInvalidInputFails() {
		Identifier[] a = { new Identifier(1, 0), new Identifier(2, 2) };
		Identifier[] b = { new Identifier(1, 0), new Identifier(2, 1) };
		assertThrows(IllegalArgumentException.class, () -> Util.generatePositionBetween(a, b, 3));
	}

}
