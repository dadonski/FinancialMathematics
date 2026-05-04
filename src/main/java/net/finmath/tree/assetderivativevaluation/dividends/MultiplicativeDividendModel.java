package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * A dividend model where dividends affect the stock price multiplicatively:
 * S_t = S_t^{nodiv} * D(t).
 *
 * This interface provides methods to compute cumulative and forward dividend factors.
 *
 * @author Edoardo Anfelli
 */
public interface MultiplicativeDividendModel extends DividendModel {

	/**
	 * Returns the cumulative multiplicative dividend factor up to the given time.
	 * For proportional dividends: D(t) = product_{t_k <= t} (1 - delta_k).
	 * For continuous yield: D(t) = exp(-q * t).
	 *
	 * @param time The evaluation time.
	 * @return The cumulative dividend factor.
	 */
	double getCumulativeDividendFactor(double time);

	/**
	 * Returns the forward multiplicative dividend factor between two times.
	 * D(time1, time2) = D(time2) / D(time1), i.e. the product of (1-delta_k)
	 * for dividends in (time1, time2].
	 *
	 * @param time1 Start time.
	 * @param time2 End time.
	 * @return The forward dividend factor over (time1, time2].
	 */
	double getForwardDividendFactor(double time1, double time2);
	
	/**
	 * Snaps dividend event times to the nearest grid point >= the original time.
	 * For discrete dividends, this maps each event time to Math.ceil(time / deltaT) * deltaT.
	 * For continuous models (e.g. ContinuousDividendYield) or NoDividends, this is a no-op.
	 *
	 * @param deltaT The uniform time step of the tree.
	 */
	default void snapToGrid(double deltaT) {
		// Default: do nothing (appropriate for continuous yield and no-dividends)
	}
}
