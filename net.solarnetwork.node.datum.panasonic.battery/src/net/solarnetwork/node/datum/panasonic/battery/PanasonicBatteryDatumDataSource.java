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

import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.EnergyStorageDatum;
import net.solarnetwork.node.domain.GeneralNodeEnergyStorageDatum;
import net.solarnetwork.node.hw.panasonic.battery.BatteryAPISupport;
import net.solarnetwork.node.hw.panasonic.battery.BatteryData;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import org.joda.time.DateTime;
import org.springframework.context.MessageSource;

/**
 * {@link DatumDataSource} implementation for
 * {@link GeneralNodeEnergyStorageDatum} with the Panasonic Battery API.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>messageSource</dt>
 * <dd>The {@link MessageSource} to use with {@link SettingSpecifierProvider}.</dd>
 * 
 * <dt>sampleCacheMs</dt>
 * <dd>The maximum number of milliseconds to cache data read from the battery
 * API, until the data will be read from the API again.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class PanasonicBatteryDatumDataSource extends BatteryAPISupport implements
		DatumDataSource<GeneralNodeEnergyStorageDatum>, SettingSpecifierProvider {

	private String email;
	private MessageSource messageSource;
	private long sampleCacheMs = 5000;
	private String sourceId = "Battery";

	private BatteryData getCurrentSample() {
		String e = getEmail();
		if ( e == null || e.length() < 1 ) {
			return null;
		}
		BatteryData currSample;
		if ( isCachedSampleExpired() ) {
			currSample = getClient().getCurrentBatteryDataForEmail(e);
			log.debug("Read BatteryData sample: {}", currSample);
		} else {
			currSample = sample;
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final DateTime sampleDate = (sample != null ? sample.getDate() : null);
		if ( sampleDate == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - sampleDate.getMillis();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public Class<? extends GeneralNodeEnergyStorageDatum> getDatumType() {
		return PanasonicBatteryDatum.class;
	}

	@Override
	public GeneralNodeEnergyStorageDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final BatteryData currSample = getCurrentSample();
		PanasonicBatteryDatum d = new PanasonicBatteryDatum(currSample);
		d.setSourceId(getSourceId());
		if ( currSample.getDate() != null && currSample.getDate().getMillis() >= start ) {
			// we read from the meter
			postDatumCapturedEvent(d, EnergyStorageDatum.class);
		}
		return d;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.panasonic.battery";
	}

	@Override
	public String getDisplayName() {
		return "Panasonic Battery API Battery";
	}

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
		results.add(new BasicTextFieldSettingSpecifier("email", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceId",
				String.valueOf(defaults.getSourceId())));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs", String.valueOf(defaults
				.getSampleCacheMs())));
		return results;
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
	 * @param sampleCacheSecondsMs
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
