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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.GpsdReportMessage;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdClientConnection;
import net.solarnetwork.node.io.gpsd.service.GpsdClientStatus;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageListener;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;

/**
 * Datum data source for GPS data collected from a {@link GpsdClientConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource<GeneralNodeDatum>, MultiDatumDataSource<GeneralNodeDatum>,
		SettingSpecifierProvider, GpsdMessageListener<GpsdReportMessage>, EventHandler {

	private final ConcurrentMap<GpsdMessageType, GpsdReportMessage> messageSamples = new ConcurrentHashMap<>(
			4, 0.9f, 1);
	private final OptionalService<GpsdClientConnection> client;

	private String sourceId;

	/**
	 * Constructor.
	 * 
	 * @param client
	 *        the client
	 */
	public GpsDatumDataSource(OptionalService<GpsdClientConnection> client) {
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
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		GpsdReportMessage msg = messageSamples.get(GpsdMessageType.TpvReport);
		if ( msg instanceof TpvReportMessage ) {
			return createTpvDatum((TpvReportMessage) msg);
		}
		return null;
	}

	@Override
	public Class<? extends GeneralNodeDatum> getMultiDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public Collection<GeneralNodeDatum> readMultipleDatum() {
		List<GeneralNodeDatum> results = new ArrayList<>();
		for ( GpsdReportMessage msg : messageSamples.values() ) {
			GeneralNodeDatum d = createDatum(msg);
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

		GeneralNodeDatum d = createDatum(message);
		if ( d != null ) {
			postDatumCapturedEvent(d);
		}
	}

	private GeneralNodeDatum createDatum(GpsdReportMessage message) {
		GeneralNodeDatum d = null;
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
		TpvGpsDatum d = new TpvGpsDatum(tpv);
		d.setSourceId(resolvePlaceholders(sourceId));
		return d;
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
	public String getSettingUID() {
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
		results.add(new BasicTextFieldSettingSpecifier("client.propertyFilters['UID']", "GPSD"));

		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

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
	 * @param soruceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
