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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Statistics {
	public static Pair<List<Integer>, List<Integer>> calculateStats(List<Integer> list) {
		double[] data = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			data[i] = list.get(i);
		}
		DescriptiveStatistics dStats = new DescriptiveStatistics(data);

		List<Integer> summary = new ArrayList<Integer>(5);
		summary.add((int) dStats.getMin()); // Minimum
		summary.add((int) dStats.getPercentile(25)); // Lower Quartile (Q1)
		summary.add((int) dStats.getPercentile(50)); // Middle Quartile (Median - Q2)
		summary.add((int) dStats.getPercentile(75)); // High Quartile (Q3)
		summary.add((int) dStats.getMax()); // Maxiumum

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
				summary.set(0, (int) dStats.getMin());
				summary.set(4, (int) dStats.getMax());
			}
		}

		return new Pair<List<Integer>, List<Integer>>(summary, outliers);
	}
}
