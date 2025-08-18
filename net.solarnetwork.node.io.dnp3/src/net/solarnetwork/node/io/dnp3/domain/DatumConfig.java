/* ==================================================================
 * DatumConfig.java - 19/08/2025 6:13:54 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * A collection of measurement configurations for a single source ID.
 *
 * @author matt
 * @version 1.0
 */
public class DatumConfig {

	private String sourceId;
	private Duration pollFrequency;
	private MeasurementConfig[] measurementConfigs;

	/**
	 * Constructor.
	 */
	public DatumConfig() {
		super();
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(6);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "pollFrequencySeconds", null));

		MeasurementConfig[] measConfs = getMeasurementConfigs();
		List<MeasurementConfig> measConfsList = (measConfs != null ? Arrays.asList(measConfs)
				: Collections.emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier(prefix + "measurementConfigs",
				measConfsList, new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MeasurementConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MeasurementConfig.clientSettings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return results;
	}

	/**
	 * Test if this configuration is valid.
	 *
	 * <p>
	 * This will verify that a non-empty source ID is configured and at least
	 * one valid measurement configuration exists.
	 * </p>
	 *
	 * @return {@code true} if this configuration is valid
	 */
	public boolean isValid() {
		final String sourceId = getSourceId();
		final MeasurementConfig[] measConfigs = getMeasurementConfigs();
		if ( sourceId == null || sourceId.isEmpty() || measConfigs == null || measConfigs.length < 1 ) {
			return false;
		}
		// find at least one valid measurement configuration
		for ( MeasurementConfig measConfig : measConfigs ) {
			if ( measConfig.isValidForClient() ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the poll frequency.
	 *
	 * @return the poll frequency
	 */
	public Duration getPollFrequency() {
		return pollFrequency;
	}

	/**
	 * Set the poll frequency.
	 *
	 * @param pollFrequency
	 *        the poll frequency to set
	 */
	public void setPollFrequency(Duration pollFrequency) {
		this.pollFrequency = pollFrequency;
	}

	/**
	 * Get the poll frequency, as seconds.
	 *
	 * @return the poll frequency in seconds
	 */
	public Long getPollFrequencySeconds() {
		Duration dur = getPollFrequency();
		return (dur != null ? dur.toSeconds() : null);
	}

	/**
	 * Set the poll frequency, in seconds.
	 *
	 * @param seconds
	 *        the poll frequency to set, in seconds
	 */
	public void setPollFrequencySeconds(Long seconds) {
		Duration dur = (seconds != null ? Duration.ofSeconds(seconds) : null);
		setPollFrequency(dur);
	}

	/**
	 * Get the measurement configurations.
	 *
	 * @return the measurement configurations
	 */
	public MeasurementConfig[] getMeasurementConfigs() {
		return measurementConfigs;
	}

	/**
	 * Set the measurement configurations to use.
	 *
	 * @param measurementConfigs
	 *        the configurations to use
	 */
	public void setMeasurementConfigs(MeasurementConfig[] measurementConfigs) {
		this.measurementConfigs = measurementConfigs;
	}

	/**
	 * Get the number of configured {@code measurementConfigs} elements.
	 *
	 * @return the number of {@code measurementConfigs} elements
	 */
	public int getMeasurementConfigsCount() {
		final MeasurementConfig[] confs = getMeasurementConfigs();
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code MeasurementConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MeasurementConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code measurementConfigs} elements.
	 */
	public void setMeasurementConfigsCount(int count) {
		this.measurementConfigs = ArrayUtils.arrayWithLength(this.measurementConfigs, count,
				MeasurementConfig.class, null);
	}

}
