/* ==================================================================
 * SimpleBatteryAPIClient.java - 16/02/2016 7:10:45 am
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

package net.solarnetwork.node.hw.panasonic.battery;

import java.io.IOException;
import java.io.InputStream;
import net.solarnetwork.node.support.JsonHttpClientSupport;

/**
 * Implementation of {@link BatteryAPIClient}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleBatteryAPIClient extends JsonHttpClientSupport implements BatteryAPIClient {

	private String baseURL = "https://api.panasonic.com/batteryapi";

	@Override
	public BatteryData getCurrentBatteryDataForEmail(String email) throws BatteryAPIException {
		final StringBuilder buf = new StringBuilder();
		appendXWWWFormURLEncodedValue(buf, "EmailID", email);
		buf.insert(0, "?").insert(0, "/BatteryByEmail").insert(0, baseURL);
		final String url = buf.toString();

		try {
			InputStream in = jsonGET(url);
			return getObjectMapper().readValue(in, BatteryData.class);
		} catch ( IOException e ) {
			throw new BatteryAPIException(e);
		}
	}

	/**
	 * Get the base URL to the battery API.
	 * 
	 * @return The base URL.
	 */
	public String getBaseURL() {
		return baseURL;
	}

	/**
	 * Set the base URL to the battery API.
	 * 
	 * @param baseURL
	 *        The base URL.
	 */
	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

}
