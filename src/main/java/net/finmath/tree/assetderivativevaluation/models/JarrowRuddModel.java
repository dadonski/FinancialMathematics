package net.finmath.tree.assetderivativevaluation.models;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.assetderivativevaluation.AbstractRecombiningTreeModel;
import net.finmath.tree.assetderivativevaluation.dividends.ContinuousDividendYield;
import net.finmath.tree.assetderivativevaluation.dividends.MultiplicativeDividendModel;
import net.finmath.tree.assetderivativevaluation.dividends.NoDividends;

/**
 * This class represents the approximation of a Black-Scholes model via the Jarrow-Rudd model.
 *
 * Supports multiplicative dividends:
 * - Proportional/Discrete proportional: dividends multiply node values, probabilities unchanged (p=0.5).
 * - Continuous dividend yield q: u = exp((r-q-0.5*sigma^2)*dt + sigma*sqrt(dt)),
 *   d = exp((r-q-0.5*sigma^2)*dt - sigma*sqrt(dt)), p = 0.5.
 *
 * @author Andrea Mazzon
 */
public class JarrowRuddModel extends AbstractRecombiningTreeModel {

	/** Dividend model */
	private final MultiplicativeDividendModel dividendModel;

	/** Effective drift used in u/d computation (accounts for dividend yield) */
	private final double effectiveDrift;

	/**
	 * Constructs a Jarrow-Rudd model without dividends.
	 */
	public JarrowRuddModel(double spotPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		this(spotPrice, riskFreeRate, volatility, lastTime, timeStep, new NoDividends());
	}

	/**
	 * Constructs a Jarrow-Rudd model without dividends.
	 */
	public JarrowRuddModel(double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		this(spotPrice, riskFreeRate, volatility, lastTime, numberOfTimes, new NoDividends());
	}

	/**
	 * Constructs a Jarrow-Rudd model with a dividend model.
	 *
	 * For continuous dividend yield q:
	 *   u = exp((r - q - 0.5*sigma^2)*dt + sigma*sqrt(dt))
	 *   d = exp((r - q - 0.5*sigma^2)*dt - sigma*sqrt(dt))
	 * For proportional dividends: u/d unchanged, node values adjusted by D(t).
	 */
	public JarrowRuddModel(double spotPrice, double riskFreeRate, double volatility, double lastTime, double timeStep, MultiplicativeDividendModel dividendModel) {
		super(spotPrice, riskFreeRate, volatility, lastTime, timeStep);
		this.dividendModel = dividendModel;
		this.dividendModel.snapToGrid(getTimeStep());

		if (dividendModel instanceof ContinuousDividendYield) {
			double qDiv = ((ContinuousDividendYield) dividendModel).getYield();
			this.effectiveDrift = riskFreeRate - qDiv;
		} else {
			this.effectiveDrift = riskFreeRate;
		}
	}

	/**
	 * Constructs a Jarrow-Rudd model with a dividend model (numberOfTimes variant).
	 */
	public JarrowRuddModel(double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes, MultiplicativeDividendModel dividendModel) {
		super(spotPrice, riskFreeRate, volatility, lastTime, numberOfTimes);
		this.dividendModel = dividendModel;
		this.dividendModel.snapToGrid(getTimeStep());

		if (dividendModel instanceof ContinuousDividendYield) {
			double qDiv = ((ContinuousDividendYield) dividendModel).getYield();
			this.effectiveDrift = riskFreeRate - qDiv;
		} else {
			this.effectiveDrift = riskFreeRate;
		}
	}

	@Override
	public int statesAt(int k) {
		return k + 1;
	}

	/**
	 * Build S_k[i] = S0 * u^(k-i) * d^i * D(t_k).
	 * u and d use the effective drift (r or r-q for continuous yield).
	 */
	@Override
	protected RandomVariable buildSpotLevel(int k) {
		double s0    = getInitialPrice();
		double dt    = getTimeStep();
		double sigma = getVolatility();

		double muStar = (effectiveDrift - 0.5 * sigma * sigma) * dt;
		double nu     = sigma * Math.sqrt(dt);

		double u = Math.exp(muStar + nu);
		double d = Math.exp(muStar - nu);

		double time = k * dt;
		double divFactor = dividendModel.getCumulativeDividendFactor(time);

		double[] level = new double[k + 1];
		for (int i = 0; i <= k; i++) {
			int ups   = k - i;
			int downs = i;
			level[i] = s0 * Math.pow(u, ups) * Math.pow(d, downs) * divFactor;
		}
		return new RandomVariableFromDoubleArray(time, level);
	}

	/**
	 * Discounted conditional expectation: V_k[i] = df * (0.5 * V_{k+1}[i] + 0.5 * V_{k+1}[i+1]).
	 */
	@Override
	protected RandomVariable conditionalExpectation(RandomVariable vNext, int k) {
		final double r = getRiskFreeRate();
		final double dt = getTimeStep();

		final double disc = Math.exp(-r * dt);
		final double p = 0.5;

		final double[] next = vNext.getRealizations();
		final int expected = k + 2;
		if (next == null || next.length != expected) {
			throw new IllegalArgumentException("vNext length " + (next == null ? "null" : next.length)
					+ " != expected " + expected + " for time index k=" + k);
		}

		final int nHere = statesAt(k);
		final double[] res = new double[nHere];
		for (int i = 0; i < nHere; i++) {
			res[i] = disc * (p * next[i] + (1.0 - p) * next[i + 1]);
		}

		return new RandomVariableFromDoubleArray(k * dt, res);
	}

	@Override
	public int getNumberOfBranches(int timeIndex, int stateIndex) {
		return 2;
	}

	@Override
	public double getTransitionProbability(int timeIndex, int stateIndex, int branchIndex) {
		switch (branchIndex) {
			case 0: return 0.5;
			case 1: return 0.5;
			default: throw new IllegalArgumentException("Invalid branchIndex " + branchIndex + " for binomial model.");
		}
	}

	@Override
	public int[] getChildStateIndexShift() {
		return new int[] { 0, 1 };
	}

	public MultiplicativeDividendModel getDividendModel() { return dividendModel; }
}
