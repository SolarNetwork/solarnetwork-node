/* ==================================================================
 * EGaugeClient.java - 9/03/2018 12:32:40 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws.client;

import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * Interface for eGauge clients.
 * 
 * @author maxieduncan
 * @version 2.0
 */
public interface EGaugeClient extends SettingSpecifierProvider {

	/**
	 * Retrieve the current reading from an eGauge device.
	 * 
	 * @return the current eGauge readings.
	 */
	AcDcEnergyDatum getCurrent();

	/**
	 * Get information about a general datum sample.
	 * 
	 * <p>
	 * {@code snap} is presumed to be populated with data collected from this
	 * client.
	 * </p>
	 * 
	 * @param snap
	 *        the data sample to get information about
	 * @return the information string
	 */
	String getSampleInfo(NodeDatum snap);

	/**
	 * Get the source ID the client is configured to populate returned datum
	 * with.
	 * 
	 * @return the datum source ID
	 */
	String getSourceId();

}
