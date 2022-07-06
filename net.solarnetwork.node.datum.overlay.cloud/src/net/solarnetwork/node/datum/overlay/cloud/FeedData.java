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

/**
 * Feed data (aggregate of all resources within feed).
 * 
 * @author matt
 * @version 1.0
 */
public class FeedData {

	private Instant timestamp;
	private Float frequency;
	private Float powerFactorA;
	private Float powerFactorB;
	private Float powerFactorC;
	private Float voltageAN;
	private Float voltageBN;
	private Float voltageCN;
	private Double currentA;
	private Double currentB;
	private Double currentC;
	private Long chargeCapacity; // Wh
	private Long availableCharge; // Wh

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
	public Double getCurrentA() {
		return currentA;
	}

	/**
	 * Set the AC current, phase A.
	 * 
	 * @param currentA
	 *        the current to set, in A
	 */
	public void setCurrentA(Double currentA) {
		this.currentA = currentA;
	}

	/**
	 * Get the AC current, phase B.
	 * 
	 * @return the current, in A
	 */
	public Double getCurrentB() {
		return currentB;
	}

	/**
	 * Set the AC current, phase B.
	 * 
	 * @param currentB
	 *        the current to set, in A
	 */
	public void setCurrentB(Double currentB) {
		this.currentB = currentB;
	}

	/**
	 * Get the AC current, phase C.
	 * 
	 * @return the current, in A
	 */
	public Double getCurrentC() {
		return currentC;
	}

	/**
	 * Set the AC current, phase C.
	 * 
	 * @param currentC
	 *        the current to set
	 */
	public void setCurrentC(Double currentC) {
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

}
