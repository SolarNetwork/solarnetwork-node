/* ==================================================================
 * SMAPowerDatum.java - Jul 3, 2014 7:09:14 AM
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

package net.solarnetwork.node.power.sma.yasdi4j;

import java.time.Instant;
import java.util.Map;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;

/**
 * Extension of {@link SimpleAcDcEnergyDatum} with SMA-specific details.
 *
 * @author matt
 * @version 3.0
 */
public class SMAPowerDatum extends SimpleAcDcEnergyDatum {

	private static final long serialVersionUID = 7538663242395333405L;

	/**
	 * Constructor.
	 *
	 * @param sourceId
	 *        the source ID
	 */
	public SMAPowerDatum(String sourceId) {
		super(sourceId, Instant.now(), new DatumSamples());
	}

	/**
	 * Get channel data as a string.
	 *
	 * @param key
	 *        the channel name to get
	 * @return the value
	 */
	private String getStringChannelData(String key) {
		return getSamples().getStatusSampleString(key);
	}

	/**
	 * Set a channel data value.
	 *
	 * @param key
	 *        the channel name
	 * @param value
	 *        the value to set
	 */
	private void setChannelDataValue(String key, Object value) {
		getSamples().putStatusSampleValue(key, value);
	}

	/**
	 * Get the status message.
	 *
	 * @return the message
	 */
	public String getStatusMessage() {
		return getStringChannelData("Op.Health");
	}

	/**
	 * Set the status message.
	 *
	 * @param statusMessage
	 *        the message to set
	 */
	public void setStatusMessage(String statusMessage) {
		setChannelDataValue("Op.Health", statusMessage);
	}

	/**
	 * Get the fault message.
	 *
	 * @return the fault message
	 */
	public String getFaultMessage() {
		return getStringChannelData("Error");
	}

	/**
	 * Set the fault message.
	 *
	 * @param faultMessage
	 *        the message to set
	 */
	public void setFaultMessage(String faultMessage) {
		setChannelDataValue("Error", faultMessage);
	}

	/**
	 * Get the channel data.
	 *
	 * @return the data
	 */
	public Map<String, Object> getChannelData() {
		return (getSamples() != null ? getSamples().getStatus() : null);
	}

	/**
	 * Set the channel data.
	 *
	 * @param channelData
	 *        the data to set
	 */
	public void setChannelData(Map<String, Object> channelData) {
		getSamples().setStatus(channelData);
	}

}
