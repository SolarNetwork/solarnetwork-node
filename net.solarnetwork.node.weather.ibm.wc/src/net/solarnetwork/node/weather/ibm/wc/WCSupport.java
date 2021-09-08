/* ===================================================================
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
 * ===================================================================
 */

package net.solarnetwork.node.weather.ibm.wc;

import net.solarnetwork.node.service.support.DatumDataSourceSupport;

/**
 * Simplifies the datum data source classes by providing shared methods.
 * 
 * @author matt frost
 */
public abstract class WCSupport extends DatumDataSourceSupport {

	private String locationIdentifier;
	private WCClient client;
	private String apiKey;
	private String datumPeriod;
	private String language;

	public WCSupport() {
		locationIdentifier = "";
		client = new BasicWCClient();
	}

	//TODO add a dropdown specifier for the period of the data (ie. 7day, 10day)

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getDatumPeriod() {
		return this.datumPeriod;
	}

	public void setDatumPeriod(String datumPeriod) {
		this.datumPeriod = datumPeriod;
	}

	public WCClient getClient() {
		return this.client;
	}

	public void setClient(WCClient client) {
		this.client = client;
	}

	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
