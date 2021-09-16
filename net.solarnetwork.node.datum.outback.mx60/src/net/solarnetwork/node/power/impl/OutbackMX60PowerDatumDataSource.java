/* ===================================================================
 * OutbackMX60PowerDatumDataSource.java
 *
 * Created Aug 7, 2008 9:48:26 PM
 *
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.node.power.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDcEnergyDatum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.support.SerialDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Implementation of {@link net.solarnetwork.node.service.DatumDataSource} for
 * {@link DcEnergyDatum} the Outback MX60.
 *
 * <p>
 * Serial parameters known to work are:
 * </p>
 *
 * <dl>
 * <dt>bufferSize</dt>
 * <dd>64</dd>
 *
 * <dt>magic</dt>
 * <dd>The address of the MX60, e.g. <em>A</em>.</dd>
 *
 * <dt>readSize</dt>
 * <dd>47</dd>
 *
 * <dt>maxWait</dt>
 * <dd>60000</dd>
 * </dl>
 *
 * @author matt
 * @version 2.0
 */
public class OutbackMX60PowerDatumDataSource extends SerialDeviceDatumDataSourceSupport<DcEnergyDatum>
		implements DatumDataSource, SettingSpecifierProvider, SerialConnectionAction<DcEnergyDatum> {

	/**
	 * The instantaneous sample key for DC output amp values.
	 */
	public static final String DC_OUTPUT_AMPS_KEY = "dcOutputAmps";

	/**
	 * The instantaneous sample key for battery voltage values.
	 */
	public static final String BATTERY_VOLTAGE_KEY = "batteryVoltage";

	/** The {@code address} default property. */
	public static final String DEFAULT_ADDRESS = "A";

	/** The number of bytes to read after the found address. */
	private static final int READ_SIZE = 47;

	private static Charset US_ASCII = Charset.forName("US-ASCII");

	private static final int FRAME_IDX_DC_AMPS = 2;
	private static final int FRAME_IDX_PV_AMPS = 3;
	private static final int FRAME_IDX_PV_VOLTS = 4;
	private static final int FRAME_IDX_KWATT_HOURS = 5;
	private static final int FRAME_IDX_BAT_VOLTS = 10;

	private static final float BAT_VOLTS_MULTIPLIER = 0.1F;
	private static final double KWATT_HOURS_MULTIPLIER = 0.1;

	private String address = DEFAULT_ADDRESS;

	/**
	 * Constructor.
	 */
	public OutbackMX60PowerDatumDataSource() {
		super();
		setDisplayName("Outback MX60 Charge Controller");
	}

	@Override
	public DcEnergyDatum readCurrentDatum() {
		final DcEnergyDatum currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		return currSample;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return DcEnergyDatum.class;
	}

	private DcEnergyDatum getCurrentSample() {
		DcEnergyDatum sample = getSample();
		if ( sample == null ) {
			try {
				sample = performAction(this);
				if ( sample != null ) {
					setCachedSample(sample);
				}
				if ( log.isTraceEnabled() && sample != null ) {
					log.trace("Sample: {}", sample.asSimpleMap());
				}
				log.debug("Read MX60 data: {}", sample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from MX60 device " + serialNetwork(), e);
			}
		}
		return sample;
	}

	@Override
	public DcEnergyDatum doWithConnection(SerialConnection conn) throws IOException {
		byte[] msg = conn.readMarkedMessage(address.getBytes(US_ASCII), READ_SIZE);
		if ( msg == null ) {
			log.warn("Null serial data received, serial communications problem");
			return null;
		}
		String data = new String(msg, US_ASCII);
		log.debug("Collected serial data: {}", data);
		return getPowerDatumInstance(data);
	}

	/**
	 * Parse an Outback Mate serial data string into a PowerDatum object.
	 *
	 * <p>
	 * The string data format looks like
	 * {@code D,00,00,00,016,129,00,00,000,00,511,000,000,046}. See the <em>Mate
	 * Serial Communications Guide</em> for details.
	 * </p>
	 *
	 * @param data
	 *        the serial data string
	 * @return a PowerDatum instance
	 */
	private DcEnergyDatum getPowerDatumInstance(String data) {

		// split data on comma
		String[] frame = data.split(",");
		if ( frame.length < 14 ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Expected 14 data elements, but got " + frame.length);
			}
			return null;
		}

		SimpleDcEnergyDatum pd = new SimpleDcEnergyDatum(resolvePlaceholders(getSourceId()),
				Instant.now(), new DatumSamples());

		Double pvAmps = getFrameDouble(frame, FRAME_IDX_PV_AMPS);

		Double pvVolts = getFrameDouble(frame, FRAME_IDX_PV_VOLTS);
		if ( pvVolts != null ) {
			pd.setDcVoltage(pvVolts.floatValue());
			if ( pvAmps != null ) {
				pd.setDcPower((int) Math.round(pvAmps.doubleValue() * pvVolts.doubleValue()));
			}
		}

		Double d = getFrameDouble(frame, FRAME_IDX_DC_AMPS);
		if ( d != null ) {
			pd.getSamples().putInstantaneousSampleValue("dcOutputAmps", d.floatValue());
		}

		d = getFrameDouble(frame, FRAME_IDX_BAT_VOLTS);
		if ( d != null ) {
			pd.getSamples().putInstantaneousSampleValue("batteryVoltage",
					d.floatValue() * BAT_VOLTS_MULTIPLIER);
		}

		d = getFrameDouble(frame, FRAME_IDX_KWATT_HOURS);
		if ( d != null ) {
			pd.setWattHourReading(Math.round(d * KWATT_HOURS_MULTIPLIER * 1000));
		}

		return pd;
	}

	private Double getFrameDouble(String[] frame, int idx) {
		if ( frame[idx].length() > 0 ) {
			return Double.valueOf(frame[idx]);
		}
		return null;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) {
		return Collections.emptyMap();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.outback.mx60";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(3);
		result.addAll(baseIdentifiableSettings(""));
		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("address", DEFAULT_ADDRESS));
		result.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['uid']",
				"Serial Port"));
		result.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		return result;
	}

	/**
	 * Set the MX60 address to read.
	 *
	 * @param address
	 *        the address
	 */
	public void setAddress(String address) {
		this.address = address;
	}

}
