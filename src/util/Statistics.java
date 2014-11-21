/*
 * Component of GAE Project for TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [http://www.gnu.org/licenses/].
 */

package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class Statistics {
	public final static int NUM_BINS = 15;
	private double[] scores;
	private List<Integer> outliers = new ArrayList<Integer>();
	private Map<String, Double> summaryStatistics = new HashMap<String, Double>(7);
	private List<Pair<Pair<Double, Double>, Long>> bins = new ArrayList<Pair<Pair<Double, Double>, Long>>(NUM_BINS);

	public Statistics(List<Integer> list) {
		scores = intsToDoubles(list);
		DescriptiveStatistics dStats = new DescriptiveStatistics(scores);

		summaryStatistics.put("min", dStats.getMin()); // Minimum
		summaryStatistics.put("q1", dStats.getPercentile(25)); // Lower Quartile (Q1)
		summaryStatistics.put("q2", dStats.getPercentile(50)); // Middle Quartile (Median - Q2)
		summaryStatistics.put("q3", dStats.getPercentile(75)); // High Quartile (Q3)
		summaryStatistics.put("max", dStats.getMax()); // Maxiumum

		summaryStatistics.put("mean", dStats.getMean()); // Mean
		summaryStatistics.put("sd", dStats.getStandardDeviation()); // Standard Deviation

		EmpiricalDistribution distribution = new EmpiricalDistribution(NUM_BINS);
		distribution.load(scores);
		List<SummaryStatistics> binStats = distribution.getBinStats();
		double[] upperBounds = distribution.getUpperBounds();

		Double lastUpperBound = upperBounds[0];
		bins.add(new Pair<Pair<Double, Double>, Long>(new Pair<Double, Double>(summaryStatistics.get("min"), lastUpperBound), binStats.get(0).getN()));
		for (int i = 1; i < binStats.size(); i++) {
			bins.add(new Pair<Pair<Double, Double>, Long>(new Pair<Double, Double>(lastUpperBound, upperBounds[i]), binStats.get(i).getN()));
			lastUpperBound = upperBounds[i];
		}

		if (list.size() > 5 && dStats.getStandardDeviation() > 0) // Only remove outliers if relatively normal
		{
			double mean = dStats.getMean();
			double stDev = dStats.getStandardDeviation();
			NormalDistribution normalDistribution = new NormalDistribution(mean, stDev);

			Iterator<Integer> listIterator = list.iterator();
			double significanceLevel = .50 / list.size(); // Chauvenet's Criterion for Outliers
			while (listIterator.hasNext()) {
				int num = listIterator.next();
				double pValue = normalDistribution.cumulativeProbability(num);
				if (pValue < significanceLevel) {
					outliers.add(num);
					listIterator.remove();
				}
			}

			if (list.size() != dStats.getN()) // If and only if outliers have been removed
			{
				double[] significantData = intsToDoubles(list);
				dStats = new DescriptiveStatistics(significantData);

				summaryStatistics.put("min", dStats.getMin());
				summaryStatistics.put("max", dStats.getMax());
				summaryStatistics.put("mean", dStats.getMean());
				summaryStatistics.put("sd", dStats.getStandardDeviation());
			}
		}
	}

	public double[] getScores() {
		return scores;
	}

	public Map<String, Double> getSummary() {
		return summaryStatistics;
	}

	public List<Integer> getOutliers() {
		return outliers;
	}

	public List<Pair<Pair<Double, Double>, Long>> getBins() {
		return bins;
	}

	private static double[] intsToDoubles(List<Integer> ints) {
		double[] doubles = new double[ints.size()];
		for (int i = 0; i < ints.size(); i++) {
			doubles[i] = ints.get(i);
		}
		return doubles;
	}
}
