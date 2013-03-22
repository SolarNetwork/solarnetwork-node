/* ==================================================================
 * CentameterPowerDatumDataSource.java - Apr 25, 2010 12:57:01 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.power.impl.centameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.centameter.CentameterDatum;
import net.solarnetwork.node.centameter.CentameterSupport;
import net.solarnetwork.node.centameter.CentameterUtils;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DataCollectorSerialPortBeanParameters;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.node.util.DataUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Implementation of {@link DatumDataSource} {@link PowerDatum} objects, using a
 * Centameter amp sensor.
 * 
 * <p>
 * Normally Centameters are used to monitor consumption, but in some situations
 * they can be used as a low-cost montior for generation, especially if the
 * generation device cannot be communicated with.
 * </p>
 * 
 * <p>
 * This implementation relies on a device that can listen to the radio signal
 * broadcast by a Cent-a-meter monitor and write that data to a local serial
 * port. This class will read the Cent-a-meter data from the serial port to
 * generate consumption data.
 * </p>
 * 
 * <p>
 * It assumes the {@link DataCollector} implementation blocks until appropriate
 * data is available when the {@link DataCollector#collectData()} method is
 * called.
 * </p>
 * 
 * <p>
 * Serial parameters that are known to work are:
 * </p>
 * 
 * <pre>
 * magicBytes = x (0x78)
 * baud = 4800
 * bufferSize = 16
 * readSize = 15
 * receiveThreshold = -1
 * maxWait = 60000
 * </pre>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>ampsFieldName</dt>
 * <dd>The bean property on {@link PowerDatum} to set the amp reading value
 * collected from the Centameter. Defaults to {@link #DEFAULT_AMPS_FIELD_NAME}.</dd>
 * 
 * <dt>voltsFieldName</dt>
 * <dd>The bean property on {@link PowerDatum} to set the {@code voltage} value.
 * Defaults to {@link #DEFAULT_AMPS_FIELD_NAME}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class CentameterPowerDatumDataSource extends CentameterSupport implements
		DatumDataSource<PowerDatum>, MultiDatumDataSource<PowerDatum>, SettingSpecifierProvider {

	/** The default value for the {@code ampsFieldName} property. */
	public static final String DEFAULT_AMPS_FIELD_NAME = "pvAmps";

	/** The default value for the {@code voltsFieldName} property. */
	public static final String DEFAULT_VOLTS_FIELD_NAME = "pvVolts";

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private String ampsFieldName = DEFAULT_AMPS_FIELD_NAME;
	private String voltsFieldName = DEFAULT_VOLTS_FIELD_NAME;

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public PowerDatum readCurrentDatum() {
		DataCollectorFactory<DataCollectorSerialPortBeanParameters> df = getDataCollectorFactory()
				.service();
		if ( df == null ) {
			log.debug("No DataCollectorFactory available");
			return null;
		}

		DataCollector dataCollector = df.getDataCollectorInstance(getSerialParams());
		byte[] data = null;
		try {
			dataCollector.collectData();
			data = dataCollector.getCollectedData();
		} finally {
			if ( dataCollector != null ) {
				dataCollector.stopCollecting();
			}
		}

		if ( data == null ) {
			log.warn("Null serial data received, serial communications problem");
			return null;
		}

		return getPowerDatumInstance(DataUtils.getUnsignedValues(data), getAmpSensorIndex());
	}

	@Override
	public Class<? extends PowerDatum> getMultiDatumType() {
		return PowerDatum.class;
	}

	@Override
	public Collection<PowerDatum> readMultipleDatum() {
		DataCollectorFactory<DataCollectorSerialPortBeanParameters> df = getDataCollectorFactory()
				.service();
		if ( df == null ) {
			return null;
		}

		List<PowerDatum> result = new ArrayList<PowerDatum>(3);
		long endTime = isCollectAllSourceIds() && getSourceIdFilter().size() > 1 ? System
				.currentTimeMillis() + (getCollectAllSourceIdsTimeout() * 1000) : 0;
		Set<String> sourceIdSet = new HashSet<String>(getSourceIdFilter().size());
		DataCollector dataCollector = null;
		try {
			dataCollector = df.getDataCollectorInstance(getSerialParams());
			do {
				dataCollector.collectData();
				byte[] data = dataCollector.getCollectedData();
				if ( data == null ) {
					log.warn("Null serial data received, serial communications problem");
					return null;
				}
				short[] unsigned = DataUtils.getUnsignedValues(data);

				// add a known address for this reading
				addKnownAddress(new CentameterDatum(
						String.format("%X", unsigned[CENTAMETER_ADDRESS_IDX]),
						(float) CentameterUtils.getAmpReading(unsigned, 1),
						(float) CentameterUtils.getAmpReading(unsigned, 2),
						(float) CentameterUtils.getAmpReading(unsigned, 3)));

				if ( log.isDebugEnabled() ) {
					log.debug(String.format(
							"Centameter address %X, count %d, amp1 %.1f, amp2 %.1f, amp3 %.1f",
							unsigned[CENTAMETER_ADDRESS_IDX], (unsigned[1] & 0xF),
							CentameterUtils.getAmpReading(unsigned, 1),
							CentameterUtils.getAmpReading(unsigned, 2),
							CentameterUtils.getAmpReading(unsigned, 3)));
				}

				for ( int ampIndex = 1; ampIndex <= 3; ampIndex++ ) {
					PowerDatum datum = getPowerDatumInstance(unsigned, ampIndex);
					if ( (ampIndex & getMultiAmpSensorIndexFlags()) != ampIndex ) {
						continue;
					}
					if ( datum != null ) {
						if ( !sourceIdSet.contains(datum.getSourceId()) ) {
							result.add(datum);
							sourceIdSet.add(datum.getSourceId());
						}
					}
				}
			} while ( System.currentTimeMillis() < endTime
					&& sourceIdSet.size() < getSourceIdFilter().size() );
		} finally {
			if ( dataCollector != null ) {
				dataCollector.stopCollecting();
			}
		}

		return result.size() < 1 ? null : result;
	}

	private PowerDatum getPowerDatumInstance(short[] unsigned, int ampIndex) {
		// report the Centameter address as upper-case hex value
		String addr = String.format(getSourceIdFormat(), unsigned[CENTAMETER_ADDRESS_IDX], ampIndex);
		float amps = (float) CentameterUtils.getAmpReading(unsigned, ampIndex);

		PowerDatum datum = new PowerDatum();

		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" + addr + "] not in source ID filter set");
			}
			return null;
		}
		datum.setSourceId(addr);

		datum.setCreated(new Date());

		Map<String, Object> props = new HashMap<String, Object>();
		props.put(ampsFieldName, amps);
		props.put(voltsFieldName, getVoltage());
		ClassUtils.setBeanProperties(datum, props);

		return datum;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.power.centameter";
	}

	@Override
	public String getDisplayName() {
		return "Cent-a-meter power meter";
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				MessageSource parent = getDefaultSettingsMessageSource();

				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(CentameterPowerDatumDataSource.class.getClassLoader());
				source.setBasename(CentameterPowerDatumDataSource.class.getName());
				source.setParentMessageSource(parent);
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getDefaultSettingSpecifiers();
		results.add(new BasicTextFieldSettingSpecifier("ampsFieldName", DEFAULT_AMPS_FIELD_NAME));
		results.add(new BasicTextFieldSettingSpecifier("voltsFieldName", DEFAULT_VOLTS_FIELD_NAME));
		return results;
	}

	public String getAmpsFieldName() {
		return ampsFieldName;
	}

	public void setAmpsFieldName(String ampsFieldName) {
		this.ampsFieldName = ampsFieldName;
	}

	public String getVoltsFieldName() {
		return voltsFieldName;
	}

	public void setVoltsFieldName(String voltsFieldName) {
		this.voltsFieldName = voltsFieldName;
	}

}
