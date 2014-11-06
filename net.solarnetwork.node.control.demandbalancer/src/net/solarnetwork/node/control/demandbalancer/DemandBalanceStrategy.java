/* ==================================================================
 * DemandBalanceStrategy.java - Mar 23, 2014 6:53:58 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.demandbalancer;


/**
 * API for evaluating current demand balance conditions and producing a desired
 * generation limit.
 * 
 * @author matt
 * @version 1.1
 */
public interface DemandBalanceStrategy {

	/**
	 * Get a unique identifier for this strategy. This should be meaningful to
	 * the strategy implementation and unique across all other strategies
	 * deployed in the runtime.
	 * 
	 * @return unique identifier
	 */
	String getUID();

	/**
	 * Evaluate current demand and generation conditions, and apply an
	 * adjustment if necessary.
	 * 
	 * @param powerControlId
	 *        the ID if the control managing the generation limit
	 * @param demandWatts
	 *        the current demand, in watts, or <b>-1</b> if unknown
	 * @param generationWatts
	 *        the current generation, in watts, or <b>-1</b> if unknown
	 * @param generationCapacityWatts
	 *        the generation capacity, in watts, or <b>-1</b> if unknown
	 * @param currentLimit
	 *        the current generation limit as an integer percentage, or
	 *        <b>-1</b> if unknown
	 * @return the desired generation limit, as an integer percentage, or
	 *         <b>-1</b> for no change
	 */
	int evaluateBalance(final String powerControlId, final int demandWatts, final int generationWatts,
			final int generationCapacityWatts, final int currentLimit);

}
