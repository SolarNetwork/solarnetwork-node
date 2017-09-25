/* ==================================================================
 * DatumDataSourceSupport.java - 26/09/2017 9:38:58 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.Identifiable;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.OptionalService;

/**
 * Helper class for {@link net.solarnetwork.node.DatumDataSource} and
 * {@link net.solarnetwork.node.MultiDatumDataSource} implementations to extend.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDataSourceSupport implements Identifiable {

	/**
	 * A global cache of source-based metadata, so only changes to metadata need
	 * be posted.
	 */
	private static final ConcurrentMap<String, GeneralDatumMetadata> SOURCE_METADATA_CACHE = new ConcurrentHashMap<String, GeneralDatumMetadata>(
			4);

	private String uid;
	private String groupUID;
	private MessageSource messageSource;
	private OptionalService<DatumMetadataService> datumMetadataService;
	private OptionalService<EventAdmin> eventAdmin;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Post an {@link Event} for the
	 * {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} topic.
	 * 
	 * @param datum
	 *        the datum that was stored
	 */
	protected final void postDatumCapturedEvent(Datum datum) {
		if ( datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum);
		postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method uses the result of {@link Datum#asSimpleMap()} as the event
	 * properties.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @return the new Event instance
	 */
	protected Event createDatumCapturedEvent(Datum datum) {
		Map<String, ?> props = datum.asSimpleMap();
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
	}

	/**
	 * Post an {@link Event}.
	 * 
	 * <p>
	 * This method only works if a {@link EventAdmin} has been configured via
	 * {@link #setEventAdmin(OptionalService)}. Otherwise the event is silently
	 * ignored.
	 * </p>
	 * 
	 * @param event
	 *        the event to post
	 */
	protected final void postEvent(Event event) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	/**
	 * Add source metadata using the configured {@link DatumMetadataService} (if
	 * available). The metadata will be cached so that subsequent calls to this
	 * method with the same metadata value will not try to re-save the unchanged
	 * value. This method will catch all exceptions and silently discard them.
	 * 
	 * @param sourceId
	 *        the source ID to add metadata to
	 * @param meta
	 *        the metadata to add
	 * @param returns
	 *        <em>true</em> if the metadata was saved successfully, or does not
	 *        need to be updated
	 */
	protected boolean addSourceMetadata(final String sourceId, final GeneralDatumMetadata meta) {
		GeneralDatumMetadata cached = SOURCE_METADATA_CACHE.get(sourceId);
		if ( cached != null && meta.equals(cached) ) {
			// we've already posted this metadata... don't bother doing it again
			log.debug("Source {} metadata already added, not posting again", sourceId);
			return true;
		}
		DatumMetadataService service = null;
		if ( datumMetadataService != null ) {
			service = datumMetadataService.service();
		}
		if ( service == null ) {
			return false;
		}
		try {
			service.addSourceMetadata(sourceId, meta);
			SOURCE_METADATA_CACHE.put(sourceId, meta);
			return true;
		} catch ( Exception e ) {
			log.warn("Error saving source {} metadata: {}", sourceId, e.getMessage());
		}
		return false;
	}

	/**
	 * Get setting specifiers for the {@code uid} and {@code groupUID}
	 * properties.
	 * 
	 * @return list of setting specifiers
	 */
	protected List<SettingSpecifier> getIdentifiableSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", null));
		return results;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This is an alias for {@link #getUID()}.
	 * </p>
	 */
	@Override
	public String getUID() {
		return getUid();
	}

	/**
	 * Get a unique ID for this service.
	 * 
	 * @return the service unique ID
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * Set the unique ID for this service.
	 * 
	 * @param uid
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	/**
	 * Set a unique group ID for this service.
	 * 
	 * @param groupUID
	 *        the group ID to use
	 */
	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	/**
	 * Get the {@link EventAdmin} service.
	 * 
	 * @return the EventAdmin service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an {@link EventAdmin} service to use.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the configured {@link MessageSource}.
	 * 
	 * @return the message source, or {@literal null}
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a {@link MessageSource} to use for resolving localized messages.
	 * 
	 * @param messageSource
	 *        the message source to use
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the configured {@link DatumMetadataService}.
	 * 
	 * @return the service to use
	 */
	public OptionalService<DatumMetadataService> getDatumMetadataService() {
		return datumMetadataService;
	}

	/**
	 * Set a {@link DatumMetadataService} to use for managing datum metadata.
	 * 
	 * @param datumMetadataService
	 *        the service to use
	 */
	public void setDatumMetadataService(OptionalService<DatumMetadataService> datumMetadataService) {
		this.datumMetadataService = datumMetadataService;
	}
}
