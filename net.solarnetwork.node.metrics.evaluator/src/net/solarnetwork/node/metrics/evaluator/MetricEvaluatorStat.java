/* ==================================================================
 * MetricEvaluatorStat.java - 29/07/2024 3:15:10 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.metrics.evaluator;

/**
 * Statics for the metric evaluator.
 *
 * @author matt
 * @version 1.0
 */
public enum MetricEvaluatorStat {

	/** Evaluations executed. */
	EvaluationCount("evaluation count"),

	/** Milliseconds spent processing evaluations. */
	ProcessingTime("processing ms"),

	/** Milliseconds spent querying metric data. */
	MetricQueryTime("metric query ms"),

	/** Milliseconds spent executing expressions. */
	ExpressionExeucutionTime("expression execution ms"),

	/** Milliseconds spent executing instructions. */
	InstructionExecutionTime("instruction execution ms"),

	/** A count of the expression number that produce a result. */
	ExpressionCount("expression count")

	;

	private final String description;

	private MetricEvaluatorStat(String description) {
		this.description = description;
	}

	/**
	 * Get the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
