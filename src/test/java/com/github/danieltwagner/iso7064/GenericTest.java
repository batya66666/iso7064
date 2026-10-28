package com.github.danieltwagner.iso7064;

import org.junit.Test;
import java.util.Random;

import junit.framework.TestCase;

public abstract class GenericTest extends TestCase {

	protected abstract PureSystemCalculator getTestedClass();
	protected abstract String getInputCharacters();
	protected abstract Iso7064Reference getReferenceSystem();
	
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
	public void testExhaustiveInputsOfLengthOneToThreeAgainstPolynomialReference() {
		PureSystemCalculator calc = getTestedClass();
		Iso7064Reference reference = getReferenceSystem();
		for (int length = 1; length <= 3; length++) {
			int checked = assertAllInputs(calc, reference, "", length);
			assertEquals("Did not enumerate all strings of length " + length,
				(int) Math.pow(getInputCharacters().length(), length), checked);
		}
	}

	@Test
	public void testGeneratedComputeVerifyProperty() {
		PureSystemCalculator calc = getTestedClass();
		Random random = new Random(7064L + getReferenceSystem().modulus);
		for (int sample = 0; sample < 500; sample++) {
			String input = randomInput(random, 1 + random.nextInt(100));
			String expected = getReferenceSystem().compute(input);
			assertEquals("Independent result for " + input, expected, calc.compute(input));
			assertTrue("Generated code should verify: " + expected, calc.verify(expected));
		}
	}

	@Test
	public void testGeneratedSingleSubstitutionProperty() {
		PureSystemCalculator calc = getTestedClass();
		Iso7064Reference reference = getReferenceSystem();
		Random random = new Random(9700L + reference.modulus);
		for (int sample = 0; sample < 300; sample++) {
			String code = reference.compute(randomInput(random, 1 + random.nextInt(60)));
			int position = random.nextInt(code.length());
			char original = code.charAt(position);
			char replacement = reference.checkCharacters.charAt(random.nextInt(reference.checkCharacters.length()));
			if (replacement == original) {
				replacement = reference.checkCharacters.charAt((reference.checkCharacters.indexOf(replacement) + 1)
					% reference.checkCharacters.length());
			}
			String mutated = code.substring(0, position) + replacement + code.substring(position + 1);
			assertFalse("Single substitution was accepted: " + mutated, isAccepted(calc, mutated));
		}
	}

	@Test
	public void testGeneratedTranspositionsMatchTheWeightCondition() {
		PureSystemCalculator calc = getTestedClass();
		Iso7064Reference reference = getReferenceSystem();
		Random random = new Random(66126L + reference.modulus);
		for (int sample = 0; sample < 300; sample++) {
			String data = randomInput(random, 2 + random.nextInt(80));
			String code = reference.compute(data);
			int first = random.nextInt(data.length());
			int second = random.nextInt(data.length() - 1);
			if (second >= first) second++;
			int firstExponent = code.length() - 1 - first;
			int secondExponent = code.length() - 1 - second;
			int valueDifference = reference.valueOfData(data.charAt(second))
				- reference.valueOfData(data.charAt(first));
			int weightDifference = Iso7064Reference.powMod(reference.radix, firstExponent, reference.modulus)
				- Iso7064Reference.powMod(reference.radix, secondExponent, reference.modulus);
			boolean expectedAccepted = (valueDifference * weightDifference) % reference.modulus == 0;

			String transposedData = swap(data, first, second);
			String transposedCode = transposedData + code.substring(data.length());
			assertEquals("The polynomial checksum condition for " + transposedCode,
				expectedAccepted, reference.compute(transposedData).equals(transposedCode));
			assertEquals("verify disagrees with the weight condition for " + transposedCode,
				expectedAccepted, isAccepted(calc, transposedCode));
		}
	}

	@Test
	public void testWeightPeriodIsTheMultiplicativeOrder() {
		Iso7064Reference reference = getReferenceSystem();
		assertEquals(1, Iso7064Reference.powMod(reference.radix, reference.weightPeriod, reference.modulus));
		for (int divisor = 1; divisor < reference.weightPeriod; divisor++) {
			if (reference.weightPeriod % divisor == 0) {
				assertFalse("Smaller divisor " + divisor + " is already a period",
					Iso7064Reference.powMod(reference.radix, divisor, reference.modulus) == 1);
			}
		}
	}

