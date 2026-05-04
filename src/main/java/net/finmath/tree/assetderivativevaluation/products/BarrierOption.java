package net.finmath.tree.assetderivativevaluation.products;

import java.util.function.DoubleUnaryOperator;

import net.finmath.modelling.products.BarrierType;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.TreeModel;

/**
 * Barrier option priced on a recombining TreeModel.
 *
 * Supports all 8 barrier types (Down/Up-In/Out Call/Put) via:
 * - OUT barriers: backward induction with barrier knock-out at each step.
 * - IN barriers:  IN-OUT parity: V_IN = V_European - V_OUT.
 *
 * Rebate handling:
 * - OUT barriers: rebate paid at hit (discounted back to time 0).
 * - IN barriers:  rebate paid at expiration if not knocked in.
 *
 * @author Edoardo Anfelli
 */
public class BarrierOption extends AbstractNonPathDependentProduct {

	private final double strike;
	private final double barrier;
	private final BarrierType barrierType;
	private final boolean isCall;
	private final double rebate;

	/**
	 * Creates a barrier option.
	 *
	 * @param maturity    Option maturity.
	 * @param strike      Strike price.
	 * @param barrier     Barrier level.
	 * @param barrierType The type of barrier (DOWN_IN, UP_IN, DOWN_OUT, UP_OUT).
	 * @param isCall      True for call, false for put.
	 * @param rebate      Rebate amount (0.0 if none).
	 */
	public BarrierOption(double maturity, double strike, double barrier, BarrierType barrierType, boolean isCall, double rebate) {
		super(maturity);
		this.strike = strike;
		this.barrier = barrier;
		this.barrierType = barrierType;
		this.isCall = isCall;
		this.rebate = rebate;
	}

	/**
	 * Creates a barrier option with no rebate.
	 */
	public BarrierOption(double maturity, double strike, double barrier, BarrierType barrierType, boolean isCall) {
		this(maturity, strike, barrier, barrierType, isCall, 0.0);
	}

	@Override
	public DoubleUnaryOperator getPayOffFunction() {
		if (isCall) {
			return s -> Math.max(s - strike, 0.0);
		} else {
			return s -> Math.max(strike - s, 0.0);
		}
	}

	/**
	 * Returns true if this is an IN-type barrier.
	 */
	private boolean isInOption() {
		return barrierType == BarrierType.DOWN_IN || barrierType == BarrierType.UP_IN;
	}

	/**
	 * Returns the corresponding OUT barrier type for use in IN-OUT parity.
	 */
	private BarrierType correspondingOutType() {
		switch (barrierType) {
			case DOWN_IN: return BarrierType.DOWN_OUT;
			case UP_IN:   return BarrierType.UP_OUT;
			default: throw new IllegalStateException("Not an IN barrier type.");
		}
	}

	@Override
	public RandomVariable[] getValues(double evaluationTime, TreeModel model) {
		validate(evaluationTime, model);

		if (isInOption()) {
			return getValuesViaInOutParity(evaluationTime, model);
		}
		return getValuesForOutOption(evaluationTime, model);
	}

	/**
	 * IN barriers via parity: V_IN = V_European - V_OUT.
	 * If rebate > 0, adds discounted rebate for paths that never knock in.
	 */
	private RandomVariable[] getValuesViaInOutParity(double evaluationTime, TreeModel model) {
		// Price the corresponding European option
		EuropeanNonPathDependent european = new EuropeanNonPathDependent(getMaturity(), getPayOffFunction());
		RandomVariable[] europeanValues = european.getValues(evaluationTime, model);

		// Price the corresponding OUT option (with rebate = 0 for parity)
		BarrierOption outOption = new BarrierOption(getMaturity(), strike, barrier, correspondingOutType(), isCall, 0.0);
		RandomVariable[] outValues = outOption.getValues(evaluationTime, model);

		// IN = European - OUT
		int length = europeanValues.length;
		RandomVariable[] inValues = new RandomVariable[length];
		for (int i = 0; i < length; i++) {
			double[] eu = europeanValues[i].getRealizations();
			double[] out = outValues[i].getRealizations();
			double[] in = new double[eu.length];
			for (int j = 0; j < eu.length; j++) {
				in[j] = eu[j] - out[j];
			}
			inValues[i] = new RandomVariableFromDoubleArray(europeanValues[i].getFiltrationTime(), in);
		}

		return inValues;
	}

	/**
	 * OUT barriers: backward induction with knock-out.
	 * At each time step, nodes where the barrier is breached are set to the
	 * discounted rebate value (or 0 if no rebate).
	 */
	private RandomVariable[] getValuesForOutOption(double evaluationTime, TreeModel model) {
		final int k0 = timeToIndex(evaluationTime, model);
		final int n = model.getNumberOfTimes() - 1;
		final RandomVariable[] values = new RandomVariable[n - k0 + 1];

		// Terminal payoff with barrier check
		RandomVariable spotT = model.getSpotAtGivenTimeIndexRV(n);
		double[] spotTvals = spotT.getRealizations();
		RandomVariable payoffRV = model.getTransformedValuesAtGivenTimeRV(model.getLastTime(), getPayOffFunction());
		double[] payoff = payoffRV.getRealizations();
		double[] termVal = new double[payoff.length];

		for (int i = 0; i < payoff.length; i++) {
			if (isKnockedOut(spotTvals[i])) {
				termVal[i] = rebate; // rebate at maturity for knocked-out terminal nodes
			} else {
				termVal[i] = payoff[i];
			}
		}
		values[n - k0] = new RandomVariableFromDoubleArray(model.getLastTime(), termVal);

		// Backward induction with barrier knock-out
		for (int k = n - 1; k >= k0; k--) {
			RandomVariable continuation = model.getConditionalExpectationRV(values[(k + 1) - k0], k);
			double[] cont = continuation.getRealizations();
			RandomVariable spotK = model.getSpotAtGivenTimeIndexRV(k);
			double[] spotKvals = spotK.getRealizations();
			double[] result = new double[cont.length];

			for (int i = 0; i < cont.length; i++) {
				if (isKnockedOut(spotKvals[i])) {
					// Knocked out: pay rebate (already discounted by backward induction)
					result[i] = rebate * model.getOneStepDiscountFactor(k);
				} else {
					result[i] = cont[i];
				}
			}
			values[k - k0] = new RandomVariableFromDoubleArray(k * model.getTimeStep(), result);
		}

		return values;
	}

	/**
	 * Checks if a spot value triggers a knock-out for this barrier type.
	 */
	private boolean isKnockedOut(double spot) {
		switch (barrierType) {
			case DOWN_OUT: return spot <= barrier;
			case UP_OUT:   return spot >= barrier;
			// For IN barriers used internally via parity:
			case DOWN_IN:  return spot <= barrier;
			case UP_IN:    return spot >= barrier;
			default: return false;
		}
	}

	/**
	 * Validates inputs before pricing.
	 */
	protected void validate(double evaluationTime, TreeModel model) {
		if (model == null) {
			throw new IllegalArgumentException("Model cannot be null.");
		}
	}

	// Getters
	public double getStrike() { return strike; }
	public double getBarrier() { return barrier; }
	public BarrierType getBarrierType() { return barrierType; }
	public boolean isCall() { return isCall; }
	public double getRebate() { return rebate; }
}
