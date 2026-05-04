package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * A dividend model representing no dividends.
 * The cumulative factor is always 1.0.
 *
 * @author Edoardo Anfelli
 */
public class NoDividends implements MultiplicativeDividendModel {

	@Override
	public double getCumulativeDividendFactor(double time) {
		return 1.0;
	}

	@Override
	public double getForwardDividendFactor(double time1, double time2) {
		return 1.0;
	}
}
