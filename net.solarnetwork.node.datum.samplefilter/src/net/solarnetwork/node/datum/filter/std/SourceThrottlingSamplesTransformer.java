/* ==================================================================
 * SourceThrottlingSamplesTransformer.java - 8/08/2017 2:02:11 PM
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.Identifiable;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link GeneralDatumSamplesTransformer} that can filter out samples based on a
 * basic frequency constraint.
 * 
 * @author matt
 * @version 1.1
 */
public class SourceThrottlingSamplesTransformer extends SamplesTransformerSupport
		implements GeneralDatumSamplesTransformer, SettingSpecifierProvider, Identifiable {

	/**
	 * The default interval at which to save {@code Datum} instances, in
	 * seconds, if not configured otherwise.
	 */
	public static final int DEFAULT_FREQUENCY_SECONDS = 60;

	private int frequencySeconds;

	public SourceThrottlingSamplesTransformer() {
		super();
		setFrequencySeconds(DEFAULT_FREQUENCY_SECONDS);
	}

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
		final String settingKey = settingKey();
		if ( settingKey == null ) {
			log.trace("Filter does not have a UID configured; not filtering: {}", this);
			return samples;
		}

		if ( !sourceIdMatches(datum) ) {
			return samples;
		}

		final String sourceId = datum.getSourceId();

		// load all Datum "last created" settings
		final ConcurrentMap<String, String> createdSettings = loadSettings(settingKey);

		final long now = (datum != null && datum.getCreated() != null ? datum.getCreated().getTime()
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
				log.debug("Datum {} was seen in the past {}s ({}s ago); filtering", datum, offset,
						(now - lastSaveTime) / 1000.0);
			}
			return null;
		}

		log.trace("Datum {} has not been seen in the past {}s; not filtering", datum, offset);

		// save the new setting date
		saveLastSeenSetting(now, settingKey, sourceId, lastSaveSetting, createdSettings);

		return samples;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.samplefilter.throttle";
	}

	@Override
	public String getDisplayName() {
		return "Source Samples Throttler";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);

		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));
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

}
