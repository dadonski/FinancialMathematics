package net.finmath.tree.assetderivativevaluation.dividends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Multiple discrete proportional dividends: S_t = S_t^{nodiv} * prod_{t_k <= t} (1 - delta_k).
 *
 * Each dividend event is a (time, delta) pair. The tree remains recombining
 * since all adjustments are multiplicative.
 *
 * @author Edoardo Anfelli
 */
public class DiscreteProportionalDividends implements MultiplicativeDividendModel {

	private final List<DividendEvent> events;
	
	/**
	 * Creates a model from parallel arrays of times and deltas.
	 *
	 * @param times  The dividend payment times.
	 * @param deltas The proportional dividend amounts.
	 */
	public DiscreteProportionalDividends(double[] times, double[] deltas) {
		if (times.length != deltas.length) {
			throw new IllegalArgumentException("times and deltas must have the same length.");
		}
		this.events = new ArrayList<>();
		for (int i = 0; i < times.length; i++) {
			this.events.add(new ProportionalDividend(times[i], deltas[i]));
		}
		this.events.sort(Comparator.comparingDouble(DividendEvent::getTime));
	}
	
	
//		IMPORTANT If dividend dates do not fall exactly on the time grid, map them to the first
//		grid point greater than or equal to the dividend time for simplicity. In this project we assume a
//		uniform time grid. Therefore, instead of refining the grid, we map dividend dates to the next
//		grid point. Including extra time points in order to cover the dates where the dividends are paid
//		could violate the assumption of a uniform time discretization (which affect the way risk neutral
//		probabilities are computed!). Solution:
	
	
	@Override
	public void snapToGrid(double deltaT) {
		// Rebuild events with snapped times
		List<DividendEvent> snapped = new ArrayList<>();
		for (DividendEvent event : events) {
			double snappedTime = Math.ceil(event.getTime() / deltaT) * deltaT;
			snapped.add(new ProportionalDividend(snappedTime, event.getDelta()));
		}
		this.events.clear();
		this.events.addAll(snapped);
		this.events.sort(Comparator.comparingDouble(DividendEvent::getTime));
	}

	
	/**
	 * Returns an unmodifiable view of the dividend events sorted by time.
	 *
	 * @return The list of dividend events.
	 */
	public List<DividendEvent> getEvents() {
		return Collections.unmodifiableList(events);
	}
	
	@Override
	public double getCumulativeDividendFactor(double time) {
		double factor = 1.0;
		for (DividendEvent event : events) {
			if (event.getTime() <= time) {
				factor *= (1.0 - event.getDelta());
			}
		}
		return factor;
	}

	@Override
	public double getForwardDividendFactor(double time1, double time2) {
		double factor = 1.0;
		for (DividendEvent event : events) {
			double tDiv = event.getTime();
			if (tDiv > time1 && tDiv <= time2) {
				factor *= (1.0 - event.getDelta());
			}
		}
		return factor;
	}
	
}
