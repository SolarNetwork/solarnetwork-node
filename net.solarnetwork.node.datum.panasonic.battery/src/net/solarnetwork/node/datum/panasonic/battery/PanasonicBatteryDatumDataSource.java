/* ==================================================================
 * PanasonicBatteryDatumDataSource.java - 16/02/2016 8:19:50 pm
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

package net.solarnetwork.node.datum.panasonic.battery;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.domain.datum.EnergyStorageDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.panasonic.battery.BatteryAPIException;
import net.solarnetwork.node.hw.panasonic.battery.BatteryAPISupport;
import net.solarnetwork.node.hw.panasonic.battery.BatteryData;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link EnergyStorageDatum} with
 * the Panasonic Battery API.
 *
 * @author matt
 * @version 2.1
 */
public class PanasonicBatteryDatumDataSource extends BatteryAPISupport
		implements DatumDataSource, SettingSpecifierProvider {

	private String email;
	private String deviceID;
	private MessageSource messageSource;
	private long sampleCacheMs = 5000;
	private String sourceId;

	/**
	 * Constructor.
	 */
	public PanasonicBatteryDatumDataSource() {
		super();
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	private synchronized BatteryData getCurrentSample() {
		final String user = getEmail();
		final String id = getDeviceID();
		final boolean useDeviceID = (id != null && id.length() > 0);
		if ( (user == null || user.length() < 1) && !useDeviceID ) {
			log.info("No device ID or email configured, cannot collect sample.");
			return null;
		}
		BatteryData currSample = sample;
		if ( isCachedSampleExpired() ) {
			try {
				if ( useDeviceID ) {
					currSample = getClient().getCurrentBatteryDataForDevice(id);
				} else {
					currSample = getClient().getCurrentBatteryDataForEmail(user);
				}
				setErrorMessage(null);
				sample = currSample;
			} catch ( BatteryAPIException e ) {
				String msg = null;
				if ( e.getCode() > 0 ) {
					switch (e.getCode()) {
						case 404:
							if ( useDeviceID ) {
								msg = getMessageSource().getMessage("error.code.404.device",
										new Object[] { id }, Locale.getDefault());
							} else {
								msg = getMessageSource().getMessage("error.code.404.email",
										new Object[] { user }, Locale.getDefault());
							}
							break;

						default:
							msg = getMessageSource().getMessage("error.code",
									new Object[] { e.getCode() }, Locale.getDefault());
							break;
					}
				} else {
					msg = "Error communicating with Battery API: " + e.getMessage();
				}
				setErrorMessage(msg);
				if ( e.getCode() > 0 ) {
					log.warn("Battery API server returned error code {}", e.getCode());
					return null;
				} else {
					throw e;
				}
			}
			log.debug("Read BatteryData sample: {}", currSample);
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final Instant sampleDate = (sample != null ? sample.getDate() : null);
		if ( sampleDate == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - sampleDate.toEpochMilli();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return PanasonicBatteryDatum.class;
	}

	@Override
	public EnergyStorageDatum readCurrentDatum() {
		final BatteryData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		return new PanasonicBatteryDatum(currSample, resolvePlaceholders(getSourceId()));
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.panasonic.battery";
	}

	@Override
	public String getDisplayName() {
		return "Panasonic Battery API Battery";
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		PanasonicBatteryDatumDataSource defaults = new PanasonicBatteryDatumDataSource();
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		results.add(new BasicTextFieldSettingSpecifier("deviceID", ""));
		results.add(new BasicTextFieldSettingSpecifier("email", ""));
		results.add(
				new BasicTextFieldSettingSpecifier("sourceId", String.valueOf(defaults.getSourceId())));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		return results;
	}

	@Override
	public String toString() {
		String key = "deviceID";
		String ident = getDeviceID();
		if ( ident == null || ident.length() < 1 ) {
			key = "email";
			ident = getEmail();
		}
		return getClass().getSimpleName() + "{" + key + "=" + (ident != null ? ident : "") + "}";
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 *
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the email address used with the Battery API.
	 *
	 * @return The configured email.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Set the email address to use with the Battery API. This must be the same
	 * as the email registered with Panasonic.
	 *
	 * @param email
	 *        The email address to use.
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Get the battery device ID to use with the Battery API. Either this or
	 * {@link #getEmail()} must be configured, with this value being used in
	 * preference to the other.
	 *
	 * @return The configured battery device ID.
	 */
	public String getDeviceID() {
		return deviceID;
	}

	/**
	 * Set the battery device ID to use with the Battery API.
	 *
	 * @param deviceID
	 *        The battery device ID to use.
	 */
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	/**
	 * Get the source ID to assign to generated datum.
	 *
	 * @return The configured source ID.
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to assign to generated datum.
	 *
	 * @param sourceId
	 *        The source ID to use.
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
