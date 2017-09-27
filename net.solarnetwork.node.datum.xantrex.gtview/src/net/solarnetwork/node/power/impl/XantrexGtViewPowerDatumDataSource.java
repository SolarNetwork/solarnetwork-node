/* ===================================================================
 * XantrexGtViewPowerDatumDataSource.java
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

/* ==================================================================
 * UrlDataCollector.java - Dec 9, 2009 9:46:41 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.power.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;

/**
 * Implementation of {@link GenerationDataSource} for the Xantrex series of
 * inverters, acquiring the data by reading the files written by the freeware
 * GT-View application.
 * 
 * <p>
 * It assumes the {@link DataCollector} implementation blocks until appropriate
 * data is available when the {@link DataCollector#collectData()} method is
 * called.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>dataCollectorFactory</dt>
 * <dd>The factory for creating {@link DataCollector} instances with.
 * {@link GenericObjectFactory#getObject()} will be called on each invocation of
 * {@link #readCurrentConsumptionDatum()}.</dd>
 * </dl>
 * 
 * @author matt, mike
 * @version 1.2
 */
public class XantrexGtViewPowerDatumDataSource implements DatumDataSource<PVEnergyDatum> {

	private static final int FRAME_IDX_PV_VOLTS = 2;
	private static final int FRAME_IDX_PV_WATTS = 5;
	private static final int FRAME_IDX_AC_WATTS = 6;
	private static final int FRAME_IDX_AC_VOLTS = 8;
	private static final int FRAME_IDX_WH = 9;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private ObjectFactory<DataCollector> dataCollectorFactory;

	@Override
	public String getUID() {
		return getClass().getName();
	}

	@Override
	public String getGroupUID() {
		return null;
	}

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

		return getPowerDatumInstance(data);

	}

	private GeneralNodePVEnergyDatum getPowerDatumInstance(String data) {
		if ( log.isDebugEnabled() ) {
			log.debug("Raw last sample data in file: " + data);
		}

		String[] tokens = data.split("\t");

		// only use this if DC watts is non-zero. Some junk is
		// left over from previous day in other fields
		Double d = getFrameDouble(tokens, FRAME_IDX_PV_WATTS);

		if ( d == null || d.doubleValue() == 0.0 ) {
			return null;
		}

		GeneralNodePVEnergyDatum datum = new GeneralNodePVEnergyDatum();
		datum.setDCPower(d.intValue());

		// Field 0: Date: unused

		// Field 1: Time: unused

		// Field 2: DC Volts
		Double pvVolts = getFrameDouble(tokens, FRAME_IDX_PV_VOLTS);
		if ( pvVolts != null ) {
			datum.setDCVoltage(pvVolts.floatValue());
			log.debug("DC Volts: {}", pvVolts);
		}

		// Field 3: DC Amps: unused

		// Field 4: MPPT: unused

		// Field 5: DC Watts: already parsed above

		// Field 6: AC Watts
		d = getFrameDouble(tokens, FRAME_IDX_AC_WATTS);
		if ( d != null ) {
			datum.setWatts(Math.round(d.floatValue()));
			log.debug("AC Watts: {}", d);
		}

		// Field 7: Efficiency: unused

		// Field 8: AC Volts
		d = getFrameDouble(tokens, FRAME_IDX_AC_VOLTS);
		if ( d != null ) {
			datum.setVoltage(d.floatValue());
			log.debug("AC Volts: {}", d);
		}

		// Field 9: Cumulative AC Wh
		d = getFrameDouble(tokens, FRAME_IDX_WH);
		if ( d != null ) {
			// store Wh as kWh
			datum.setWattHourReading(d.longValue());
			log.debug("WH: {}", d);
		}

		return datum;
	}

	private Double getFrameDouble(String[] frame, int idx) {
		if ( frame[idx].length() > 0 ) {
			return Double.valueOf(frame[idx]);
		}
		return null;
	}

	public ObjectFactory<DataCollector> getDataCollectorFactory() {
		return dataCollectorFactory;
	}

	public void setDataCollectorFactory(ObjectFactory<DataCollector> dataCollectorFactory) {
		this.dataCollectorFactory = dataCollectorFactory;
	}

}
