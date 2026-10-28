package com.github.danieltwagner.iso7064;

import org.junit.Test;


public class Mod1271_36Test extends GenericTest {

	@Override
	protected PureSystemCalculator getTestedClass() {
		return new Mod1271_36();
	}
	
	@Override
	protected String getInputCharacters() {
		return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	}

	@Override
	protected Iso7064Reference getReferenceSystem() {
		return new Iso7064Reference(1271, 36, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", 2, 60, false);
	}
	
	@Test
	public void testBasic() {
		PureSystemCalculator calc = getTestedClass();
		
		assertEquals("ISO793W", calc.compute("ISO79"));
		
		assertTrue(calc.verify("ISO793W"));
		assertFalse(calc.verify("ISO793X"));
	}

	@Test
	public void testCompositeModulusCanCancelUnequalWeights() {
		PureSystemCalculator calc = getTestedClass();
		Iso7064Reference reference = getReferenceSystem();
		String data = "0" + repeat('0', 19) + "V";
		String code = reference.compute(data);
		String transposedData = "V" + repeat('0', 19) + "0";
		int firstWeight = Iso7064Reference.powMod(36, code.length() - 1, 1271);
		int lastWeight = Iso7064Reference.powMod(36, 2, 1271);
		int weightDifference = firstWeight - lastWeight;

		assertFalse("The endpoint weights themselves are not equal", weightDifference % 1271 == 0);
		assertEquals("The symbol difference 31 cancels the weight difference modulo 31*41",
			0, (31 * weightDifference) % 1271);
		assertTrue(calc.verify(transposedData + code.substring(data.length())));
	}

	private String repeat(char character, int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++) result.append(character);
		return result.toString();
	}
}
