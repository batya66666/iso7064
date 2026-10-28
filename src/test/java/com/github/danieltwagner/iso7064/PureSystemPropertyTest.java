package com.github.danieltwagner.iso7064;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;

public class PureSystemPropertyTest {
	@Property(tries = 500, seed = "7064")
	public void computedCodesMatchTheIndependentOracleAndVerify(@ForAll("validInputs") SystemInput sample) {
		String expected = sample.system.reference.compute(sample.data);
		if (!sample.system.reference.isValid(expected)) {
			throw new AssertionError("Reference generated an invalid code: " + expected);
		}
		String actual = sample.system.calculator.compute(sample.data);
		if (!expected.equals(actual)) {
			throw new AssertionError("Reference mismatch for " + sample.system.name + " input " + sample.data
				+ ": expected " + expected + " but got " + actual);
		}
		if (!sample.system.calculator.verify(expected)) {
			throw new AssertionError("Generated code was rejected by " + sample.system.name + ": " + expected);
		}
	}

	@Property(tries = 500, seed = "9700")
	public void anySingleCharacterSubstitutionIsRejected(@ForAll("validInputs") SystemInput sample) {
		String code = sample.system.reference.compute(sample.data);
		int position = Math.floorMod(sample.data.length() * 7 + sample.data.charAt(0), code.length());
		char original = code.charAt(position);
		int replacementIndex = sample.system.reference.checkCharacters.indexOf(original);
		char replacement = sample.system.reference.checkCharacters.charAt(
			(replacementIndex + 1) % sample.system.reference.checkCharacters.length());
		String corrupted = code.substring(0, position) + replacement + code.substring(position + 1);
		if (isAccepted(sample.system.calculator, corrupted)) {
			throw new AssertionError("Single substitution was accepted by " + sample.system.name + ": " + corrupted);
		}
	}

	@Property(tries = 500, seed = "66126")
	public void dataTranspositionsAreAcceptedExactlyWhenTheWeightsCancel(
		@ForAll("transpositionInputs") TranspositionInput sample) {
		Iso7064Reference reference = sample.system.reference;
		String code = reference.compute(sample.data);
		int firstExponent = code.length() - 1 - sample.first;
		int secondExponent = code.length() - 1 - sample.second;
		int valueDifference = reference.valueOfData(sample.data.charAt(sample.second))
			- reference.valueOfData(sample.data.charAt(sample.first));
		int weightDifference = Iso7064Reference.powMod(reference.radix, firstExponent, reference.modulus)
			- Iso7064Reference.powMod(reference.radix, secondExponent, reference.modulus);
		boolean expectedAccepted = valueDifference * weightDifference % reference.modulus == 0;

		String transposedData = swap(sample.data, sample.first, sample.second);
		String transposedCode = transposedData + code.substring(sample.data.length());
		boolean oracleAccepted = reference.compute(transposedData).equals(transposedCode);
		boolean implementationAccepted = isAccepted(sample.system.calculator, transposedCode);
		if (oracleAccepted != expectedAccepted || implementationAccepted != expectedAccepted) {
			throw new AssertionError("Transposition condition mismatch for " + sample.system.name + ": "
				+ sample.data + " -> " + transposedData + ", expected acceptance=" + expectedAccepted
				+ ", oracle=" + oracleAccepted + ", implementation=" + implementationAccepted);
		}
	}

	@Provide
	Arbitrary<SystemInput> validInputs() {
		return Arbitraries.integers().between(0, systems().length - 1).flatMap(index -> {
			SystemCase system = systems()[index];
			return Arbitraries.strings().withChars(system.reference.dataCharacters.toCharArray())
				.ofMinLength(1).ofMaxLength(100)
				.map(data -> new SystemInput(system, data));
		});
	}

	@Provide
	Arbitrary<TranspositionInput> transpositionInputs() {
		return Arbitraries.integers().between(0, systems().length - 1).flatMap(index -> {
			SystemCase system = systems()[index];
			return Arbitraries.integers().between(2, 100).flatMap(length ->
				Arbitraries.strings().withChars(system.reference.dataCharacters.toCharArray()).ofLength(length)
					.flatMap(data -> Arbitraries.integers().between(0, length - 1).flatMap(first ->
						Arbitraries.integers().between(0, length - 2).map(other -> {
							int second = other >= first ? other + 1 : other;
							return new TranspositionInput(system, data, first, second);
						}))));
		});
	}

	private SystemCase[] systems() {
		return new SystemCase[] {
			new SystemCase("MOD 11-2", new Mod11_2(),
				new Iso7064Reference(11, 2, "0123456789", "0123456789X", 1, 10, false)),
			new SystemCase("MOD 37-2", new Mod37_2(),
				new Iso7064Reference(37, 2, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
					"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ*", 1, 36, false)),
			new SystemCase("MOD 97-10", new Mod97_10(),
				new Iso7064Reference(97, 10, "0123456789", "0123456789", 2, 96, true)),
			new SystemCase("MOD 661-26", new Mod661_26(),
				new Iso7064Reference(661, 26, "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
					"ABCDEFGHIJKLMNOPQRSTUVWXYZ", 2, 330, false)),
			new SystemCase("MOD 1271-36", new Mod1271_36(),
				new Iso7064Reference(1271, 36, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
					"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", 2, 60, false))
		};
	}

	private boolean isAccepted(PureSystemCalculator calculator, String code) {
		try {
			return calculator.verify(code);
		} catch (IllegalStateException invalidCharacter) {
			return false;
		}
	}

	private String swap(String input, int first, int second) {
		char[] characters = input.toCharArray();
		char temporary = characters[first];
		characters[first] = characters[second];
		characters[second] = temporary;
		return new String(characters);
	}

	private static final class SystemCase {
		final String name;
		final PureSystemCalculator calculator;
		final Iso7064Reference reference;

		SystemCase(String name, PureSystemCalculator calculator, Iso7064Reference reference) {
			this.name = name;
			this.calculator = calculator;
			this.reference = reference;
		}
	}

	private static final class SystemInput {
		final SystemCase system;
		final String data;

		SystemInput(SystemCase system, String data) {
			this.system = system;
			this.data = data;
		}
	}

	private static final class TranspositionInput {
		final SystemCase system;
		final String data;
		final int first;
		final int second;

		TranspositionInput(SystemCase system, String data, int first, int second) {
			this.system = system;
			this.data = data;
			this.first = first;
			this.second = second;
		}
	}
}
