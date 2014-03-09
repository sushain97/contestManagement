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
 * GNU General Public License for more destails.
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
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Statistics {
	public static Pair<Map<String, Double>, List<Integer>> calculateStats(List<Integer> list) {
		double[] data = new double[list.size()]; // Convert integers scores to doubles for Apache Math
		for (int i = 0; i < list.size(); i++) {
			data[i] = list.get(i);
		}
		DescriptiveStatistics dStats = new DescriptiveStatistics(data);

		Map<String, Double> summary = new HashMap<String, Double>(5);
		summary.put("min", dStats.getMin()); // Minimum
		summary.put("q1", dStats.getPercentile(25)); // Lower Quartile (Q1)
		summary.put("q2", dStats.getPercentile(50)); // Middle Quartile (Median - Q2)
		summary.put("q3", dStats.getPercentile(75)); // High Quartile (Q3)
		summary.put("max", dStats.getMax()); // Maxiumum

		summary.put("mean", dStats.getMean()); // Mean
		summary.put("sd", dStats.getStandardDeviation()); // Standard Deviation

		List<Integer> outliers = new ArrayList<Integer>();
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
				double[] significantData = new double[list.size()];
				for (int i = 0; i < list.size(); i++) {
					significantData[i] = list.get(i);
				}

				dStats = new DescriptiveStatistics(significantData);
				summary.put("min", dStats.getMin());
				summary.put("max", dStats.getMax());
				summary.put("mean", dStats.getMean());
				summary.put("sd", dStats.getStandardDeviation());
			}
		}

		return new Pair<Map<String, Double>, List<Integer>>(summary, outliers);
	}
}
