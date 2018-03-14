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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig.EGaugeReadingType;
import net.solarnetwork.node.datum.egauge.ws.client.EGaugeClient;
import net.solarnetwork.node.datum.egauge.ws.client.XmlEGaugeClient;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
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

	/** The ID that identifies the source. */
	private String sourceId;
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

	/** The list of property/register configurations. */
	private EGaugePropertyConfig[] propertyConfigs;

	private AtomicReference<CachedResult<EGaugePowerDatum>> sampleCache = new AtomicReference<>();
	private final Map<String, Long> validationCache = new HashMap<String, Long>(4);

	private EGaugePowerDatum getCurrentSample() {
		// First check for a cached sample
		CachedResult<EGaugePowerDatum> cache = sampleCache.get();
		if ( cache != null && cache.isValid() ) {
			return cache.getResult();
		}

		// Cache has expired so initiate new instance and cache
		EGaugePowerDatum datum = null;
		try {
			datum = getClient().getCurrent(this);
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
		return "EGaugeDatumDataSource [sourceId=" + sourceId + ", client=" + client + ", sampleCacheMs="
				+ sampleCacheMs + ", sampleException=" + sampleException + ", propertyConfigs="
				+ Arrays.toString(propertyConfigs) + "]";
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getDatumType() {
		return EGaugePowerDatum.class;
	}

	@Override
	public EGaugePowerDatum readCurrentDatum() {
		return getCurrentSample();
	}

	@Override
	public String getUID() {
		return getSourceId();
	}

	public void init() {
		if ( getPropertyConfigs() == null ) {
			setPropertyConfigs(new EGaugePropertyConfig[] {
					new EGaugePropertyConfig("generation", "Solar+", EGaugeReadingType.INSTANTANEOUS),
					new EGaugePropertyConfig("consumption", "Grid", EGaugeReadingType.INSTANTANEOUS) });
		}
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
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", null));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.sampleCacheMs)));

		results.add(new BasicGroupSettingSpecifier("client", getClient().settings("client.")));

		EGaugePropertyConfig[] confs = getPropertyConfigs();
		List<EGaugePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<EGaugePropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propertyConfigs", confsList,
				new SettingsUtil.KeyedListCallback<EGaugePropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(EGaugePropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								EGaugePropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

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
			buf.append(snap.getSampleInfo(getPropertyConfigs()));
			buf.append(String.format("%tc", snap.getCreated()));
		}
		return (buf.length() < 1 ? "N/A" : buf.toString());
	}

	/**
	 * Get the configured source ID.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID value to assign to the collected data.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
		validationCache.clear();
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

	/**
	 * @return the propertyConfig
	 */
	public EGaugePropertyConfig[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * @param propertyConfigs
	 *        the propertyConfig to set
	 */
	public void setPropertyConfigs(EGaugePropertyConfig[] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropertyConfigsCount() {
		EGaugePropertyConfig[] confs = this.propertyConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propertyConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ModbusPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propIncludes} elements.
	 */
	public void setPropertyConfigsCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		EGaugePropertyConfig[] confs = this.propertyConfigs;
		int lCount = (confs == null ? 0 : confs.length);
		if ( lCount != count ) {
			EGaugePropertyConfig[] newIncs = new EGaugePropertyConfig[count];
			if ( confs != null ) {
				System.arraycopy(confs, 0, newIncs, 0, Math.min(count, confs.length));
			}
			for ( int i = 0; i < count; i++ ) {
				if ( newIncs[i] == null ) {
					newIncs[i] = new EGaugePropertyConfig();
				}
			}
			this.propertyConfigs = newIncs;
		}
	}

}
