package net.finmath.tree;

import org.junit.Assert;
import org.junit.Test;

import it.univr.fima.barrieroptionsformulas.BarrierOptions;
import net.finmath.modelling.products.BarrierType;
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;
import net.finmath.tree.assetderivativevaluation.models.JarrowRuddModel;
import net.finmath.tree.assetderivativevaluation.models.BoyleTrinomial;
import net.finmath.tree.assetderivativevaluation.products.BarrierOption;

/**
 * Exercise 5: Compares barrier option tree prices against Black-Scholes closed-form formulas.
 *
 * Uses the class {@code it.univr.fima.barrieroptionsformulas.BarrierOptions}
 * for the analytical reference values. 
 */
public class BarrierOptionTreeTest {

	private static final double SPOT = 100.0;
	private static final double RATE = 0.05;
	private static final double VOL = 0.20;
	private static final double MATURITY = 1.0;
	private static final double STRIKE = 100.0;
	private static final double REBATE = 0.0;
	private static final double DIV_YIELD = 0.0;
	private static final int STEPS = 500;

	@Test
	public void testDownOutCall() {
		double barrier = 90.0;
		testBarrierOption(STRIKE, barrier, BarrierType.DOWN_OUT, true, "Down-Out Call");
	}

	@Test
	public void testUpOutCall() {
		double barrier = 120.0;
		testBarrierOption(STRIKE, barrier, BarrierType.UP_OUT, true, "Up-Out Call");
	}

	@Test
	public void testDownOutPut() {
		double barrier = 90.0;
		testBarrierOption(STRIKE, barrier, BarrierType.DOWN_OUT, false, "Down-Out Put");
	}

	@Test
	public void testUpOutPut() {
		double barrier = 120.0;
		testBarrierOption(STRIKE, barrier, BarrierType.UP_OUT, false, "Up-Out Put");
	}

	@Test
	public void testDownInCall() {
		double barrier = 90.0;
		testBarrierOption(STRIKE, barrier, BarrierType.DOWN_IN, true, "Down-In Call");
	}

	@Test
	public void testUpInCall() {
		double barrier = 120.0;
		testBarrierOption(STRIKE, barrier, BarrierType.UP_IN, true, "Up-In Call");
	}

	@Test
	public void testDownInPut() {
		double barrier = 90.0;
		testBarrierOption(STRIKE, barrier, BarrierType.DOWN_IN, false, "Down-In Put");
	}

	@Test
	public void testUpInPut() {
		double barrier = 120.0;
		testBarrierOption(STRIKE, barrier, BarrierType.UP_IN, false, "Up-In Put");
	}

	/**
	 * Tests a barrier option by comparing tree prices (CRR, JR, Trinomial)
	 * against the Black-Scholes closed-form value.
	 */
	private void testBarrierOption(double strike, double barrier, BarrierType type, boolean isCall, String label) {

		// Analytical value from closed-form formulas
		BarrierOptions.BarrierType cfType = mapBarrierType(type);
		double analytical = BarrierOptions.blackScholesBarrierOptionValue(
				SPOT, RATE, DIV_YIELD, VOL, MATURITY, strike, isCall, REBATE, barrier, cfType);

		// Tree models
		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(SPOT, RATE, VOL, MATURITY, STEPS);
		JarrowRuddModel jr = new JarrowRuddModel(SPOT, RATE, VOL, MATURITY, STEPS);
		BoyleTrinomial tri = new BoyleTrinomial(SPOT, RATE, VOL, MATURITY, STEPS);

		// Barrier option product
		BarrierOption barrierOpt = new BarrierOption(MATURITY, strike, barrier, type, isCall, REBATE);

		double treeCRR = barrierOpt.getValue(crr);
		double treeJR  = barrierOpt.getValue(0.0, jr).getAverage();
		double treeTRI = barrierOpt.getValue(0.0, tri).getAverage();

		System.out.println("=== " + label + " ===");
		System.out.println("  Analytical: " + analytical);
		System.out.println("  CRR tree:   " + treeCRR + " (diff: " + Math.abs(treeCRR - analytical) + ")");
		System.out.println("  JR tree:    " + treeJR  + " (diff: " + Math.abs(treeJR  - analytical) + ")");
		System.out.println("  TRI tree:   " + treeTRI + " (diff: " + Math.abs(treeTRI - analytical) + ")");

		// Barrier options converge more slowly; allow wider tolerance
		double tol = 1.0;
		Assert.assertEquals(label + " CRR vs Analytical", analytical, treeCRR, tol);
		Assert.assertEquals(label + " JR vs Analytical",  analytical, treeJR,  tol);
		Assert.assertEquals(label + " TRI vs Analytical", analytical, treeTRI, tol);
	}

	/**
	 * Maps our BarrierType enum to the one used by the closed-form formula class.
	 */
	private BarrierOptions.BarrierType mapBarrierType(BarrierType type) {
		switch (type) {
			case DOWN_IN:  return BarrierOptions.BarrierType.DOWN_IN;
			case UP_IN:    return BarrierOptions.BarrierType.UP_IN;
			case DOWN_OUT: return BarrierOptions.BarrierType.DOWN_OUT;
			case UP_OUT:   return BarrierOptions.BarrierType.UP_OUT;
			default: throw new IllegalArgumentException("Unknown barrier type: " + type);
		}
	}

	@Test
	public void testInOutParity() {
		// V_IN + V_OUT = V_European for all barrier types
		double barrier = 90.0;
		double strike = 100.0;

		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(SPOT, RATE, VOL, MATURITY, STEPS);

		// Down barrier, Call
		BarrierOption downInCall  = new BarrierOption(MATURITY, strike, barrier, BarrierType.DOWN_IN, true);
		BarrierOption downOutCall = new BarrierOption(MATURITY, strike, barrier, BarrierType.DOWN_OUT, true);

		double inPrice  = downInCall.getValue(crr);
		double outPrice = downOutCall.getValue(crr);

		// European call for reference
		net.finmath.tree.assetderivativevaluation.products.EuropeanOption euCall =
				new net.finmath.tree.assetderivativevaluation.products.EuropeanOption(MATURITY, strike, net.finmath.modelling.products.CallOrPut.CALL);
		double euPrice = euCall.getValue(crr);

		System.out.println("\n=== IN-OUT Parity Check (Down Call, barrier=90) ===");
		System.out.println("  European:     " + euPrice);
		System.out.println("  Down-In:      " + inPrice);
		System.out.println("  Down-Out:     " + outPrice);
		System.out.println("  In + Out:     " + (inPrice + outPrice));
		System.out.println("  Diff:         " + Math.abs(euPrice - (inPrice + outPrice)));

		Assert.assertEquals("IN + OUT should equal European", euPrice, inPrice + outPrice, 1e-8);
	}
}
