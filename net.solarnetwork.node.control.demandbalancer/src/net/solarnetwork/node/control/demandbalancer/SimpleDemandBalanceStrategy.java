/* ==================================================================
s * SimpleDemandBalanceStrategy.java - Mar 23, 2014 7:46:01 PM
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very basic implementation of {@link DemandBalanceStrategy} that simply
 * enforces a generation limit on the current demand.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDemandBalanceStrategy implements DemandBalanceStrategy {

	/** The UID for this strategy: {@code Simple}. */
	public static final String UID = "Simple";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public String getUID() {
		return UID;
	}

	@Override
	public int evaluateBalance(final String powerControlId, final int demandWatts,
			final int generationWatts, final int generationCapacityWatts, final int currentLimit) {
		int desiredLimit = 100; // default to 100%
		if ( demandWatts < generationCapacityWatts ) {
			// demand is less than generation capacity... enforce limit
			desiredLimit = (int) Math.floor(100.0 * demandWatts / generationCapacityWatts);
			log.debug("Demand of {} is less than {} capacity of {}, limiting to {}%", demandWatts,
					powerControlId, generationCapacityWatts, desiredLimit);
		}
		return desiredLimit;
	}

}
