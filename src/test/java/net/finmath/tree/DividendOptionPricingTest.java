package net.finmath.tree;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.modelling.products.CallOrPut;
import net.finmath.tree.assetderivativevaluation.dividends.ContinuousDividendYield;
import net.finmath.tree.assetderivativevaluation.dividends.DiscreteProportionalDividends;
import net.finmath.tree.assetderivativevaluation.dividends.ProportionalDividend;
import net.finmath.tree.assetderivativevaluation.models.BoyleTrinomial;
import net.finmath.tree.assetderivativevaluation.models.CoxRossRubinsteinModel;
import net.finmath.tree.assetderivativevaluation.models.JarrowRuddModel;
import net.finmath.tree.assetderivativevaluation.products.AmericanNonPathDependent;
import net.finmath.tree.assetderivativevaluation.products.EuropeanOption;

/**
 * Exercise 3: Tests the impact of dividends on European and American option prices.
 *
 * Key check: American call == European call for non-dividend stocks,
 * but American call > European call when dividends are large enough.
 *
 */
public class DividendOptionPricingTest {

	@Test
	public void testAmericanCallDivergesWithDividend_CRR() {
		double spot = 100.0;
		double rate = 0.05;
		double vol = 0.20;
		double maturity = 2.0;
		int steps = 200;
		double strike = 100.0;

		// No dividends: American call == European call
		CoxRossRubinsteinModel crrNodiv = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps);
		EuropeanOption euCall = new EuropeanOption(maturity, strike, CallOrPut.CALL);
		AmericanNonPathDependent usCall = new AmericanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));

		double euNodiv = euCall.getValue(crrNodiv);
		double usNodiv = usCall.getValue(0.0, crrNodiv).getAverage();

		System.out.println("CRR No Dividends");
		System.out.println("European Call: " + euNodiv);
		System.out.println("American Call: " + usNodiv);
		System.out.println("Difference:    " + Math.abs(usNodiv - euNodiv));

		// Without dividends they should be (nearly) equal
		Assert.assertEquals("No-div: American call should equal European call", euNodiv, usNodiv, 1e-6);

		// Large proportional dividend: American call should be strictly > European call
		ProportionalDividend bigDiv = new ProportionalDividend(1.0, 0.10); // 10% at t=1
		CoxRossRubinsteinModel crrDiv = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps, bigDiv);

		double euDiv = euCall.getValue(crrDiv);
		double usDiv = usCall.getValue(0.0, crrDiv).getAverage();

		System.out.println("\n=== CRR With 10% Proportional Dividend at t=1 ===");
		System.out.println("European Call: " + euDiv);
		System.out.println("American Call: " + usDiv);
		System.out.println("Difference:    " + (usDiv - euDiv));

		Assert.assertTrue("With dividend: American call should be >= European call", usDiv + 1e-10 >= euDiv);
		// The dividend should make early exercise optimal in some states
		Assert.assertTrue("With large dividend: American call should be strictly > European call",
				usDiv - euDiv > 0.01);
	}

	@Test
	public void testDiscreteProportionalDividends_CRR() {
		double spot = 100.0;
		double rate = 0.05;
		double vol = 0.20;
		double maturity = 3.0;
		int steps = 300;
		double strike = 100.0;

		// Two dividends: 5% at t=1 and 5% at t=2
		DiscreteProportionalDividends divs = new DiscreteProportionalDividends(
				new double[]{1.0, 2.0}, new double[]{0.05, 0.05});

		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps, divs);

		EuropeanOption euCall = new EuropeanOption(maturity, strike, CallOrPut.CALL);
		EuropeanOption euPut  = new EuropeanOption(maturity, strike, CallOrPut.PUT);
		AmericanNonPathDependent usCall = new AmericanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));
		AmericanNonPathDependent usPut  = new AmericanNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));

		double euCallPrice = euCall.getValue(crr);
		double euPutPrice  = euPut.getValue(crr);
		double usCallPrice = usCall.getValue(0.0, crr).getAverage();
		double usPutPrice  = usPut.getValue(0.0, crr).getAverage();

		System.out.println("\n=== CRR with Discrete Proportional Dividends (5% at t=1, 5% at t=2) ===");
		System.out.println("European Call: " + euCallPrice + " | American Call: " + usCallPrice);
		System.out.println("European Put:  " + euPutPrice  + " | American Put:  " + usPutPrice);

		Assert.assertTrue("American call >= European call", usCallPrice + 1e-10 >= euCallPrice);
		Assert.assertTrue("American put >= European put",   usPutPrice  + 1e-10 >= euPutPrice);
	}

	@Test
	public void testContinuousDividendYield_AllModels() {
		double spot = 100.0;
		double rate = 0.05;
		double vol = 0.20;
		double maturity = 1.0;
		int steps = 200;
		double strike = 100.0;
		double divYield = 0.03;

		ContinuousDividendYield contDiv = new ContinuousDividendYield(divYield);

		CoxRossRubinsteinModel crr = new CoxRossRubinsteinModel(spot, rate, vol, maturity, steps, contDiv);
		JarrowRuddModel jr = new JarrowRuddModel(spot, rate, vol, maturity, steps, contDiv);
		BoyleTrinomial tri = new BoyleTrinomial(spot, rate, vol, maturity, steps, contDiv);

		EuropeanOption euCall = new EuropeanOption(maturity, strike, CallOrPut.CALL);
		AmericanNonPathDependent usCall = new AmericanNonPathDependent(maturity, s -> Math.max(s - strike, 0.0));

		double euCRR = euCall.getValue(crr);
		double euJR  = euCall.getValue(0.0, jr).getAverage();
		double euTRI = euCall.getValue(0.0, tri).getAverage();

		double usCRR = usCall.getValue(0.0, crr).getAverage();
		double usJR  = usCall.getValue(0.0, jr).getAverage();
		double usTRI = usCall.getValue(0.0, tri).getAverage();

		System.out.println("\n=== Continuous Dividend Yield q=3% ===");
		System.out.println("European Call - CRR: " + euCRR + " | JR: " + euJR + " | TRI: " + euTRI);
		System.out.println("American Call - CRR: " + usCRR + " | JR: " + usJR + " | TRI: " + usTRI);

		double tolBin = 0.05;  // CRR vs JR: both binomial, tight convergence
		double tolTri = 0.25;  // Trinomial converges more slowly at 200 steps
		Assert.assertEquals("EU Call: CRR vs JR",  euCRR, euJR,  tolBin);
		Assert.assertEquals("EU Call: CRR vs TRI", euCRR, euTRI, tolTri);
		Assert.assertTrue("American call >= European call (CRR)", usCRR + 1e-10 >= euCRR);
		Assert.assertTrue("American call >= European call (JR)",  usJR  + 1e-10 >= euJR);
		Assert.assertTrue("American call >= European call (TRI)", usTRI + 1e-10 >= euTRI);
	}

	@Test
	public void testDividendModelsConsistency_JR_Boyle() {
		double spot = 100.0;
		double rate = 0.05;
		double vol = 0.25;
		double maturity = 2.0;
		int steps = 200;
		double strike = 110.0;

		ProportionalDividend div = new ProportionalDividend(1.0, 0.05);

		JarrowRuddModel jr = new JarrowRuddModel(spot, rate, vol, maturity, steps, div);
		BoyleTrinomial tri = new BoyleTrinomial(spot, rate, vol, maturity, steps, div);

		EuropeanOption euPut = new EuropeanOption(maturity, strike, CallOrPut.PUT);
		AmericanNonPathDependent usPut = new AmericanNonPathDependent(maturity, s -> Math.max(strike - s, 0.0));

		double euJR  = euPut.getValue(0.0, jr).getAverage();
		double euTRI = euPut.getValue(0.0, tri).getAverage();
		double usJR  = usPut.getValue(0.0, jr).getAverage();
		double usTRI = usPut.getValue(0.0, tri).getAverage();

		System.out.println("\n=== JR & Boyle with 5% Proportional Dividend at t=1 ===");
		System.out.println("European Put - JR: " + euJR + " | TRI: " + euTRI);
		System.out.println("American Put - JR: " + usJR + " | TRI: " + usTRI);

		double tol = 0.10;
		Assert.assertEquals("EU Put: JR vs TRI", euJR, euTRI, tol);
		Assert.assertTrue("American put >= European put (JR)",  usJR  + 1e-10 >= euJR);
		Assert.assertTrue("American put >= European put (TRI)", usTRI + 1e-10 >= euTRI);
	}
}
