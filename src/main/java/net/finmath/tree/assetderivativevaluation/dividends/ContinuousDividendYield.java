package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * Continuous dividend yield model: D(t) = exp(-q * t) where q is a constant yield.
 *
 * WARNING: This implementation assumes a constant dividend yield q.
 * If q were time-dependent, the transition probabilities of the tree model
 * would need to be recomputed at each time step (time-inhomogeneous model).
 *
 * For CRR: p_u = (exp((r-q)*dt) - d) / (u - d)
 * For JR:  u = exp((r - q - 0.5*sigma^2)*dt + sigma*sqrt(dt))
 * For Boyle: R = exp((r-q)*dt)
 *
 * @author Edoardo Anfelli
 */
public class ContinuousDividendYield implements MultiplicativeDividendModel {

	private final double yield;

	/**
	 * Creates a continuous dividend yield model.
	 *
	 * @param yield The constant continuous dividend yield q >= 0.
	 */
	public ContinuousDividendYield(double yield) {
		if (yield < 0.0) {
			throw new IllegalArgumentException("Dividend yield must be non-negative, got " + yield);
		}
		this.yield = yield;
	}

	/**
	 * Returns the constant dividend yield q.
	 *
	 * @return The dividend yield.
	 */
	public double getYield() {
		return yield;
	}

	@Override
	public double getCumulativeDividendFactor(double time) {
		return Math.exp(-yield * time);
	}

	@Override
	public double getForwardDividendFactor(double time1, double time2) {
		return Math.exp(-yield * (time2 - time1));
	}
}
