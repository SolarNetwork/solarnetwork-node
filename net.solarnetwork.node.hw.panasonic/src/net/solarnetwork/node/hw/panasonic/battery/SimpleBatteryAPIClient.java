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
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;

/**
 * Implementation of {@link BatteryAPIClient}.
 * 
 * @author matt
 * @version 2.0
 */
public class SimpleBatteryAPIClient extends JsonHttpClientSupport implements BatteryAPIClient {

	private String baseURL = "https://api.panasonic.com/batteryapi";

	/** The returned code for a successful response. */
	public static final int CODE_OK = 200;

	/** The returned code for a device or email not found. */
	public static final int CODE_NOT_FOUND = 404;

	private BatteryData getBatteryData(final String url) {
		try {
			InputStream in = jsonGET(url);
			JsonNode json = getObjectMapper().readTree(in);
			JsonNode codeNode = json.get("Code");
			int code = (codeNode != null ? codeNode.intValue() : 0);
			if ( code != CODE_OK ) {
				throw new BatteryAPIException(code, "Server returned error code " + code);
			}
			return getObjectMapper().treeToValue(json, BatteryData.class);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public BatteryData getCurrentBatteryDataForEmail(final String email) throws BatteryAPIException {
		final StringBuilder buf = new StringBuilder();
		appendXWWWFormURLEncodedValue(buf, "EmailID", email);
		buf.insert(0, "?").insert(0, "/BatteryByEmail").insert(0, baseURL);
		final String url = buf.toString();
		return getBatteryData(url);
	}

	@Override
	public BatteryData getCurrentBatteryDataForDevice(final String deviceID) throws BatteryAPIException {
		final StringBuilder buf = new StringBuilder();
		appendXWWWFormURLEncodedValue(buf, "BatteryID", deviceID);
		buf.insert(0, "?").insert(0, "/BatteryByID").insert(0, baseURL);
		final String url = buf.toString();
		return getBatteryData(url);
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
