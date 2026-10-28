# Test methodology and results

## Independent reference and exhaustive short strings

`Iso7064Reference` computes expected check characters from the ISO polynomial formula, independently of the production left-to-right recurrence. MOD 97-10 uses its dedicated rule: append two zeroes, calculate the remainder modulo 97, and subtract it from 98. This distinction matters when the remainder is zero.

For each implementation, the JUnit tests enumerate every valid data string of lengths 1, 2, and 3, compare `compute` with the reference, and verify the reference code:

| System | Data alphabet size | Length 1 | Length 2 | Length 3 | Total inputs |
|---|---:|---:|---:|---:|---:|
| MOD 11-2 | 10 | 10 | 100 | 1,000 | 1,110 |
| MOD 37-2 | 36 | 36 | 1,296 | 46,656 | 47,988 |
| MOD 97-10 | 10 | 10 | 100 | 1,000 | 1,110 |
| MOD 661-26 | 26 | 26 | 676 | 17,576 | 18,278 |
| MOD 1271-36 | 36 | 36 | 1,296 | 46,656 | 47,988 |
| **Total** | | | | | **116,474** |

The MOD 11-2 data alphabet is `0-9`; `X` is a supplementary check character. The MOD 37-2 data alphabet is `0-9A-Z`; `*` is supplementary. The implementation now distinguishes the protected-data alphabet from the alphabet used to encode check characters.

## MOD 97-10 zero-remainder case

For input `97`, the standard procedure gives:

```text
9700 mod 97 = 0
98 - 0 = 98
```

The expected complete code is `9798`. The previous implementation returned `9701`: it was internally consistent because its `verify` recomputed the same result, but it did not follow the MOD 97-10 generation procedure. The MOD 97-10 implementation now uses the standard `98 - remainder` rule, and separate tests require `compute("97") == "9798"` and `verify("9798") == true`.

The MOD 97-10 procedure is described in ISO/IEC 7064:2003, which ISO lists as the current edition, and is reproduced for a U.S. federal identifier in the [Consumer Financial Protection Bureau's Appendix C to Regulation C (12 CFR Part 1003)](https://www.consumerfinance.gov/rules-policy/regulations/1003/c/). The standard's pure-system equation is also available in this [national standards body preview](https://preview.sist.si/sist-preview/31531/4ca4e9783a4340acb22586379dc59284/ISO-IEC-7064-2003.pdf).

## Generated properties

`PureSystemPropertyTest` uses jqwik with randomized generators. Each property runs 500 cases and selects among the five systems:

1. A generated legal input produces the same code as the independent reference, and the code verifies.
2. A single character substitution in generated protected codes is rejected.
3. A data transposition is accepted exactly when its weighted checksum change is zero modulo the system modulus.

The JUnit tests also run fixed-seed generated inputs for each system, test every single-symbol replacement for a representative code, and test endpoint swaps separated by one full weight period.

## Weight periods and transposition condition

For a protected string with character values `a_i` and right-to-left exponents `k_i`, its weighted sum is

```text
S = Σ a_i · r^k_i (mod M)
```

Swapping different values `a_i` and `a_j` changes the sum by

```text
ΔS = (a_j - a_i) · (r^k_i - r^k_j) (mod M)
```

The transposition preserves the checksum exactly when `ΔS ≡ 0 (mod M)`. Equal weights are sufficient, but for a composite modulus they are not necessary: a non-zero symbol difference can cancel a non-zero weight difference modulo the composite modulus.

| System | Modulus | Radix | Weight period `ord_M(r)` |
|---|---:|---:|---:|
| MOD 11-2 | 11 | 2 | 10 |
| MOD 37-2 | 37 | 2 | 36 |
| MOD 97-10 | 97 | 10 | 96 |
| MOD 661-26 | 661 | 26 | 330 |
| MOD 1271-36 | 1,271 | 36 | 60 |

For MOD 1271-36, `1271 = 31 · 41`. The orders are `ord_31(36) = 3` and `ord_41(36) = 20`; the Chinese remainder theorem gives `ord_1271(36) = lcm(3, 20) = 60`. The tests verify these component orders and the resulting period.

The composite case also has checksum-preserving transpositions with **unequal** weights. In the fixed MOD 1271-36 example, `0` and `V` have values 0 and 31. The selected endpoint weights differ, but their difference times 31 is divisible by `31 · 41`; the swapped code therefore remains valid. The exact product condition, rather than a blanket claim about transpositions, is what the property tests check.

## Boundaries and malformed input

The suite checks too-short protected codes, forbidden punctuation, invalid data characters, invalid check characters, case normalization, repeated-zero data, maximum-alphabet data, and strings of length 256. MOD 11-2 rejects `X` in data, and MOD 37-2 rejects `*` in data.

`null` and formatted inputs containing spaces or separators are not included in these tests yet. Their API behavior needs an explicit policy before it can be asserted: the current API documents rejection of characters outside its alphabet, while ISO/IEC 7064 describes embedded spaces and special characters as ignored.

## Coverage and mutation measurements

Baseline measurements were made at the original repository commit `47cb4d0` with its original tests, using the same JDK and tool versions as the final run. For the baseline, JaCoCo and PIT were temporarily enabled in the Maven configuration and the compiler target was set to Java 8, because the installed JDK does not support the original Java 6 target. Production code and tests were not changed.

| Measurement | Baseline | After |
|---|---:|---:|
| JaCoCo instruction coverage, all production classes | 181 / 200 (90.5%) | 221 / 221 (100%) |
| JaCoCo branch coverage | 10 / 12 (83.3%) | 12 / 12 (100%) |
| PIT mutants killed | 44 / 45 (97.8%) | 52 / 52 (100%) |
| PIT line coverage | 48 / 48 (100%) | 54 / 54 (100%) |

At baseline PIT reported one surviving mutation: changing the `verify` short-input comparison from `<=` to `<`. The too-short input test added in this work kills that mutation. The final PIT run reported no surviving or uncovered mutants.

The PIT populations differ (45 baseline mutants versus 52 after the production fixes), so the score change is not a controlled same-mutant comparison. The reports still show the concrete baseline survivor being killed and no surviving mutants in the final implementation.

## Reproducing the measurements

```text
mvn clean verify
mvn org.pitest:pitest-maven:mutationCoverage
```

JaCoCo writes its report to `target/site/jacoco/index.html`; PIT writes its report to `target/pit-reports/index.html`.
