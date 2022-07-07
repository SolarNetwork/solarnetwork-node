/* ==================================================================
 * FeedData.java - 7/07/2022 8:40:45 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.overlay.cloud;

import java.time.Instant;
import java.util.Map;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;

/**
 * Feed data (aggregate of all resources within feed).
 * 
 * @author matt
 * @version 1.0
 */
public class FeedData implements FeedDataAccessor {

	private Instant timestamp;
	private Integer wattsA;
	private Integer wattsB;
	private Integer wattsC;
	private Float frequency;
	private Float powerFactorA;
	private Float powerFactorB;
	private Float powerFactorC;
	private Float voltageAN;
	private Float voltageBN;
	private Float voltageCN;
	private Float currentA;
	private Float currentB;
	private Float currentC;
	private Long chargeCapacity; // Wh
	private Long availableCharge; // Wh
	private Integer stateOfHealth;

	private static Float avgValue(Float a, Float b, Float c) {
		double total = 0;
		int count = 0;
		if ( a != null ) {
			count += 1;
			total += a.doubleValue();
		}
		if ( b != null ) {
			count += 1;
			total += b.doubleValue();
		}
		if ( c != null ) {
			count += 1;
			total += c.doubleValue();
		}
		if ( count < 1 ) {
			return null;
		}
		return (float) (total / count);
	}

	private static Integer sumValue(Integer a, Integer b, Integer c) {
		int total = 0;
		int count = 0;
		if ( a != null ) {
			count += 1;
			total += a.longValue();
		}
		if ( b != null ) {
			count += 1;
			total += b.longValue();
		}
		if ( c != null ) {
			count += 1;
			total += c.longValue();
		}
		if ( count < 1 ) {
			return null;
		}
		return total;
	}

	private static Float sumValue(Float a, Float b, Float c) {
		float total = 0;
		int count = 0;
		if ( a != null ) {
			count += 1;
			total += a.floatValue();
		}
		if ( b != null ) {
			count += 1;
			total += b.floatValue();
		}
		if ( c != null ) {
			count += 1;
			total += c.floatValue();
		}
		if ( count < 1 ) {
			return null;
		}
		return total;
	}

	/**
	 * Get the watts, phase A.
	 * 
	 * @return the watts
	 */
	public Integer getWattsA() {
		return wattsA;
	}

	/**
	 * Set the watts, phase A.
	 * 
	 * @param wattsA
	 *        the watts to set
	 */
	public void setWattsA(Integer wattsA) {
		this.wattsA = wattsA;
	}

	/**
	 * Get the watts, phase B.
	 * 
	 * @return the watts
	 */
	public Integer getWattsB() {
		return wattsB;
	}

	/**
	 * Set the watts, phase B.
	 * 
	 * @param wattsB
	 *        the watts to set
	 */
	public void setWattsB(Integer wattsB) {
		this.wattsB = wattsB;
	}

	/**
	 * Get the watts, phase C.
	 * 
	 * @return the watts
	 */
	public Integer getWattsC() {
		return wattsC;
	}

	/**
	 * Set the watts, phase C.
	 * 
	 * @param wattsC
	 *        the watts to set
	 */
	public void setWattsC(Integer wattsC) {
		this.wattsC = wattsC;
	}

	/**
	 * Get the timestamp.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp.
	 * 
	 * @param timestamp
	 *        the timestamp to set
	 */
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get the frequency.
	 * 
	 * @return the frequency, in Hz
	 */
	@Override
	public Float getFrequency() {
		return frequency;
	}

	/**
	 * Set the frequency.
	 * 
	 * @param frequency
	 *        the frequency to set, in Hz
	 */
	public void setFrequency(Float frequency) {
		this.frequency = frequency;
	}

	/**
	 * Get the power factor, phase A.
	 * 
	 * @return the power factor
	 */
	public Float getPowerFactorA() {
		return powerFactorA;
	}

	/**
	 * Set the power factor, phase A.
	 * 
	 * @param powerFactorA
	 *        the power factor to set
	 */
	public void setPowerFactorA(Float powerFactorA) {
		this.powerFactorA = powerFactorA;
	}

	/**
	 * Get the power factor, phase B.
	 * 
	 * @return the power factor
	 */
	public Float getPowerFactorB() {
		return powerFactorB;
	}

	/**
	 * Set the power factor, phase B.
	 * 
	 * @param powerFactorB
	 *        the power factor to set
	 */
	public void setPowerFactorB(Float powerFactorB) {
		this.powerFactorB = powerFactorB;
	}

	/**
	 * Get the power factor, phase C.
	 * 
	 * @return the power factor
	 */
	public Float getPowerFactorC() {
		return powerFactorC;
	}

	/**
	 * Set the power factor, phase C.
	 * 
	 * @param powerFactorC
	 *        the power factor to set
	 */
	public void setPowerFactorC(Float powerFactorC) {
		this.powerFactorC = powerFactorC;
	}

	/**
	 * Get the voltage, phase A-N.
	 * 
	 * @return the voltage
	 */
	public Float getVoltageAN() {
		return voltageAN;
	}

