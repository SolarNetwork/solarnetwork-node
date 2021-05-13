/* ==================================================================
 * VirtualMeterExpressionRootImpl.java - 9/05/2021 7:50:18 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.samplefilter.virt;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.support.ExpressionService;

/**
 * An object to use as the "root" for virtual meter {@link ExpressionService}
 * evaluation.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public class VirtualMeterExpressionRootImpl extends ExpressionRoot
		implements VirtualMeterExpressionRoot {

	private final VirtualMeterConfig config;
	private final Map<String, ?> metadata;
	private final long prevDate;
	private final long currDate;
	private final BigDecimal prevVal;
	private final BigDecimal currVal;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param config
	 *        the virtual meter configuration
	 * @param prevDate
	 *        the previous reading date, as an epoch
	 * @param currDate
	 *        the current reading date, as an epoch
	 * @param prevVal
	 *        the previous reading value
	 * @param currVal
	 *        the current reading value
	 * @param metadata
	 *        metadata
	 */
	public VirtualMeterExpressionRootImpl(Datum datum, VirtualMeterConfig config, long prevDate,
			long currDate, BigDecimal prevVal, BigDecimal currVal, Map<String, ?> metadata) {
		super(datum);
		this.config = config;
		this.prevDate = prevDate;
		this.currDate = currDate;
		this.prevVal = prevVal;
		this.currVal = currVal;
		this.metadata = (metadata != null ? metadata : Collections.emptyMap());
	}

	@Override
	public VirtualMeterConfig getConfig() {
		return config;
	}

	@Override
	public Map<String, ?> getMetadata() {
		return metadata;
	}

	@Override
	public long getPrevDate() {
		return prevDate;
	}

	@Override
	public long getCurrDate() {
		return currDate;
	}

	@Override
	public BigDecimal getPrevVal() {
		return prevVal;
	}

	@Override
	public BigDecimal getCurrVal() {
		return currVal;
	}

}
