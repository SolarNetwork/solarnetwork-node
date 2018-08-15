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

import org.springframework.context.MessageSource;
import net.solarnetwork.node.domain.Datum;

/**
 * Simplifies the datum data source classes by providing shared methods.
 * 
 * @author matt frost
 *
 * @param <T>
 */
public abstract class WCSupport<T extends Datum> {

	private String uid;
	private String groupUID;
	private String locationIdentifier;
	private WCClient client;
	private String apiKey;
	private String datumPeriod;
	private MessageSource messageSource;
	private String language;

	public WCSupport() {
		locationIdentifier = "";
		client = new BasicWCClient();
	}

	//TODO add a dropdown specifier for the period of the data (ie. 7day, 10day)

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getUID() {
		return getUid();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

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

	/**
	 * Get a {@link MessageSource} for supporting message resolution.
	 * 
	 * @return the message source
	 */

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public WCClient getClient() {
		return this.client;
	}

	public void setClient(WCClient client) {
		this.client = client;
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
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
