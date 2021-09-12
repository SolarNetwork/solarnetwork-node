/* ==================================================================
 * ThrottlingDatumFilterService.java - 8/08/2017 2:02:11 PM
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

package net.solarnetwork.node.datum.filter.std;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link GeneralDatumSamplesTransformer} that can filter out samples based on a
 * basic frequency constraint.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class ThrottlingDatumFilterService extends DatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, SettingsChangeObserver {

	/**
	 * The default interval at which to save {@code Datum} instances, in
	 * seconds, if not configured otherwise.
	 */
	public static final int DEFAULT_FREQUENCY_SECONDS = 60;

	/** The default {@code settingUid} property value. */
	public static final String DEFAULT_SETTING_UID = "net.solarnetwork.node.datum.samplefilter.throttle";

	private int frequencySeconds;
	private String settingUid = DEFAULT_SETTING_UID;

	public ThrottlingDatumFilterService() {
		super();
		setFrequencySeconds(DEFAULT_FREQUENCY_SECONDS);
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> params) {
		final long start = incrementInputStats();
		final String settingKey = settingKey();
		if ( settingKey == null ) {
			log.trace("Filter does not have a UID configured; not filtering: {}", this);
			incrementIgnoredStats(start);
			return samples;
		}

		if ( !(sourceIdMatches(datum) && operationalModeMatches()) ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final String sourceId = datum.getSourceId();

		// load all Datum "last created" settings
		final ConcurrentMap<String, String> createdSettings = loadSettings(settingKey);

		final long now = (datum != null && datum.getTimestamp() != null
				? datum.getTimestamp().toEpochMilli()
				: System.currentTimeMillis());
		final long offset = frequencySeconds * 1000L;

		boolean filter = false;
		String lastSaveSetting = createdSettings.get(sourceId);
		long lastSaveTime = (lastSaveSetting != null ? Long.valueOf(lastSaveSetting, 16) : 0);
		if ( lastSaveTime > 0 && lastSaveTime + offset > now ) {
			filter = true;
		}

		if ( filter ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Throttle filter [{}] filtering source [{}] seen the past {}s ({}s ago)",
						getUid(), sourceId, offset, (now - lastSaveTime) / 1000.0);
			}
			incrementStats(start, samples, null);
			return null;
		}

		log.trace("Throttle filter [{}] has not seen source [{}] in the past {}s; not filtering",
				getUid(), sourceId, offset);

		// save the new setting date
		saveLastSeenSetting(now, settingKey, sourceId, lastSaveSetting, createdSettings);
		incrementStats(start, samples, samples);
		return samples;
	}

	@Override
	public String getSettingUid() {
		return settingUid;
	}

	@Override
	public String getDisplayName() {
		return "Source Samples Throttler";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = baseIdentifiableSettings();
		populateBaseSampleTransformSupportSettings(results);
		populateStatusSettings(results);
		results.add(new BasicTextFieldSettingSpecifier("frequencySeconds",
				String.valueOf(DEFAULT_FREQUENCY_SECONDS)));

		return results;
	}

	/**
	 * Set the frequency seconds to limit samples to.
	 * 
	 * @param frequencySeconds
	 *        the frequency seconds to set
	 */
	public void setFrequencySeconds(int frequencySeconds) {
		this.frequencySeconds = frequencySeconds;
	}

	/**
	 * The setting UID to use.
	 * 
	 * @param settingUid
	 *        the setting UID
	 */
	public void setSettingUid(String settingUid) {
		this.settingUid = settingUid;
	}

}
