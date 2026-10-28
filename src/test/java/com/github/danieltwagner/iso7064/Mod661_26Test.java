package com.github.danieltwagner.iso7064;

import org.junit.Test;


public class Mod661_26Test extends GenericTest {

	@Override
	protected PureSystemCalculator getTestedClass() {
		return new Mod661_26();
	}
	
	@Override
	protected String getInputCharacters() {
		return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	}

	@Override
	protected Iso7064Reference getReferenceSystem() {
		return new Iso7064Reference(661, 26, "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ", 2, 330, false);
	}
	
	@Test
	public void testBasic() {
		PureSystemCalculator calc = getTestedClass();
		
		assertEquals("BAISDLAFKBM", calc.compute("BAISDLAFK"));
		
		assertTrue(calc.verify("BAISDLAFKBM"));
		assertFalse(calc.verify("BAISDLAFKBA"));
	}
}
