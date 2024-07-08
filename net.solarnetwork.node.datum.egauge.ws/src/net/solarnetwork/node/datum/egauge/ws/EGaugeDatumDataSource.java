/* ==================================================================
 * EGaugeXMLDatumDataSource.java - Oct 2, 2011 8:50:13 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws;

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeClient;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * Web service based support for eGauge inverters.
 *
 * <p>
 * Needs to be configured with an {@link EGaugeClient} such as
 * {@link XmlEGaugeClient} to retrieve the content to be stored.
 * </p>
 *
 * @author maxieduncan
 * @author matt
 * @version 2.1
 */
public class EGaugeDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/**
	 * The client that should be used to retrieve the eGauge data from the
	 * {@code host}.
	 */
	private EGaugeClient client;

	/** The time that the results should be cached in milliseconds. */
	private long sampleCacheMs = 5000;

	/**
	 * Used to store error details when the client fails to access eGauge
	 * content.
	 */
	private Throwable sampleException;

	private final AtomicReference<CachedResult<AcDcEnergyDatum>> sampleCache = new AtomicReference<>();

	/**
	 * Constructor.
	 */
	public EGaugeDatumDataSource() {
		super();
	}

	private AcDcEnergyDatum getCurrentSample() {
		// First check for a cached sample
		CachedResult<AcDcEnergyDatum> cache = sampleCache.get();
		if ( cache != null && cache.isValid() ) {
			return cache.getResult();
		}

		// Cache has expired so initiate new instance and cache
		AcDcEnergyDatum datum = null;
		try {
			datum = getClient().getCurrent();
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}

			// Keep track of the root exception for reporting
			sampleException = root;
			throw e;
		}

		if ( datum != null ) {
			setSampleCache(cache, datum);
		}
		return datum;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		String sourceId = resolvePlaceholders(client != null ? client.getSourceId() : null);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	private void setSampleCache(CachedResult<AcDcEnergyDatum> cache, AcDcEnergyDatum datum) {
		sampleCache.compareAndSet(cache,
				new CachedResult<>(datum, sampleCacheMs, TimeUnit.MILLISECONDS));
	}

	@Override
	public String toString() {
		return "EGaugeDatumDataSource{client=" + client + ", sampleCacheMs=" + sampleCacheMs
				+ ", sampleException=" + sampleException + "}";
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		return getCurrentSample();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.egauge.ws";
	}

	@Override
	public String getDisplayName() {
		return "eGauge web service data source";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		EGaugeDatumDataSource defaults = new EGaugeDatumDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.sampleCacheMs)));

		results.addAll(getClientSettings());

		return results;
	}

	/**
	 * Get the client settings.
	 *
	 * @return the client settings
	 */
	protected List<SettingSpecifier> getClientSettings() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		List<SettingSpecifier> clientSettings = getClient().getSettingSpecifiers();
		if ( clientSettings != null ) {
			for ( SettingSpecifier clientSetting : clientSettings ) {
				if ( clientSetting instanceof MappableSpecifier ) {
					results.add(((MappableSpecifier) clientSetting).mappedTo("client."));
				} else {
					results.add(clientSetting);
				}
			}
		}

		return results;
	}

	/**
	 * Get an informational status message.
	 *
	 * @return A status message.
	 */
	public String getInfoMessage() {
		AcDcEnergyDatum snap = null;
		try {
			snap = getCurrentSample();
		} catch ( Exception e ) {
			// we must ignore exceptions here
		}
		StringBuilder buf = new StringBuilder();
		Throwable t = sampleException;
		if ( t != null ) {
			buf.append("Error communicating with eGauge inverter: ").append(t.getMessage());
		}
		if ( snap != null ) {
			String info = getClient().getSampleInfo(snap);
			if ( info != null ) {
				if ( buf.length() > 0 ) {
					buf.append("; ");
				}
				buf.append(info);
			}
			if ( snap.getTimestamp() != null ) {
				if ( buf.length() > 0 ) {
					buf.append("; ");
				}
				buf.append(formatForLocalDisplay(snap.getTimestamp()));
			}
		}
		return (buf.length() < 1 ? "N/A" : buf.toString());
	}

	/**
	 * Get the sample cache TTL, in milliseconds.
	 *
	 * @return the TTL, in milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache TTL, in milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the TTL in milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the client.
	 *
	 * @return the client
	 */
	public EGaugeClient getClient() {
		return client;
	}

	/**
	 * Set the client.
	 *
	 * @param client
	 *        the client
	 */
	public void setClient(EGaugeClient client) {
		this.client = client;
	}

}
