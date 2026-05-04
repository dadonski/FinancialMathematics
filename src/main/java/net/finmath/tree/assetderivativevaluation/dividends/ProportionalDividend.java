package net.finmath.tree.assetderivativevaluation.dividends;

/**
 * A single proportional dividend: at time t_div the stock drops by a factor (1 - delta).
 * S_t = S_t^{nodiv} * (1 - delta) for t >= t_div.
 *
 * The tree remains recombining since this is a multiplicative adjustment.
 *
 * @author Edoardo Anfelli
 */
public class ProportionalDividend implements MultiplicativeDividendModel, DividendEvent {

	private double time;
	private final double delta;

	/**
	 * Creates a single proportional dividend event.
	 *
	 * @param time  The time at which the dividend is paid.
	 * @param delta The proportional amount in [0, 1), e.g. 0.02 for a 2% dividend.
	 */
	public ProportionalDividend(double time, double delta) {
		if (delta < 0.0 || delta >= 1.0) {
			System.out.print("Delta must be in [0, 1), got " + delta);
		}
		this.time = time;
		this.delta = delta;
	}

	@Override
	public double getTime() {
		return time;
	}

	@Override
	public double getDelta() {
		return delta;
	}

	@Override
	public double getCumulativeDividendFactor(double t) {
		if (t >= time) {
			return 1.0 - delta;
		}
		return 1.0;
	}

	@Override
	public double getForwardDividendFactor(double time1, double time2) {
		if (time1 < time && time <= time2) {
			return 1.0 - delta;
		}
		return 1.0;
	}
	
	@Override
	public void snapToGrid(double deltaT) {
		this.time = Math.ceil(time / deltaT) * deltaT;
	}
}
