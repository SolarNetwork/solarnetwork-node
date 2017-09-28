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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;

/**
 * Implementation of {@link net.solarnetwork.node.DatumDataSource} for
 * {@link PVEnergyDatum} the Outback MX60, communicating via the
 * {@code DataCollector} serial API.
 * 
 * <p>
 * Serial parameters known to work are:
 * </p>
 * 
 * <dl>
 * <dt>bufferSize</dt>
 * <dd>64</dt>
 * 
 * <dt>magic</dt>
 * <dd>The address of the MX60, e.g. <em>A</em>.</dd>
 * 
 * <dt>readSize</dt>
 * <dd>47</dt>
 * 
 * <dt>maxWait</dt>
 * <dd>60000</dt>
 * </dl>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>dataCollectorFactory</dt>
 * <dd>The {@link DataCollectorFactory} to use to obtain {@link DataCollector}
 * instances for reading the Outback MX60 data.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class OutbackMX60PowerDatumDataSource implements DatumDataSource<PVEnergyDatum> {

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for DC output amp values.
	 */
	public static final String DC_OUTPUT_AMPS_KEY = "dcOutputAmps";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for battery voltage values.
	 */
	public static final String BATTERY_VOLTAGE_KEY = "batteryVoltage";

	private static final int FRAME_IDX_DC_AMPS = 2;
	private static final int FRAME_IDX_PV_AMPS = 3;
	private static final int FRAME_IDX_PV_VOLTS = 4;
	private static final int FRAME_IDX_KWATT_HOURS = 5;
	private static final int FRAME_IDX_BAT_VOLTS = 10;

	private static final float BAT_VOLTS_MULTIPLIER = 0.1F;
	private static final double KWATT_HOURS_MULTIPLIER = 0.1;

	private ObjectFactory<DataCollector> dataCollectorFactory;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public Class<? extends PVEnergyDatum> getDatumType() {
		return GeneralNodePVEnergyDatum.class;
	}

	@Override
	public PVEnergyDatum readCurrentDatum() {
		DataCollector dataCollector = null;
		String data = null;
		try {
			dataCollector = this.dataCollectorFactory.getObject();
			dataCollector.collectData();
			data = dataCollector.getCollectedDataAsString();
		} finally {
			if ( dataCollector != null ) {
				dataCollector.stopCollecting();
			}
		}

		if ( data == null ) {
			log.warn("Null serial data received, serial communications problem");
			return null;
		}

		if ( log.isDebugEnabled() ) {
			log.debug("Collected serial data: " + data);
		}

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
	private GeneralNodePVEnergyDatum getPowerDatumInstance(String data) {

		// split data on comma
		String[] frame = data.split(",");
		if ( frame.length < 14 ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Expected 14 data elements, but got " + frame.length);
			}
			return null;
		}

		GeneralNodePVEnergyDatum pd = new GeneralNodePVEnergyDatum();

		Double pvAmps = getFrameDouble(frame, FRAME_IDX_PV_AMPS);

		Double pvVolts = getFrameDouble(frame, FRAME_IDX_PV_VOLTS);
		if ( pvVolts != null ) {
			pd.setDCVoltage(pvVolts.floatValue());
			if ( pvAmps != null ) {
				pd.setDCPower((int) Math.round(pvAmps.doubleValue() * pvVolts.doubleValue()));
			}
		}

		Double d = getFrameDouble(frame, FRAME_IDX_DC_AMPS);
		if ( d != null ) {
			pd.putInstantaneousSampleValue("dcOutputAmps", d.floatValue());
		}

		d = getFrameDouble(frame, FRAME_IDX_BAT_VOLTS);
		if ( d != null ) {
			pd.putInstantaneousSampleValue("batteryVoltage", d.floatValue() * BAT_VOLTS_MULTIPLIER);
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
	public String getUID() {
		return getClass().getName();
	}

	@Override
	public String getGroupUID() {
		return null;
	}

	public ObjectFactory<DataCollector> getDataCollectorFactory() {
		return dataCollectorFactory;
	}

	public void setDataCollectorFactory(ObjectFactory<DataCollector> dataCollectorFactory) {
		this.dataCollectorFactory = dataCollectorFactory;
	}

}
