/* ==================================================================
 * KTLDatumDataSource.java - 23/11/2017 3:06:48 pm
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

package net.solarnetwork.node.datum.csi.ktl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLData;
import net.solarnetwork.node.hw.csi.inverter.KTLSupport;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodeACEnergyDatum}
 * with the CSI inverter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>messageSource</dt>
 * <dd>The {@link MessageSource} to use with
 * {@link SettingSpecifierProvider}.</dd>
 * 
 * <dt>sampleCacheMs</dt>
 * <dd>The maximum number of milliseconds to cache data read from the inverter,
 * until the data will be read from the inverter again.</dd>
 * </dl>
 * 
 * @author matt
 * @author maxieduncan
 * @version 1.0
 */
public class KTLDatumDataSource extends KTLSupport implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private MessageSource messageSource;
	private long sampleCacheMs = 5000;

	private KTLData getCurrentSample() {
		KTLData currSample;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(new ModbusConnectionAction<KTLData>() {

					@Override
					public KTLData doWithConnection(ModbusConnection connection) throws IOException {
						getSample().readInverterData(connection);
						return getSample().getSnapshot();
					}

				});
				if ( log.isTraceEnabled() && currSample != null) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read KTL data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
			}
		} else {
			currSample = getSample().getSnapshot();
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - getSample().getInverterDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return KTLDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final KTLData currSample = getCurrentSample();
		KTLDatum d = new KTLDatum(currSample);
		if ( currSample.getInverterDataTimestamp() >= start ) {
			// we read from the inverter
			postDatumCapturedEvent(d);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return KTLDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final KTLData currSample = getCurrentSample();
		final List<GeneralNodeACEnergyDatum> results = new ArrayList<GeneralNodeACEnergyDatum>(1);
		if ( currSample == null ) {
			return results;
		}
		final boolean postCapturedEvent = (currSample.getInverterDataTimestamp() >= start);
		if ( postCapturedEvent ) {
			KTLDatum d = new KTLDatum(currSample);
			d.setSourceId("Main");// TODO
			if ( postCapturedEvent ) {
				// we read from the inverter
				postDatumCapturedEvent(d);
			}
			results.add(d);
		}
		
		return results;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.csi.ktl";
	}

	@Override
	public String getDisplayName() {
		return "CSI KTL Inverter";
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
		KTLDatumDataSource defaults = new KTLDatumDataSource();
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
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

}
