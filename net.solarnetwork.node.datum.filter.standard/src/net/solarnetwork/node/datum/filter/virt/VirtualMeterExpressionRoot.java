/* ==================================================================
 * VirtualMeterExpressionRoot.java - 9/05/2021 11:31:31 AM
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

package net.solarnetwork.node.datum.filter.virt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.domain.datum.DatumExpressionRoot;

/**
 * API for a virtual meter expression root object.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public interface VirtualMeterExpressionRoot extends DatumExpressionRoot {

	/**
	 * Get the virtual meter configuration.
	 * 
	 * @return the configuration
	 */
	VirtualMeterConfig getConfig();

	/**
	 * Get the previous date.
	 * 
	 * @return the date, as an epoch
	 */
	long getPrevDate();

	/**
	 * Get the current date.
	 * 
	 * @return the date, as an epoch
	 */
	long getCurrDate();

	/**
	 * Get the amount of time units between the previous and current dates.
	 * 
	 * @return the time units, using a default scale of {@literal 12}, never
	 *         {@literal null}
	 */
	default BigDecimal getTimeUnits() {
		return timeUnits(12);
	}

	/**
	 * Get the amount of time units between the previous and current dates.
	 * 
	 * @param scale
	 *        the desired decimal scale of the output
	 * @return the time units, never {@literal null}
	 */
	default BigDecimal timeUnits(int scale) {
		VirtualMeterConfig config = getConfig();
		TimeUnit unit = (config != null ? config.getTimeUnit() : null);
		if ( unit == null ) {
			return BigDecimal.ZERO;
		}
		long msDiff = getCurrDate() - getPrevDate();
		if ( msDiff == 0 ) {
			return BigDecimal.ZERO;
		}
		BigDecimal unitMs = new BigDecimal(unit.toMillis(1));
		return BigDecimal.valueOf(msDiff).divide(unitMs, scale, RoundingMode.HALF_UP);
	}

	/**
	 * Get the previous input property value.
	 * 
	 * @return the previous property value, never {@literal null}
	 */
	BigDecimal getPrevInput();

	/**
	 * Get the current input property value.
	 * 
	 * @return the current property value, never {@literal null}
	 */
	BigDecimal getCurrInput();

	/**
	 * Get the input property difference, as {@code curr - prev}.
	 * 
	 * @return the difference, never {@literal null}
	 */
	default BigDecimal getInputDiff() {
		BigDecimal prev = getPrevInput();
		BigDecimal curr = getCurrInput();
		if ( prev == null || curr == null ) {
			return BigDecimal.ZERO;
		}
		VirtualMeterConfig config = getConfig();
		if ( config != null && config.getVirtualMeterScale() > 0
				&& curr.scale() < config.getVirtualMeterScale() ) {
			curr = curr.setScale(config.getVirtualMeterScale());
		}
		return curr.subtract(prev);
	}

	/**
	 * Get the previous output reading value.
	 * 
	 * @return the previous reading value, never {@literal null}
	 */
	BigDecimal getPrevReading();

}