	/**
	 * Set the voltage, phase A-N.
	 * 
	 * @param voltageAN
	 *        the voltage to set
	 */
	public void setVoltageAN(Float voltageAN) {
		this.voltageAN = voltageAN;
	}

	/**
	 * Get the voltage, phase B-N.
	 * 
	 * @return the voltage
	 */
	public Float getVoltageBN() {
		return voltageBN;
	}

	/**
	 * Set the voltage, phase B-N.
	 * 
	 * @param voltageBN
	 *        the voltage to set
	 */
	public void setVoltageBN(Float voltageBN) {
		this.voltageBN = voltageBN;
	}

	/**
	 * Get the voltage, phase C-N.
	 * 
	 * @return the voltage
	 */
	public Float getVoltageCN() {
		return voltageCN;
	}

	/**
	 * Set the voltage, phase C-N.
	 * 
	 * @param voltageCN
	 *        the voltage to set
	 */
	public void setVoltageCN(Float voltageCN) {
		this.voltageCN = voltageCN;
	}

	/**
	 * Get the AC current, phase A.
	 * 
	 * @return the current, in A
	 */
	public Float getCurrentA() {
		return currentA;
	}

	/**
	 * Set the AC current, phase A.
	 * 
	 * @param currentA
	 *        the current to set, in A
	 */
	public void setCurrentA(Float currentA) {
		this.currentA = currentA;
	}

	/**
	 * Get the AC current, phase B.
	 * 
	 * @return the current, in A
	 */
	public Float getCurrentB() {
		return currentB;
	}

	/**
	 * Set the AC current, phase B.
	 * 
	 * @param currentB
	 *        the current to set, in A
	 */
	public void setCurrentB(Float currentB) {
		this.currentB = currentB;
	}

	/**
	 * Get the AC current, phase C.
	 * 
	 * @return the current, in A
	 */
	public Float getCurrentC() {
		return currentC;
	}

	/**
	 * Set the AC current, phase C.
	 * 
	 * @param currentC
	 *        the current to set
	 */
	public void setCurrentC(Float currentC) {
		this.currentC = currentC;
	}

	/**
	 * Get the charge capacity.
	 * 
	 * @return the charge capacity, in Wh
	 */
	public Long getChargeCapacity() {
		return chargeCapacity;
	}

	/**
	 * Set the charge capacity.
	 * 
	 * @param chargeCapacity
	 *        the charge capacity to set, in Wh
	 */
	public void setChargeCapacity(Long chargeCapacity) {
		this.chargeCapacity = chargeCapacity;
	}

	/**
	 * Get the available charge.
	 * 
	 * @return the available charge, in Wh
	 */
	public Long getAvailableCharge() {
		return availableCharge;
	}

	/**
	 * Set the available charge.
	 * 
	 * @param availableCharge
	 *        the available charge to set, in Wh
	 */
	public void setAvailableCharge(Long availableCharge) {
		this.availableCharge = availableCharge;
	}

	/**
	 * Get the state of health.
	 * 
	 * @return the health, as an integer percentage between 0 and 100
	 */
	public Integer getStateOfHealth() {
		return stateOfHealth;
	}

	/**
	 * Set the state of health.
	 * 
	 * @param stateOfHealth
	 *        the health, as an integer percentage between 0 and 100
	 */
	public void setStateOfHealth(Integer stateOfHealth) {
		this.stateOfHealth = stateOfHealth;
	}

	@Override
	public FeedDataAccessor accessorForPhase(AcPhase phase) {
		if ( phase == AcPhase.Total ) {
			return this;
		}
		return new PhaseFeedDataAccessor(phase);
	}

	@Override
	public FeedDataAccessor reversed() {
		return new ReversedFeedDataAccessor(this);
	}

	@Override
	public Float getCurrent() {
		return sumValue(currentA, currentB, currentC);
	}

	@Override
	public Float getNeutralCurrent() {
		return null;
	}

	@Override
	public Float getVoltage() {
		return avgValue(voltageAN, voltageBN, voltageCN);
	}

	@Override
	public Float getLineVoltage() {
		return null;
	}

	@Override
	public Float getPowerFactor() {
		return avgValue(powerFactorA, powerFactorB, powerFactorC);
	}

	@Override
	public Integer getActivePower() {
		return sumValue(wattsA, wattsB, wattsC);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return null;
	}

	@Override
	public Long getActiveEnergyReceived() {
		return null;
	}

