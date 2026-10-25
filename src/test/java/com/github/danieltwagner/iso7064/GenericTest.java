package com.github.danieltwagner.iso7064;

import org.junit.Test;

import junit.framework.TestCase;

public abstract class GenericTest extends TestCase {

	protected abstract PureSystemCalculator getTestedClass();
	protected abstract String getInputCharacters();
	
	@Test
	public void testCoherence() {
		PureSystemCalculator calc = getTestedClass();
		
		String firstCharacter = getInputCharacters().substring(0, 1);
		String[] inputs = { firstCharacter, repeat(firstCharacter.charAt(0), 5), getInputCharacters(),
			getInputCharacters() + getInputCharacters(),
		repeat(getInputCharacters().charAt(getInputCharacters().length() - 1), 256) };
		for (String input : inputs) {
			String output = calc.compute(input);
			assertTrue(input + " generated invalid output " + output, calc.verify(output));
		}
	}

	@Test
	public void testRejectsTooShortInput() {
		PureSystemCalculator calc = getTestedClass();
		int checkCharacterCount = calc.isDoubleCheckDigit() ? 2 : 1;
		String shortestInput = repeat(getInputCharacters().charAt(0), checkCharacterCount);
		try {
			calc.verify(shortestInput);
			fail("Expected an input without protected data to be rejected");
		} catch (IllegalArgumentException expected) {
			// A code must contain at least one data character and its check character(s).
		}
	}

	@Test
	public void testRejectsIllegalCharacters() {
		try {
			getTestedClass().compute("!");
			fail("Expected an illegal character to be rejected");
		} catch (IllegalStateException expected) {
			// The character is outside this system's alphabet.
		}
	}

	@Test
	public void testRejectsIllegalCharacterInProtectedData() {
		try {
			getTestedClass().verify("!00");
			fail("Expected an illegal data character to be rejected");
		} catch (IllegalStateException expected) {
			// The character is outside this system's alphabet.
		}
	}

	@Test
	public void testRejectsIllegalCharacterInCheckCharacters() {
		PureSystemCalculator calc = getTestedClass();
		String validCode = calc.compute(getInputCharacters());
		String corrupted = validCode.substring(0, validCode.length() - 1) + "!";
		assertFalse(calc.verify(corrupted));
	}

	@Test
	public void testDetectsEverySingleCharacterSubstitution() {
		PureSystemCalculator calc = getTestedClass();
		String input = getInputCharacters();
		String validCode = calc.compute(input);
		String alphabet = getTestedClass().getCharacterSet();

		for (int position = 0; position < validCode.length(); position++) {
			for (int replacement = 0; replacement < alphabet.length(); replacement++) {
				char candidate = alphabet.charAt(replacement);
				if (candidate == validCode.charAt(position)) continue;

				String corrupted = validCode.substring(0, position) + candidate
					+ validCode.substring(position + 1);
				assertFalse("Accepted substitution at position " + position + ": " + corrupted,
					calc.verify(corrupted));
			}
		}
	}

	@Test
	public void testDetectsAdjacentTranspositions() {
		PureSystemCalculator calc = getTestedClass();
		String input = getInputCharacters();
		String validCode = calc.compute(input);
		String checkCharacters = validCode.substring(input.length());

		for (int i = 0; i < input.length() - 1; i++) {
			if (input.charAt(i) == input.charAt(i + 1)) continue;
			String transposed = input.substring(0, i) + input.charAt(i + 1)
				+ input.charAt(i) + input.substring(i + 2) + checkCharacters;
			assertFalse("Accepted adjacent transposition: " + transposed, calc.verify(transposed));
		}
	}

	@Test
	public void testVerifyIsCaseInsensitive() {
		PureSystemCalculator calc = getTestedClass();
		String validCode = calc.compute(getInputCharacters());
		assertTrue(calc.verify(validCode.toLowerCase()));
	}

	private String repeat(char character, int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++) result.append(character);
		return result.toString();
	}
}
