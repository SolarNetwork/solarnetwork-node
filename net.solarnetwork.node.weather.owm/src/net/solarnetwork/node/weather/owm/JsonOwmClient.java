/* ==================================================================
 * JsonOwmClient.java - 17/09/2018 7:45:49 AM
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

package net.solarnetwork.node.weather.owm;

/**
 * JSON implementation of {@link OwmClient}
 * 
 * @author matt
 * @version 1.0
 */
public class JsonOwmClient implements OwmClient {

	private String apiKey;

	/**
	 * Default constructor.
	 */
	public JsonOwmClient() {
		super();
	}

	@Override
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * Set the OWM API key to use.
	 * 
	 * @param apiKey
	 *        the API key
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

}
