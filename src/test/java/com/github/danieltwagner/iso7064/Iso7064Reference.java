package com.github.danieltwagner.iso7064;

/** Independent polynomial-formula oracle used by the test suite. */
final class Iso7064Reference {
	final int modulus;
	final int radix;
	final String dataCharacters;
	final String checkCharacters;
	final int checkCharacterCount;
	final int weightPeriod;
	final boolean mod97_10;

	Iso7064Reference(int modulus, int radix, String dataCharacters,
		String checkCharacters, int checkCharacterCount, int weightPeriod,
		boolean mod97_10) {
		this.modulus = modulus;
		this.radix = radix;
		this.dataCharacters = dataCharacters;
		this.checkCharacters = checkCharacters;
		this.checkCharacterCount = checkCharacterCount;
		this.weightPeriod = weightPeriod;
		this.mod97_10 = mod97_10;
	}

	String compute(String data) {
		if (mod97_10) {
			int remainder = 0;
			for (int i = 0; i < data.length(); i++) {
				int value = dataCharacters.indexOf(Character.toUpperCase(data.charAt(i)));
				if (value < 0) throw new IllegalArgumentException("Invalid MOD 97-10 data");
				remainder = (remainder * 10 + value) % 97;
			}
			int checkDigits = 98 - ((remainder * 100) % 97);
			return data + (checkDigits < 10 ? "0" : "") + checkDigits;
		}

		int weightedData = 0;
		for (int i = 0; i < data.length(); i++) {
			int value = dataCharacters.indexOf(Character.toUpperCase(data.charAt(i)));
			if (value < 0) throw new IllegalArgumentException("Invalid protected data");
			int exponent = data.length() - 1 - i + checkCharacterCount;
			weightedData = (weightedData + value * powMod(radix, exponent, modulus)) % modulus;
		}

		int checkValue = (1 - weightedData + modulus) % modulus;
		if (checkCharacterCount == 1) {
			return data + checkCharacters.charAt(checkValue);
		}

		int first = checkValue / radix;
		int second = checkValue % radix;
		return data + checkCharacters.charAt(first) + checkCharacters.charAt(second);
	}

	boolean isValid(String protectedString) {
		if (protectedString == null || protectedString.length() <= checkCharacterCount) return false;
		int sum = 0;
		for (int i = 0; i < protectedString.length(); i++) {
			char character = Character.toUpperCase(protectedString.charAt(i));
			String alphabet = i < protectedString.length() - checkCharacterCount
				? dataCharacters : checkCharacters;
			int value = alphabet.indexOf(character);
			if (value < 0) return false;
			int exponent = protectedString.length() - 1 - i;
			sum = (sum + value * powMod(radix, exponent, modulus)) % modulus;
		}
		return sum == 1;
	}

	int valueOfData(char character) {
		return dataCharacters.indexOf(Character.toUpperCase(character));
	}

	static int powMod(int base, int exponent, int modulus) {
		int result = 1;
		for (int i = 0; i < exponent; i++) result = (result * base) % modulus;
		return result;
	}
}
