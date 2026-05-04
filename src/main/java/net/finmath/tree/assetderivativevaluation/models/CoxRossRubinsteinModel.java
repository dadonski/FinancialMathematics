package net.finmath.tree.assetderivativevaluation.models;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.assetderivativevaluation.AbstractRecombiningTreeModel;
import net.finmath.tree.assetderivativevaluation.dividends.ContinuousDividendYield;
import net.finmath.tree.assetderivativevaluation.dividends.MultiplicativeDividendModel;
import net.finmath.tree.assetderivativevaluation.dividends.NoDividends;

/**
 * This class represents the approximation of a Black-Scholes model via the Cox Ross Rubinstein  model.
 * It extends a recombining tree model. The implemented method computes the up and down factors
 *
 * Supports multiplicative dividends:
 * - Proportional/Discrete proportional: tree remains recombining, dividends multiply node values.
 * - Continuous dividend yield q: modifies risk-neutral probability p = (exp((r-q)*dt) - d) / (u - d).
 *
 * @author Andrea Mazzon
 * @author Edoardo Anfelli
 */
public class CoxRossRubinsteinModel extends AbstractRecombiningTreeModel {

	/** Specific CRR parameters */
	private final double u;
	private final double d;
	private final double q;

	/** Dividend model */
	private final MultiplicativeDividendModel dividendModel;

	/**
	 * Constructs a CRR model without dividends.
	 */
	public CoxRossRubinsteinModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		this(initialPrice, riskFreeRate, volatility, lastTime, timeStep, new NoDividends());
	}

	/**
	 * Constructs a CRR model without dividends.
	 */
	public CoxRossRubinsteinModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		this(initialPrice, riskFreeRate, volatility, lastTime, numberOfTimes, new NoDividends());
	}

	/**
	 * Constructs a CRR model with a dividend model.
	 *
	 * For continuous dividend yield the e.m.m. q = (exp((r-q)*dt) - d) / (u - d).
	 * For proportional dividends: q is unchanged, node values adjusted by D(t).
	 */
	public CoxRossRubinsteinModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, double timeStep, MultiplicativeDividendModel dividendModel) {
		super(initialPrice, riskFreeRate, volatility, lastTime, timeStep);
		this.dividendModel = dividendModel;
		this.dividendModel.snapToGrid(getTimeStep());
		double dt = getTimeStep();
		double sigma = getVolatility();
		double r = getRiskFreeRate();
		this.u = Math.exp(sigma * Math.sqrt(dt));
		this.d = 1.0 / u;

		// Continuous dividend yield modifies the risk-neutral drift
		double effectiveGrowth;
		if (dividendModel instanceof ContinuousDividendYield) {
			double qDiv = ((ContinuousDividendYield) dividendModel).getYield();
			effectiveGrowth = Math.exp((r - qDiv) * dt);
		} else {
			effectiveGrowth = Math.exp(r * dt);
		}
		this.q = (effectiveGrowth - d) / (u - d);
	}

	/**
	 * Constructs a CRR model with a dividend model (numberOfTimes variant).
	 */
	public CoxRossRubinsteinModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes, MultiplicativeDividendModel dividendModel) {
		super(initialPrice, riskFreeRate, volatility, lastTime, numberOfTimes);
		this.dividendModel = dividendModel;
		this.dividendModel.snapToGrid(getTimeStep());
		double dt = getTimeStep();
		double sigma = getVolatility();
		double r = getRiskFreeRate();
		this.u = Math.exp(sigma * Math.sqrt(dt));
		this.d = 1.0 / u;

		double effectiveGrowth;
		if (dividendModel instanceof ContinuousDividendYield) {
			double qDiv = ((ContinuousDividendYield) dividendModel).getYield();
			effectiveGrowth = Math.exp((r - qDiv) * dt);
		} else {
			effectiveGrowth = Math.exp(r * dt);
		}
		this.q = (effectiveGrowth - d) / (u - d);
	}

	@Override
	public int statesAt(int k) {
		return k + 1;
	}

	/**
	 * Build S_k[i] = S0 * u^(k-i) * d^i * D(t_k).
	 * For proportional dividends, D(t_k) is the cumulative dividend factor.
	 * For continuous dividend yield, D(t) is already embedded in probabilities,
	 * but we still need D(t_k) = exp(-q*t_k) to adjust node values.
	 */
	
	
	@Override
	protected RandomVariable buildSpotLevel(int k) {
		double[] s = new double[statesAt(k)];
		double s0 = getInitialPrice();
		double time = k * getTimeStep();
		double divFactor = dividendModel.getCumulativeDividendFactor(time);

		for (int i = 0; i <= k; i++) {
			int up = k - i;
			int down = i;
			s[i] = s0 * Math.pow(u, up) * Math.pow(d, down) * divFactor;
		}
		return new RandomVariableFromDoubleArray(time, s);
	}

	/**
	 * Discounted conditional expectation: V_k[i] = df * (q * V_{k+1}[i] + (1-q) * V_{k+1}[i+1]).
	 */
	
	@Override
	protected RandomVariable conditionalExpectation(RandomVariable vNext, int k) {
		double[] next = vNext.getRealizations();
		double[] vK = new double[statesAt(k)];
		double disc = getOneStepDiscountFactor(k);

		for (int i = 0; i <= k; i++) {
			vK[i] = disc * (q * next[i] + (1.0 - q) * next[i + 1]);
		}
		return new RandomVariableFromDoubleArray(k * getTimeStep(), vK);
	}

	@Override
	public int getNumberOfBranches(int timeIndex, int stateIndex) {
		return 2;
	}

	@Override
	public double getTransitionProbability(int timeIndex, int stateIndex, int branchIndex) {
		switch (branchIndex) {
			case 0: return q;
			case 1: return 1.0 - q;
			default: throw new IllegalArgumentException("Invalid branchIndex " + branchIndex + " for binomial model.");
		}
	}

	@Override
	public int[] getChildStateIndexShift() {
		return new int[] { 0, 1 };
	}

	/** Getter */
	public double getU() { return u; }
	public double getD() { return d; }
	public double getQ() { return q; }
	public MultiplicativeDividendModel getDividendModel() { return dividendModel; }
}
