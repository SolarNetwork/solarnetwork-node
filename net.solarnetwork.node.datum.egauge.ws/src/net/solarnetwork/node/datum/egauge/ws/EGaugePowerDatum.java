/* ==================================================================
 * EnaSolarPowerDatum.java - Nov 16, 2013 6:41:18 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws;

import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;

/**
 * Extension of {@link PowerDatum} to map eGauge data appropriately.
 * 
 * @author maxieduncan
 * @version 1.2
 */
public class EGaugePowerDatum extends GeneralNodePVEnergyDatum {

	private static final String UNIT_POWER = "P";

	/** The number of seconds in an hour, used for conversion. */
	private static final int HOUR_SECONDS = 3600;

	/**
	 * Adds an eGauge accumulating value converting eGauge units into the SN
	 * equivalent.
	 * 
	 * @param propertyConfig
	 *        the configuration for the property being recorded
	 * @param type
	 *        the eGauge value type
	 * @param value
	 *        the accumulating value to record
	 */
	public void addEGaugeAccumulatingPropertyReading(EGaugePropertyConfig propertyConfig, String type,
			String value) {
		switch (type) {
			case UNIT_POWER:
				if ( value != null ) {
					// Convert watt-seconds into watt-hours
					Long wattHours = Long.valueOf(value) / HOUR_SECONDS;// TODO review rounding
					super.putAccumulatingSampleValue(
							propertyConfig.getPropertyName() + "WattHourReading", wattHours);
				}
				break;
			default:
				throw new UnsupportedOperationException(
						"Values of type: " + type + " aren't currently supported");
		}
	}

	/**
	 * Adds an eGauge instantaneous reading.
	 * 
	 * @param propertyConfig
	 *        the configuration for the property being recorded
	 * @param type
	 *        the eGauge value type
	 * @param value
	 *        the accumulating value to record, ignored if null
	 * @param instantenouseValue
	 *        the instantaneous reading
	 */
	public void addEGaugeInstantaneousPropertyReading(EGaugePropertyConfig propertyConfig, String type,
			String value, String instantenouseValue) {
		addEGaugeAccumulatingPropertyReading(propertyConfig, type, value);

		switch (type) {
			case UNIT_POWER:
				if ( instantenouseValue != null ) {
					super.putInstantaneousSampleValue(propertyConfig.getPropertyName() + "Watts",
							Integer.valueOf(instantenouseValue));
					break;
				}
			default:
				throw new UnsupportedOperationException(
						"Values of type: " + type + " aren't currently supported");
		}
	}

	public String getSampleInfo(EGaugePropertyConfig[] eGaugePropertyConfigs) {
		StringBuilder buf = new StringBuilder();
		for ( EGaugePropertyConfig propertyConfig : eGaugePropertyConfigs ) {
			switch (propertyConfig.getReadingType()) {
				case INSTANTANEOUS:
					buf.append(getInstantaneousSampleInteger(propertyConfig.getPropertyName() + "Watts"))
							.append(" W; ");
				case TOTAL:
					buf.append(getAccumulatingSampleLong(
							propertyConfig.getPropertyName() + "WattHourReading")).append(" Wh;");
				default:
					buf.append(" " + propertyConfig.getPropertyName() + " sample created.\n");
			}
		}
		return buf.toString();
	}

}
