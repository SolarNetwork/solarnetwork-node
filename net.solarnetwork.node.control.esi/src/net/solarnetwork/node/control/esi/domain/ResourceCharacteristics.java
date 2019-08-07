/* ==================================================================
 * ResourceCharacteristics.java - 7/08/2019 2:57:54 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi.domain;

import java.util.Objects;

/**
 * DER characteristic details.
 * 
 * @author matt
 * @version 1.0
 */
public class ResourceCharacteristics {

	private Long loadPowerMax;
	private Float loadPowerFactor;
	private Long supplyPowerMax;
	private Float supplyPowerFactor;
	private Long storageEnergyCapacity;
	private DurationRange responseTime;

	/**
	 * Default constructor.
	 */
	public ResourceCharacteristics() {
		super();
	}

	public ResourceCharacteristics copy() {
		ResourceCharacteristics c = new ResourceCharacteristics();
		c.setLoadPowerMax(getLoadPowerMax());
		c.setLoadPowerFactor(getLoadPowerFactor());
		c.setSupplyPowerMax(getSupplyPowerMax());
		c.setSupplyPowerFactor(getSupplyPowerFactor());
		c.setStorageEnergyCapacity(getStorageEnergyCapacity());

		DurationRange d = getResponseTime();
		if ( d != null ) {
			c.setResponseTime(d.copy());
		}
		return c;
	}

	@Override
	public int hashCode() {
		return Objects.hash(loadPowerFactor, loadPowerMax, responseTime, storageEnergyCapacity,
				supplyPowerFactor, supplyPowerMax);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof ResourceCharacteristics) ) {
			return false;
		}
		ResourceCharacteristics other = (ResourceCharacteristics) obj;
		return Objects.equals(loadPowerFactor, other.loadPowerFactor)
				&& Objects.equals(loadPowerMax, other.loadPowerMax)
				&& Objects.equals(responseTime, other.responseTime)
				&& Objects.equals(storageEnergyCapacity, other.storageEnergyCapacity)
				&& Objects.equals(supplyPowerFactor, other.supplyPowerFactor)
				&& Objects.equals(supplyPowerMax, other.supplyPowerMax);
	}

	@Override
	public String toString() {
		return "ResourceCharacteristics{loadPowerMax=" + loadPowerMax + ", loadPowerFactor="
				+ loadPowerFactor + ", supplyPowerMax=" + supplyPowerMax + ", supplyPowerFactor="
				+ supplyPowerFactor + ", storageEnergyCapacity=" + storageEnergyCapacity
				+ ", responseTime=" + responseTime + "}";
	}

	/**
	 * Get the maximum load this resource can demand, in W.
	 * 
	 * @return the power maximum
	 */
	public Long getLoadPowerMax() {
		return loadPowerMax;
	}

	/**
	 * Set the maximum load this resource can demand, in W.
	 * 
	 * @param loadPowerMax
	 *        the power maximum to set
	 */
	public void setLoadPowerMax(Long loadPowerMax) {
		this.loadPowerMax = loadPowerMax;
	}

	/**
	 * Get the maximum load this resource can demand, in W.
	 * 
	 * @return the power maximum, or {@literal 0} if not set
	 */
	public Long loadPowerMax() {
		Long v = getLoadPowerMax();
		return (v != null ? v : 0L);
	}

	/**
	 * Get the expected power factor of load, between -1..1.
	 * 
	 * @return the power factor
	 */
	public Float getLoadPowerFactor() {
		return loadPowerFactor;
	}

	/**
	 * Set the maximum supply resource can offer, in W.
	 * 
	 * @param loadPowerFactor
	 *        the loadPowerFactor to set
	 */
	public void setLoadPowerFactor(Float loadPowerFactor) {
		this.loadPowerFactor = loadPowerFactor;
	}

	/**
	 * Get the expected power factor of load, between -1..1.
	 * 
	 * @return the power factor, or {@literal 0} if not set
	 */
	public Float loadPowerFactor() {
		Float v = getLoadPowerFactor();
		return (v != null ? v : 0f);
	}

	/**
	 * Get the maximum supply resource can offer, in W.
	 * 
	 * @return the power maximum
	 */
	public Long getSupplyPowerMax() {
		return supplyPowerMax;
	}

	/**
	 * Set the maximum supply resource can offer, in W.
	 * 
	 * @param supplyPowerMax
	 *        the power maximum to set
	 */
	public void setSupplyPowerMax(Long supplyPowerMax) {
		this.supplyPowerMax = supplyPowerMax;
	}

	/**
	 * Get the maximum supply resource can offer, in W.
	 * 
	 * @return the power maximum, or {@literal 0} if not set
	 */
	public Long supplyPowerMax() {
		Long v = getSupplyPowerMax();
		return (v != null ? v : 0L);
	}

	/**
	 * Get the expected power factor of supply, between -1..1.
	 * 
	 * @return the power factor
	 */
	public Float getSupplyPowerFactor() {
		return supplyPowerFactor;
	}

	/**
	 * Set the expected power factor of supply, between -1..1.
	 * 
	 * @param supplyPowerFactor
	 *        the power factor to set
	 */
	public void setSupplyPowerFactor(Float supplyPowerFactor) {
		this.supplyPowerFactor = supplyPowerFactor;
	}

	/**
	 * Get the expected power factor of supply, between -1..1.
	 * 
	 * @return the power factor, or {@literal 0} if not set
	 */
	public Float supplyPowerFactor() {
		Float v = getSupplyPowerFactor();
		return (v != null ? v : 0f);
	}

	/**
	 * Get the theoretical storage capacity of this resource, in Wh.
	 * 
	 * @return the capacity
	 */
	public Long getStorageEnergyCapacity() {
		return storageEnergyCapacity;
	}

	/**
	 * Set the theoretical storage capacity of this resource, in Wh.
	 * 
	 * @param storageEnergyCapacity
	 *        the capacity to set
	 */
	public void setStorageEnergyCapacity(Long storageEnergyCapacity) {
		this.storageEnergyCapacity = storageEnergyCapacity;
	}

	/**
	 * Get the theoretical storage capacity of this resource, in Wh.
	 * 
	 * @return the capacity, or {@literal 0} if not set
	 */
	public Long storageEnergyCapacity() {
		Long v = getStorageEnergyCapacity();
		return (v != null ? v : 0L);
	}

	/**
	 * Get the expected minimum/maximum response time to start/finish executing
	 * load or supply changes.
	 * 
	 * @return the response time
	 */
	public DurationRange getResponseTime() {
		return responseTime;
	}

	/**
	 * Set the expected minimum/maximum response time to start/finish executing
	 * load or supply changes.
	 * 
	 * @param responseTime
	 *        the value to set
	 */
	public void setResponseTime(DurationRange responseTime) {
		this.responseTime = responseTime;
	}

	/**
	 * Get the response time details, creating a new one if it doesn't already
	 * exist.
	 * 
	 * @return the response time details
	 */
	public DurationRange responseTime() {
		DurationRange e = getResponseTime();
		if ( e == null ) {
			e = new DurationRange();
			setResponseTime(e);
		}
		return e;
	}

}
