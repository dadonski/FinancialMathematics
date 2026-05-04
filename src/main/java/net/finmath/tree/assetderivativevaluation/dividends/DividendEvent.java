package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * Represents a single dividend payment event.
 * Each event has a time at which the dividend is paid and a proportional amount (delta).
 *
 * @author Edoardo Anfelli
 */
public interface DividendEvent {

	/**
	 * Returns the time at which the dividend is paid.
	 *
	 * @return The dividend payment time.
	 */
	double getTime();

	/**
	 * Returns the proportional dividend amount delta, where the stock
	 * is multiplied by (1 - delta) at dividend time.
	 *
	 * @return The proportional dividend amount in [0, 1).
	 */
	double getDelta();
}
