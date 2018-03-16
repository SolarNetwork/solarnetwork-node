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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeClient;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.util.CachedResult;

/**
 * Web service based support for eGauge inverters. Needs to be configured with
 * an {@link EGaugeClient} such as {@link XmlEGaugeClient} to retrieve the
 * content to be stored.
 * 
 * <p>
 * If the {@code client} configuration is the same, it should be possible to
 * share a single client between multiple instances and just configure the
 * {@code host} and {@code sourceId} properties to use different sources.
 * </p>
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class EGaugeDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource<GeneralNodePVEnergyDatum>, SettingSpecifierProvider {

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

	private AtomicReference<CachedResult<EGaugePowerDatum>> sampleCache = new AtomicReference<>();

	private EGaugePowerDatum getCurrentSample() {
		// First check for a cached sample
		CachedResult<EGaugePowerDatum> cache = sampleCache.get();
		if ( cache != null && cache.isValid() ) {
			return cache.getResult();
		}

		// Cache has expired so initiate new instance and cache
		EGaugePowerDatum datum = null;
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

	//	

	private void setSampleCache(CachedResult<EGaugePowerDatum> cache, EGaugePowerDatum datum) {
		sampleCache.compareAndSet(cache,
				new CachedResult<EGaugePowerDatum>(datum, sampleCacheMs, TimeUnit.MILLISECONDS));
	}

	@Override
	public String toString() {
		return "EGaugeDatumDataSource [client=" + client + ", sampleCacheMs=" + sampleCacheMs
				+ ", sampleException=" + sampleException + "]";
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getDatumType() {
		return EGaugePowerDatum.class;
	}

	@Override
	public EGaugePowerDatum readCurrentDatum() {
		return getCurrentSample();
	}

	public void init() {
		// nothing to do
	}

	@Override
	public String getSettingUID() {
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

		results.add(new BasicGroupSettingSpecifier("client", getClientSettings()));

		return results;
	}

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
		EGaugePowerDatum snap = null;
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
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append((getClient().getSampleInfo(snap)));
			buf.append(String.format("%tc", snap.getCreated()));
		}
		return (buf.length() < 1 ? "N/A" : buf.toString());
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	public EGaugeClient getClient() {
		return client;
	}

	public void setClient(EGaugeClient client) {
		this.client = client;
	}

}
