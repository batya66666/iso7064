package com.github.danieltwagner.iso7064;

import org.junit.Test;


public class Mod37_2Test extends GenericTest {

	@Override
	protected PureSystemCalculator getTestedClass() {
		return new Mod37_2();
	}
	
	@Override
	protected String getInputCharacters() {
		return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	}

	@Override
	protected Iso7064Reference getReferenceSystem() {
		return new Iso7064Reference(37, 2, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ*", 1, 36, false);
	}
	
	@Test
	public void testBasic() {
		PureSystemCalculator calc = getTestedClass();
		
		assertEquals("G123489654321Y", calc.compute("G123489654321"));
		
		assertTrue(calc.verify("G123489654321Y"));
		assertFalse(calc.verify("G123489654322Y"));
	}
}
