package com.github.danieltwagner.iso7064;

public abstract class PureSystemCalculator {
	
    protected abstract int getModulus();
    protected abstract int getRadix();
    protected abstract String getCharacterSet();
    protected abstract boolean isDoubleCheckDigit();

    protected String getInputCharacterSet() {
        return getCharacterSet();
    }

    /** Returns the check value; MOD 97-10 overrides this to use 98 - remainder. */
    protected int getCheckValue(int valueAfterData, int modulus) {
        return (modulus - valueAfterData + 1) % modulus;
    }
    
    /**
     * Verify if the given input is a protected code with valid check characters.
     * @param input The input string that should be checked.
     * @return Whether or not the input was successfully verified. False if the code was parsed but did not verify. 
     * @throws IllegalStateException Raised if an illegal character is encountered.
     * @throws IllegalArgumentException Raised if the input string is too short.
     */
    public boolean verify(String input) throws IllegalStateException, IllegalArgumentException {
    	input = input.toUpperCase();
    	int numDigits = isDoubleCheckDigit() ? 2 : 1;
    	if(input.length() <= numDigits) throw new IllegalArgumentException("Input string too short!");
    	
    	String dataOnly = input.substring(0, input.length() - numDigits);
    	return input.equals(compute(dataOnly));
    }
    
    /**
     * Compute check characters for the given input
     * @param input The input string that should be appended with a check digit.
     * @return The input string with the check digit appended.
     * @throws IllegalStateException Raised if an illegal character is encountered.
     */
    public String compute(String input) throws IllegalStateException {
        input = input.toUpperCase();
        int m = getModulus();
        int r = getRadix();
        String chars = getCharacterSet();
        String inputChars = getInputCharacterSet();
        boolean doubleDigit = isDoubleCheckDigit();

        // Process the input characters from left to right.
        int p = 0;
        for (int i = 0; i < input.length(); i++) {
            int val = inputChars.indexOf(input.charAt(i));
            if (val < 0) throw new IllegalStateException("Found illegal character " + input.charAt(i));

            p = ((p + val) * r) % m;
        }

        // For two check characters, perform one additional pass with val = 0.
        if (doubleDigit) p = (p * r) % m;

        int checksum = getCheckValue(p, m);
        if (doubleDigit) {
            int second = checksum % r;
            int first = (checksum - second) / r;
            return input + chars.charAt(first) + chars.charAt(second);
        }
        return input + chars.charAt(checksum);
    }
    
}
