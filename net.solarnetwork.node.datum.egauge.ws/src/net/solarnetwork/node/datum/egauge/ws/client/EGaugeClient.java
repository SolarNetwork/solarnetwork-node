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

import java.util.List;
import net.solarnetwork.node.datum.egauge.ws.EGaugeDatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Interface for eGauge clients.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public interface EGaugeClient {

	/**
	 * Retrieve the current reading from an eGauge device.
	 * 
	 * @param source
	 *        The source making the request to get the current datum.
	 * @return the current eGauge readings.
	 */
	EGaugePowerDatum getCurrent(EGaugeDatumDataSource source);

	List<SettingSpecifier> settings(String string);

}
