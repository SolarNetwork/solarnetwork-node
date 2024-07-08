/* ==================================================================
 * GpsDatumDataSource.java - 15/11/2019 4:38:46 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.gps.gpsd;

import static net.solarnetwork.util.StringUtils.delimitedStringFromMap;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.GpsdReportMessage;
import net.solarnetwork.node.io.gpsd.domain.NmeaMode;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdClientConnection;
import net.solarnetwork.node.io.gpsd.service.GpsdClientStatus;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageListener;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.NumberUtils;

/**
 * Datum data source for GPS data collected from a {@link GpsdClientConnection}.
 *
 * @author matt
 * @version 2.2
 */
public class GpsDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider,
		GpsdMessageListener<GpsdReportMessage>, EventHandler {

	private final ConcurrentMap<GpsdMessageType, GpsdReportMessage> messageSamples = new ConcurrentHashMap<>(
			4, 0.9f, 1);

	/**
	 * The default {@code nodeLocationUpdateMaxLatLonErrorMeters} property
	 * value.
	 *
	 * @since 1.2
	 */
	public static final double DEFAULT_MAX_LAT_LON_ERROR_METERS = 10.0;

	private final OptionalFilterableService<GpsdClientConnection> client;
	private OptionalService<LocationService> locationService;
	private String sourceId;
	private boolean updateNodeLocation = false;
	private double nodeLocationUpdateMaxLatLonErrorMeters = DEFAULT_MAX_LAT_LON_ERROR_METERS;

	/**
	 * Constructor.
	 *
	 * @param client
	 *        the client
	 */
	public GpsDatumDataSource(OptionalFilterableService<GpsdClientConnection> client) {
		super();
		this.client = client;
	}

	/**
	 * Call once to initialize after properties configured.
	 */
	public void startup() {
		GpsdClientConnection conn = connection();
		if ( conn != null ) {
			GpsdClientStatus status = conn.getClientStatus();
			handleStatusChange(conn, status);
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		GpsdReportMessage msg = messageSamples.get(GpsdMessageType.TpvReport);
		if ( msg instanceof TpvReportMessage ) {
			return createTpvDatum((TpvReportMessage) msg);
		}
		return null;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		List<NodeDatum> results = new ArrayList<>();
		for ( GpsdReportMessage msg : messageSamples.values() ) {
			NodeDatum d = createDatum(msg);
			if ( d != null ) {
				results.add(d);
			}
		}
		return results;
	}

	private GpsdClientConnection connection() {
		return (client != null ? client.service() : null);
	}

	@Override
	public void onGpsdMessage(GpsdReportMessage message) {
		GpsdMessageType type = (message != null ? message.getMessageType() : null);
		if ( type == null ) {
			return;
		}
		Instant t = message.getTimestamp();
		if ( t == null ) {
			t = Instant.now();
			message = message.withTimestamp(t);
		}
		messageSamples.put(type, message);

		NodeDatum d = createDatum(message);
		if ( d != null ) {
			offerDatumCapturedEvent(d);
		}

		if ( type == GpsdMessageType.TpvReport ) {
			publishNodeLocation((TpvReportMessage) message);
		}
	}

	private void publishNodeLocation(TpvReportMessage message) {
		if ( message.getMode() != NmeaMode.ThreeDimensional ) {
			return;
		}
		if ( nodeLocationUpdateMaxLatLonErrorMeters > 0.0 ) {
			if ( message.getLatitudeError() == null || message.getLatitudeError()
					.doubleValue() > nodeLocationUpdateMaxLatLonErrorMeters ) {
				return;
			}
			if ( message.getLongitudeError() == null || message.getLongitudeError()
					.doubleValue() > nodeLocationUpdateMaxLatLonErrorMeters ) {
				return;
			}
		}
		LocationService locService = OptionalService.service(locationService);
		if ( locService != null ) {
			net.solarnetwork.domain.Location loc = locationForTpvMessage(message);
			if ( loc != null ) {
				try {
					locService.updateNodeLocation(loc);
				} catch ( RuntimeException e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					if ( root instanceof IOException ) {
						log.warn("Communication error updating node location: {}", root.toString());
					} else {
						log.error("Error updating node location: {}", root.toString(), root);
					}
				}
			}
		}
	}

	private net.solarnetwork.domain.Location locationForTpvMessage(TpvReportMessage message) {
		SimpleLocation loc = new SimpleLocation();
		loc.setElevation(NumberUtils.bigDecimalForNumber(message.getAltitude()));
		loc.setLatitude(NumberUtils.bigDecimalForNumber(message.getLatitude()));
		loc.setLongitude(NumberUtils.bigDecimalForNumber(message.getLongitude()));
		return (loc.getElevation() != null && loc.getLatitude() != null && loc.getLongitude() != null
				? loc
				: null);
	}

	private NodeDatum createDatum(GpsdReportMessage message) {
		NodeDatum d = null;
		if ( message instanceof TpvReportMessage ) {
			d = createTpvDatum((TpvReportMessage) message);
		}
		return d;
	}

	private TpvGpsDatum createTpvDatum(TpvReportMessage tpv) {
		if ( tpv == null ) {
			return null;
		}
		final String sourceId = getSourceId();
		if ( sourceId == null ) {
			return null;
		}
		return new TpvGpsDatum(tpv, resolvePlaceholders(sourceId));
	}

	// EventHandler

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("GpsDatumDataSource{");
		GpsdClientConnection conn = connection();
		if ( conn != null ) {
			buf.append("conn=");
			buf.append(conn.getUid());
			buf.append(";");
			buf.append(conn);
		}
		if ( sourceId != null ) {
			if ( conn != null ) {
				buf.append(", ");
			}
			buf.append("sourceId=");
			buf.append(sourceId);
		}
		buf.append("}");
		return buf.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if ( GpsdClientConnection.EVENT_TOPIC_CLIENT_STATUS_CHANGE.equals(event.getTopic()) ) {
			handleStatusChangeEvent(event);
		}
	}

	private void handleStatusChangeEvent(Event event) {
		GpsdClientConnection conn = connection();
		Object eventUid = event.getProperty(UID_PROPERTY);
		if ( conn != null && eventUid != null && eventUid.toString().equals(conn.getUid()) ) {
			GpsdClientStatus status = (GpsdClientStatus) event
					.getProperty(GpsdClientConnection.STATUS_PROPERTY);
			handleStatusChange(conn, status);
		}
	}

	private void handleStatusChange(GpsdClientConnection conn, GpsdClientStatus status) {
		if ( status == GpsdClientStatus.Connected ) {
			// add myself as a listener; this is OK to call multiple times in the case of re-connection
			conn.addMessageListener(GpsdReportMessage.class, this);
		}
	}

	// SettingsSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.gps.gpsd";
	}

	@Override
	public String getDisplayName() {
		return "GPSd Data Source";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("status", getStatusMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("client.propertyFilters['uid']", null, false,
				"(objectClass=net.solarnetwork.node.io.gpsd.service.GpsdClientConnection)"));

		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		results.add(new BasicToggleSettingSpecifier("updateNodeLocation", Boolean.FALSE));
		results.add(new BasicTextFieldSettingSpecifier("nodeLocationUpdateMaxLatLonErrorMeters",
				String.valueOf(DEFAULT_MAX_LAT_LON_ERROR_METERS)));

		return results;
	}

	private String getStatusMessage() {
		GpsdClientConnection conn = connection();
		if ( conn == null ) {
			Map<String, ?> filters = null;
			if ( client instanceof FilterableService ) {
				filters = ((FilterableService) client).getPropertyFilters();
			}
			if ( filters != null && !filters.isEmpty() ) {
				return "No GPSd Connection available matching: " + delimitedStringFromMap(filters);
			}
			return "No GPSd Connection available.";
		}
		GpsdClientStatus status = conn.getClientStatus();
		switch (status) {
			case Closed:
				return "GPSd connection shut down.";

			case Connected:
				return "Connected to GPSd.";

			case ConnectionScheduled:
				return "Reconnecting to GPSd...";

			default:
				return status.toString();
		}
	}

	private String getSampleMessage() {
		StringBuilder buf = new StringBuilder();
		for ( GpsdReportMessage msg : messageSamples.values() ) {
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(msg.toString());
		}
		return buf.toString();
	}

	// Accessor

	/**
	 * Get the source ID to use for returned datum.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Set the location service.
	 *
	 * @return the location service
	 * @since 1.2
	 */
	public OptionalService<LocationService> getLocationService() {
		return locationService;
	}

	/**
	 * Get the location service.
	 *
	 * @param locationService
	 *        the location service to set
	 * @since 1.2
	 */
	public void setLocationService(OptionalService<LocationService> locationService) {
		this.locationService = locationService;
	}

	/**
	 * Get the maximum error meters allowed when publishing the node location.
	 *
	 * @return the meters
	 * @since 1.2
	 */
	public double getNodeLocationUpdateMaxLatLonErrorMeters() {
		return nodeLocationUpdateMaxLatLonErrorMeters;
	}

	/**
	 * Set the maximum error meters allowed when publishing the node location.
	 *
	 * @param nodeLocationUpdateMaxLatLonErrorMeters
	 *        the meters to set
	 * @since 1.2
	 */
	public void setNodeLocationUpdateMaxLatLonErrorMeters(
			double nodeLocationUpdateMaxLatLonErrorMeters) {
		this.nodeLocationUpdateMaxLatLonErrorMeters = nodeLocationUpdateMaxLatLonErrorMeters;
	}

	/**
	 * Get the update node location flag.
	 *
	 * @return {@literal true} to update the node's location based on the GPS
	 *         coordinates received from GPSd
	 * @since 1.2
	 */
	public boolean isUpdateNodeLocation() {
		return updateNodeLocation;
	}

	/**
	 * Set the update node location flag.
	 *
	 * @param updateNodeLocation
	 *        {@literal true} to update the node's location based on the GPS
	 *        coordinates received from GPSd
	 * @since 1.2
	 */
	public void setUpdateNodeLocation(boolean updateNodeLocation) {
		this.updateNodeLocation = updateNodeLocation;
	}

	/**
	 * Get the client.
	 *
	 * @return the client
	 * @since 2.1
	 */
	public final OptionalFilterableService<GpsdClientConnection> getClient() {
		return client;
	}

}