	@Test
	public void testTranspositionAtAFullWeightPeriodCanRemainValid() {
		PureSystemCalculator calc = getTestedClass();
		Iso7064Reference reference = getReferenceSystem();
		String data = reference.dataCharacters.charAt(0)
			+ repeat(reference.dataCharacters.charAt(0), reference.weightPeriod - 1)
			+ reference.dataCharacters.charAt(1);
		String code = reference.compute(data);
		String transposedData = swap(data, 0, data.length() - 1);
		String transposedCode = transposedData + code.substring(data.length());
		assertTrue("Equal weights make this endpoint transposition checksum-preserving",
			calc.verify(transposedCode));
	}

	@Test
	public void testSupplementaryCheckSymbolsAreNotData() {
		PureSystemCalculator calc = getTestedClass();
		Iso7064Reference reference = getReferenceSystem();
		if (reference.modulus == 11) {
			try {
				calc.compute("X");
				fail("X is supplementary check data, not numeric protected data");
			} catch (IllegalStateException expected) { }
		} else if (reference.modulus == 37) {
			try {
				calc.compute("*");
				fail("* is supplementary check data, not protected data");
			} catch (IllegalStateException expected) { }
		}
	}

	@Test
	public void testCompositeModulus1271WeightPeriodUsesLcmByCrt() {
		Iso7064Reference reference = getReferenceSystem();
		if (reference.modulus == 1271) {
			assertEquals(1271, 31 * 41);
			int orderModulo31 = multiplicativeOrder(36, 31);
			int orderModulo41 = multiplicativeOrder(36, 41);
			assertEquals(3, orderModulo31);
			assertEquals(20, orderModulo41);
			assertEquals(60, lcm(orderModulo31, orderModulo41));
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
		String alphabet = getReferenceSystem().checkCharacters;

		for (int position = 0; position < validCode.length(); position++) {
			for (int replacement = 0; replacement < alphabet.length(); replacement++) {
				char candidate = alphabet.charAt(replacement);
				if (candidate == validCode.charAt(position)) continue;

				String corrupted = validCode.substring(0, position) + candidate
					+ validCode.substring(position + 1);
				assertFalse("Accepted substitution at position " + position + ": " + corrupted,
					isAccepted(calc, corrupted));
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

	private int assertAllInputs(PureSystemCalculator calc, Iso7064Reference reference,
		String prefix, int targetLength) {
		if (prefix.length() == targetLength) {
			String expected = reference.compute(prefix);
			assertTrue("Reference formula should accept its generated code: " + expected,
				reference.isValid(expected));
			assertEquals("Independent result for " + prefix, expected, calc.compute(prefix));
			assertTrue("Reference code should verify: " + expected, calc.verify(expected));
			return 1;
		}
		int count = 0;
		String alphabet = getInputCharacters();
		for (int i = 0; i < alphabet.length(); i++) {
			count += assertAllInputs(calc, reference, prefix + alphabet.charAt(i), targetLength);
		}
		return count;
	}

	private String randomInput(Random random, int length) {
		String alphabet = getInputCharacters();
		StringBuilder input = new StringBuilder();
		for (int i = 0; i < length; i++) input.append(alphabet.charAt(random.nextInt(alphabet.length())));
		return input.toString();
	}

	private String swap(String input, int first, int second) {
		char[] chars = input.toCharArray();
		char temporary = chars[first];
		chars[first] = chars[second];
		chars[second] = temporary;
		return new String(chars);
	}

	private boolean isAccepted(PureSystemCalculator calc, String code) {
		try {
			return calc.verify(code);
		} catch (IllegalStateException invalidAlphabet) {
			return false;
		}
	}

	private int multiplicativeOrder(int base, int modulus) {
		int value = 1;
		for (int order = 1; order <= modulus; order++) {
			value = (value * base) % modulus;
			if (value == 1) return order;
		}
		throw new AssertionError("No multiplicative order found");
	}

	private int lcm(int first, int second) {
		return first * second / gcd(first, second);
	}

	private int gcd(int first, int second) {
		while (second != 0) {
			int remainder = first % second;
			first = second;
			second = remainder;
		}
		return first;
	}
}
