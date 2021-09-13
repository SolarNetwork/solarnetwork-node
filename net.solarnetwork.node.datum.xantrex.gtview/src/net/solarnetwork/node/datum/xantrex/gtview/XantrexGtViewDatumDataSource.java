/* ===================================================================
 * XantrexGtViewDatumDataSource.java
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

package net.solarnetwork.node.datum.xantrex.gtview;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.io.UrlUtils;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Implementation of {@link DatumDataSource} for the Xantrex series of
 * inverters, acquiring the data by reading the files written by the freeware
 * GT-View application.
 * 
 * @author matt, mike
 * @version 2.0
 */
public class XantrexGtViewDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	private static final int FRAME_IDX_PV_VOLTS = 2;
	private static final int FRAME_IDX_PV_WATTS = 5;
	private static final int FRAME_IDX_AC_WATTS = 6;
	private static final int FRAME_IDX_AC_VOLTS = 8;
	private static final int FRAME_IDX_WH = 9;

	private String url;
	private String sourceId;

	/**
	 * Constructor.
	 */
	public XantrexGtViewDatumDataSource() {
		super();
		setUid(getClass().getName());
		setDisplayName("Zantrex GT View");
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		String data = null;
		URLConnection conn;
		try {
			conn = UrlUtils.getURLConnection(url, null, 10000, null);
			try (BufferedReader r = new BufferedReader(
					UrlUtils.getUnicodeReaderFromURLConnection(conn))) {
				// jump to last available line, assuming data is sorted by time in ascending order
				String s = null;
				while ( (s = r.readLine()) != null ) {
					data = s;
				}
			}
		} catch ( IOException e ) {
			throw new RuntimeException("Communication error reading from [" + url + "]", e);
		}
		if ( data == null ) {
			log.warn("Null data received, communications problem");
			return null;
		}
		return getPowerDatumInstance(data);

	}

	private AcDcEnergyDatum getPowerDatumInstance(String data) {
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

		SimpleAcDcEnergyDatum datum = new SimpleAcDcEnergyDatum(resolvePlaceholders(sourceId),
				Instant.now(), new DatumSamples());
		datum.setDcPower(d.intValue());

		// Field 0: Date: unused

		// Field 1: Time: unused

		// Field 2: DC Volts
		d = getFrameDouble(tokens, FRAME_IDX_PV_VOLTS);
		if ( d != null ) {
			datum.setDcVoltage(d.floatValue());
			log.debug("DC Volts: {}", d);
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

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.xantrex.gtview";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.addAll(basicIdentifiableSettings());
		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("url", null));
		return result;
	}

	/**
	 * Get the URL to the GT-View log file.
	 * 
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL to the GT-View log file.
	 * 
	 * @param url
	 *        the URL
	 */
	public void setUrl(String url) {
		this.url = url;
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
	 *        the source ID
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
