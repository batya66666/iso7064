package com.github.danieltwagner.iso7064;

import org.junit.Test;


public class Mod97_10Test extends GenericTest {

	@Override
	protected PureSystemCalculator getTestedClass() {
		return new Mod97_10();
	}
	
	@Override
	protected String getInputCharacters() {
		return "0123456789";
	}

	@Override
	protected Iso7064Reference getReferenceSystem() {
		return new Iso7064Reference(97, 10, "0123456789", "0123456789", 2, 96, true);
	}
	
	@Test
	public void testBasic() {
		PureSystemCalculator calc = getTestedClass();

		assertEquals("79444", calc.compute("794"));
		assertEquals("10757107", calc.compute("107571"));
		
		assertTrue(calc.verify("79444"));
		assertTrue(calc.verify("10757107"));
		assertFalse(calc.verify("79445"));
		assertFalse(calc.verify("10757108"));
	}

	@Test
	public void testZeroRemainderUses98AsCheckDigits() {
		PureSystemCalculator calc = getTestedClass();
		assertEquals("9798", calc.compute("97"));
	}

	@Test
	public void testVerifiesCanonicalZeroRemainderCode() {
		PureSystemCalculator calc = getTestedClass();
		assertTrue(calc.verify("9798"));
	}

	@Test
	public void testRejectsCharactersAboveTheDigitRange() {
		try {
			getTestedClass().compute("A");
			fail("MOD 97-10 accepts numeric input only");
		} catch (IllegalStateException expected) {
			// The character sorts after '9' and is not a decimal digit.
		}
	}
}
