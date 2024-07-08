/* ==================================================================
 * PanasonicBatteryDatum.java - 16/02/2016 8:07:31 pm
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.panasonic.battery;

import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleEnergyStorageDatum;
import net.solarnetwork.node.hw.panasonic.battery.BatteryData;

/**
 * Extension of {@link SimpleEnergyStorageDatum} with specific support for the
 * Panasonic Battery API.
 *
 * @author matt
 * @version 2.0
 */
public class PanasonicBatteryDatum extends SimpleEnergyStorageDatum {

	private static final long serialVersionUID = -3893889583456359279L;

	/** The data. */
	private final BatteryData sample;

	/**
	 * Construct with a sample.
	 *
	 * @param sample
	 *        the sample
	 * @param sourceId
	 *        the source ID
	 */
	public PanasonicBatteryDatum(BatteryData sample, String sourceId) {
		super(sourceId, sample.getDate(), new DatumSamples());
		this.sample = sample;
		sample.populateMeasurements(this);
	}

	/**
	 * Test if the data appears valid in this datum.
	 *
	 * @return {@literal true} if the data appears to be valid
	 */
	public boolean isValid() {
		return (getAvailableEnergy() != null);
	}

	/**
	 * Get the raw sample data used by this datum.
	 *
	 * @return the sample data
	 */
	public BatteryData getSample() {
		return sample;
	}

}
