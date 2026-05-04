package net.finmath.tree.assetderivativevaluation.models;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.assetderivativevaluation.AbstractRecombiningTreeModel;
import net.finmath.tree.assetderivativevaluation.dividends.ContinuousDividendYield;
import net.finmath.tree.assetderivativevaluation.dividends.MultiplicativeDividendModel;
import net.finmath.tree.assetderivativevaluation.dividends.NoDividends;

/**
 * Trinomial (Boyle) model for option pricing.
 *
 * Supports multiplicative dividends:
 * - Proportional/Discrete proportional: node values adjusted by D(t), probabilities unchanged.
 * - Continuous dividend yield q: R = exp((r-q)*dt), M2 = exp(2(r-q)*dt + sigma^2*dt),
 *   probabilities recomputed accordingly.
 *
 * @author Andrea Mazzon
 */
public class BoyleTrinomial extends AbstractRecombiningTreeModel {

	/** Specific parameters */
	private final double dt;
	private final double r;
	private final double u;
	private final double d;
	private final double pu, pm, pd;

	/** Dividend model */
	private final MultiplicativeDividendModel dividendModel;

	/**
	 * Constructs a Boyle trinomial model without dividends.
	 */
	public BoyleTrinomial(double spotPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		this(spotPrice, riskFreeRate, volatility, lastTime, timeStep, new NoDividends());
	}

	/**
	 * Constructs a Boyle trinomial model without dividends.
	 */
	public BoyleTrinomial(double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		this(spotPrice, riskFreeRate, volatility, lastTime, lastTime / (numberOfTimes - 1.0), new NoDividends());
	}

	/**
	 * Constructs a Boyle trinomial model with a dividend model.
	 *
	 * DIVIDEND MODEL
	 * For continuous dividend yield q:
	 *   R = exp((r-q)*dt), M2 = exp(2*(r-q)*dt + sigma^2*dt).
	 * For proportional dividends: R and M2 unchanged, node values adjusted.
	 */
	public BoyleTrinomial(double spotPrice, double riskFreeRate, double volatility, double lastTime, double timeStep, MultiplicativeDividendModel dividendModel) {
		super(spotPrice, riskFreeRate, volatility, lastTime, timeStep);
		this.dividendModel = dividendModel;
		this.dividendModel.snapToGrid(getTimeStep());
		this.dt = getTimeStep();
		this.u  = Math.exp(getVolatility() * Math.sqrt(2.0 * dt));
		this.d  = 1.0 / u;

		// For continuous dividend yield, R and M2 use (r-q) instead of r
		double effectiveRate;
		if (dividendModel instanceof ContinuousDividendYield) {
			double qDiv = ((ContinuousDividendYield) dividendModel).getYield();
			effectiveRate = getRiskFreeRate() - qDiv;
		} else {
			effectiveRate = getRiskFreeRate();
		}

		this.r  = Math.exp(effectiveRate * dt);
		double M2 = Math.exp(2.0 * effectiveRate * dt + Math.pow(getVolatility(), 2) * dt);

		double b1 = r  - 1.0;
		double b2 = M2 - 1.0;

		double A11 = (u - 1.0);
		double A12 = (d - 1.0);
		double A21 = (u * u - 1.0);
		double A22 = (d * d - 1.0);

		double det = A11 * A22 - A12 * A21;
		if (Math.abs(det) < 1e-14) {
			throw new IllegalArgumentException("Degenerate system for trinomial probabilities (det≈0).");
		}

		double puTmp = (b1 * A22 - b2 * A12) / det;
		double pdTmp = (A11 * b2 - A21 * b1) / det;
		double pmTmp = 1.0 - puTmp - pdTmp;

		double puC = Math.max(0.0, Math.min(1.0, puTmp));
		double pmC = Math.max(0.0, Math.min(1.0, pmTmp));
		double pdC = Math.max(0.0, Math.min(1.0, pdTmp));
		double sum = puC + pmC + pdC;
		if (sum <= 0.0) throw new IllegalArgumentException("Invalid probabilities in Boyle trinomial.");
		this.pu = puC / sum;
		this.pm = pmC / sum;
		this.pd = pdC / sum;
	}

	/**
	 * Constructs a Boyle trinomial model with a dividend model (numberOfTimes variant).
	 */
	public BoyleTrinomial(double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes, MultiplicativeDividendModel dividendModel) {
		this(spotPrice, riskFreeRate, volatility, lastTime, lastTime / (numberOfTimes - 1.0), dividendModel);
	}

	@Override
	public int statesAt(int k) {
		return 2 * k + 1;
	}

	/**
	 * Builds S_k[i] = S0 * u^ups * d^downs * D(t_k).
	 */
	@Override
	protected RandomVariable buildSpotLevel(int k) {
		double[] level = new double[2 * k + 1];
		double S0 = getSpot();
		double time = k * dt;
		double divFactor = dividendModel.getCumulativeDividendFactor(time);

		for (int j = -k; j <= k; j++) {
			int idx = j + k;
			int upCount   = Math.max(j, 0);
			int downCount = Math.max(-j, 0);
			level[idx] = S0 * Math.pow(u, upCount) * Math.pow(d, downCount) * divFactor;
		}
		return new RandomVariableFromDoubleArray(time, level);
	}

	/**
	 * Backward induction step: V_k(j) = (pu*V_{k+1}(j+1) + pm*V_{k+1}(j) + pd*V_{k+1}(j-1)) / R.
	 */
	@Override
	protected RandomVariable conditionalExpectation(RandomVariable vNext, int k) {
		double[] next = vNext.getRealizations();
		int expectedLen = 2 * (k + 1) + 1;
		if (next.length != expectedLen) {
			throw new IllegalArgumentException("vNext length " + next.length + " != expected " + expectedLen + " for level k=" + k);
		}

		double[] now = new double[2 * k + 1];
		for (int j = -k; j <= k; j++) {
			int idxNow = j + k;

			double up   = next[j + k + 2];
			double mid  = next[j + k + 1];
			double down = next[j + k];

			now[idxNow] = (pu * up + pm * mid + pd * down) / r;
		}

		double time = k * dt;
		return new RandomVariableFromDoubleArray(time, now);
	}

	@Override
	public int getNumberOfBranches(int timeIndex, int stateIndex) {
		return 3;
	}

	@Override
	public double getTransitionProbability(int timeIndex, int stateIndex, int branchIndex) {
		switch (branchIndex) {
			case 0: return pu;
			case 1: return pm;
			case 2: return pd;
			default: throw new IllegalArgumentException("Invalid branchIndex " + branchIndex + " for trinomial model.");
		}
	}

	@Override
	public int[] getChildStateIndexShift() {
		return new int[] { 0, 1, 2 };
	}

	public MultiplicativeDividendModel getDividendModel() { return dividendModel; }
}
