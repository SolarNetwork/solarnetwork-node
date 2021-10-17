/* ==================================================================
 * BatteryAPISupport.java - 16/02/2016 8:22:50 pm
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

import java.util.ArrayList;
import java.util.List;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * Supporting class for {@link BatteryAPIClient} use.
 * 
 * @author matt
 * @version 2.0
 */
public class BatteryAPISupport extends BaseIdentifiable {

	// the client to use
	private BatteryAPIClient client = new SimpleBatteryAPIClient();

	// an optional EventAdmin service
	private OptionalService<EventAdmin> eventAdmin;

	private String errorMessage;

	/** A class-level logger to use. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * An instance of {@link BatteryData} to support keeping the last-read
	 * values of data in memory.
	 */
	protected BatteryData sample;

	private String getSampleMessage(BatteryData data) {
		if ( data == null || data.getDate() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append(data.getOperationStatusMessage());
		buf.append("; sampled at ").append(data.getDate());
		return buf.toString();
	}

	public List<SettingSpecifier> getSettingSpecifiers() {
		BatteryAPISupport defaults = new BatteryAPISupport();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		String msg = getErrorMessage();
		if ( msg != null ) {
			results.add(new BasicTitleSettingSpecifier("info", msg, true));
		}

		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUid", defaults.getGroupUid()));

		if ( client instanceof SimpleBatteryAPIClient ) {
			SimpleBatteryAPIClient c = (SimpleBatteryAPIClient) client;
			results.add(new BasicTextFieldSettingSpecifier("client.baseURL", c.getBaseURL()));
		}

		return results;
	}

	/**
	 * Get the client to use.
	 * 
	 * @return The configured client.
	 */
	public BatteryAPIClient getClient() {
		return client;
	}

	/**
	 * Set the client to use.
	 * 
	 * @param client
	 *        The client to use.
	 */
	public void setClient(BatteryAPIClient client) {
		this.client = client;
	}

	/**
	 * Get the configured optional EventAdmin service.
	 * 
	 * @return The service.
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an optional {@code EventAdmin} service for posting events with.
	 * 
	 * @param eventAdmin
	 *        The service to use.
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * An error message to use in status messages.
	 * 
	 * @return An error message (or {@literal null}).
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Set an error message to show in status messages.
	 * 
	 * @param errorMessage
	 *        The error message.
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
