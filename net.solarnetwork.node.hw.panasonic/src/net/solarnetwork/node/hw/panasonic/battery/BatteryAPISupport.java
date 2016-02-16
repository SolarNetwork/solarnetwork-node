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
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import org.joda.time.format.DateTimeFormat;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supporting class for {@link BatteryAPIClient} use.
 * 
 * @author matt
 * @version 1.0
 */
public class BatteryAPISupport {

	// the client to use
	private BatteryAPIClient client = new SimpleBatteryAPIClient();

	// an optional EventAdmin service
	private OptionalService<EventAdmin> eventAdmin;

	private String uid;
	private String groupUID;
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
		buf.append("; sampled at ").append(DateTimeFormat.forStyle("LS").print(data.getDate()));
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
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));

		if ( client instanceof SimpleBatteryAPIClient ) {
			SimpleBatteryAPIClient c = (SimpleBatteryAPIClient) client;
			results.add(new BasicTextFieldSettingSpecifier("client.baseURL", c.getBaseURL()));
		}

		return results;
	}

	/**
	 * Post a {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link #createDatumCapturedEvent(Datum, Class)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param datum
	 *        the {@link Datum} to post the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @since 1.3
	 */
	protected final void postDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum, eventDatumType);
		ea.postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method will populate all simple properties of the given
	 * {@link Datum} into the event properties, along with the
	 * {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE}.
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @return the new Event instance
	 * @since 1.3
	 */
	protected Event createDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(datum, null);
		props.put(DatumDataSource.EVENT_DATUM_CAPTURED_DATUM_TYPE, eventDatumType.getName());
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
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
	 * Get a unique ID for this service.
	 * 
	 * @return The unique ID.
	 */
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * Get a unique ID for this service. This is an alias for
	 * {@link BatteryAPISupport#getUid()}.
	 * 
	 * @return The unique ID.
	 */
	public String getUID() {
		return getUid();
	}

	/**
	 * Get a group ID to use for this service.
	 * 
	 * @return The group ID.
	 */
	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	/**
	 * An error message to use in status messages.
	 * 
	 * @return An error message (or <em>null</em>).
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