	@Override
	public Integer getApparentPower() {
		return null;
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public Long getApparentEnergyReceived() {
		return null;
	}

	@Override
	public Integer getReactivePower() {
		return null;
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return null;
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return null;
	}

	@Override
	public Instant getDataTimestamp() {
		return getTimestamp();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return null;
	}

	@Override
	public Float getAvailableEnergyPercentage() {
		Long avail = getAvailableEnergy();
		Long capac = getEnergyCapacity();
		if ( avail != null && capac != null && capac.doubleValue() > 0.0 ) {
			return (float) (avail.doubleValue() / capac.doubleValue());
		}
		return null;
	}

	@Override
	public Long getAvailableEnergy() {
		return availableCharge;
	}

	@Override
	public Long getEnergyCapacity() {
		return chargeCapacity;
	}

	@Override
	public Float getStateOfHealthPercentage() {
		Integer soh = getStateOfHealth();
		if ( soh != null ) {
			return soh.floatValue() / 100.0f;
		}
		return null;
	}

	private class PhaseFeedDataAccessor implements FeedDataAccessor {

		private final AcPhase phase;

		private PhaseFeedDataAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return FeedData.this.getDeviceInfo();
		}

		@Override
		public Instant getDataTimestamp() {
			return FeedData.this.getDataTimestamp();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return FeedData.this.accessorForPhase(phase);
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return FeedData.this.reversed();
		}

		@Override
		public Float getFrequency() {
			return FeedData.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			switch (phase) {
				case PhaseA:
					return currentA;

				case PhaseB:
					return currentB;

				case PhaseC:
					return currentC;

				default:
					return FeedData.this.getCurrent();
			}
		}

		@Override
		public Float getNeutralCurrent() {
			return FeedData.this.getNeutralCurrent();
		}

		@Override
		public Float getVoltage() {
			Number n = null;
			switch (phase) {
				case PhaseA:
					n = voltageAN;
					break;

				case PhaseB:
					n = voltageBN;
					break;

				case PhaseC:
					n = voltageCN;
					break;

				default:
					return FeedData.this.getVoltage();
			}
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Float getLineVoltage() {
			return FeedData.this.getLineVoltage();
		}

		@Override
		public Float getPowerFactor() {
			switch (phase) {
				case PhaseA:
					return powerFactorA;

				case PhaseB:
					return powerFactorB;

				case PhaseC:
					return powerFactorC;

				default:
					return FeedData.this.getCurrent();
			}
		}

		@Override
		public Integer getActivePower() {
			switch (phase) {
				case PhaseA:
					return wattsA;

				case PhaseB:
					return wattsB;

				case PhaseC:
					return wattsC;

				default:
					return FeedData.this.getActivePower();
			}
		}

		@Override
		public Integer getApparentPower() {
			return FeedData.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return FeedData.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return FeedData.this.getActiveEnergyDelivered();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return FeedData.this.getActiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return FeedData.this.getReactiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return FeedData.this.getReactiveEnergyReceived();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return FeedData.this.getApparentEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return FeedData.this.getApparentEnergyReceived();
		}

		@Override
		public Float getAvailableEnergyPercentage() {
			return FeedData.this.getAvailableEnergyPercentage();
		}

		@Override
		public Long getAvailableEnergy() {
			return FeedData.this.getAvailableEnergy();
		}

		@Override
		public Long getEnergyCapacity() {
			return FeedData.this.getEnergyCapacity();
		}

		@Override
		public Float getStateOfHealthPercentage() {
			return FeedData.this.getStateOfHealthPercentage();
		}

	}

	private static class ReversedFeedDataAccessor implements FeedDataAccessor {

		private final FeedDataAccessor delegate;

		private ReversedFeedDataAccessor(FeedDataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return delegate.accessorForPhase(phase);
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return delegate;
		}

		@Override
		public Instant getDataTimestamp() {
			return delegate.getDataTimestamp();
		}

		@Override
		public Float getFrequency() {
			return delegate.getFrequency();
		}

		@Override
		public Float getCurrent() {
			return delegate.getCurrent();
		}

		@Override
		public Float getNeutralCurrent() {
			return delegate.getNeutralCurrent();
		}

		@Override
		public Float getVoltage() {
			return delegate.getVoltage();
		}

		@Override
		public Float getLineVoltage() {
			return delegate.getLineVoltage();
		}

		@Override
		public Float getPowerFactor() {
			return delegate.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = delegate.getActivePower();
			return (n != null ? n * -1 : null);
		}

		@Override
		public Integer getApparentPower() {
			return delegate.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			Integer n = delegate.getReactivePower();
			return (n != null ? n * -1 : null);
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return delegate.getActiveEnergyReceived();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return delegate.getActiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return delegate.getReactiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return delegate.getReactiveEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return delegate.getApparentEnergyReceived();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return delegate.getApparentEnergyDelivered();
		}

		@Override
		public Float getAvailableEnergyPercentage() {
			Float avail = delegate.getAvailableEnergyPercentage();
			if ( avail != null ) {
				return 1.0f - avail.floatValue();
			}
			return null;
		}

		@Override
		public Long getAvailableEnergy() {
			Long capacity = delegate.getAvailableEnergy();
			Long avail = delegate.getAvailableEnergy();
			if ( capacity != null && avail != null ) {
				return capacity - avail;
			}
			return null;
		}

		@Override
		public Long getEnergyCapacity() {
			return delegate.getEnergyCapacity();
		}

		@Override
		public Float getStateOfHealthPercentage() {
			return delegate.getStateOfHealthPercentage();
		}

	}
}
