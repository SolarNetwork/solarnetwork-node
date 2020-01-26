/* ==================================================================
 * EM5600Datum.java - Mar 26, 2014 10:17:43 AM
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

package net.solarnetwork.node.datum.hc.em5600;

import java.util.Date;
import java.util.Map;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.hc.EM5600DataAccessor;

/**
 * Extension of {@link GeneralNodeACEnergyDatum} with additional properties
 * supported by the EM5600 series meters.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class EM5600Datum extends GeneralNodeACEnergyDatum implements ACEnergyDataAccessor {

	private final EM5600DataAccessor data;
	private final boolean backwards;

	/**
	 * Construct with a sample.
	 * 
	 * @param sample
	 *        the sample
	 * @param phase
	 *        the phase
	 * @param backwards
	 *        {@literal true} to reverse the direction of current
	 */
	public EM5600Datum(EM5600DataAccessor data, ACPhase phase, boolean backwards) {
		super();
		this.data = data;
		this.backwards = backwards;
		if ( data.getDataTimestamp() > 0 ) {
			setCreated(new Date(data.getDataTimestamp()));
		}
		setPhase(phase);
		ACEnergyDataAccessor phaseData = accessorForPhase(phase);
		populateMeasurements(phaseData, phase);
	}

	private void populateMeasurements(ACEnergyDataAccessor data, ACPhase phase) {
		setPhase(phase);
		setFrequency(data.getFrequency());
		setVoltage(data.getVoltage());
		setLineVoltage(data.getLineVoltage());
		setCurrent(data.getCurrent());
		setNeutralCurrent(data.getNeutralCurrent());
		setPowerFactor(data.getPowerFactor());
		setApparentPower(data.getApparentPower());
		setReactivePower(data.getReactivePower());
		setWatts(data.getActivePower());
		setWattHourReading(data.getActiveEnergyDelivered());
		setReverseWattHourReading(data.getActiveEnergyReceived());
	}

	/**
	 * Test if the data appears valid in this datum.
	 * 
	 * @return <em>true</em> if the data appears to be valid
	 */
	public boolean isValid() {
		return (getWatts() != null || getWattHourReading() != null);
	}

	@Override
	public long getDataTimestamp() {
		return data.getDataTimestamp();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return data.getDeviceInfo();
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		ACEnergyDataAccessor phaseData = data.accessorForPhase(phase);
		if ( backwards ) {
			phaseData = phaseData.reversed();
		}
		return phaseData;
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		return data.reversed();
	}

	@Override
	public Integer getActivePower() {
		return data.getActivePower();
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return data.getActiveEnergyDelivered();
	}

	@Override
	public Long getActiveEnergyReceived() {
		return data.getActiveEnergyReceived();
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return data.getApparentEnergyDelivered();
	}

	@Override
	public Long getApparentEnergyReceived() {
		return data.getApparentEnergyReceived();
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return data.getReactiveEnergyDelivered();
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return data.getReactiveEnergyReceived();
	}

}
