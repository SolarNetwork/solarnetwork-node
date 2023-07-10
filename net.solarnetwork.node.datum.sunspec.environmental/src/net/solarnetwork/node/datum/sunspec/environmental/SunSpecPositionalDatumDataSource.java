/* ==================================================================
 * SunSpecPositionalDatumDataSource.java - 11/07/2023 6:54:44 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.sunspec.environmental;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.environmental.GpsModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.Incline;
import net.solarnetwork.node.hw.sunspec.environmental.InclinometerModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.PositionalDatum;
import net.solarnetwork.node.hw.sunspec.support.SunSpecDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * {@link DatumDataSource} for a SunSpec compatible positional devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SunSpecPositionalDatumDataSource extends SunSpecDeviceDatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * Constructor.
	 */
	public SunSpecPositionalDatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SunSpecPositionalDatumDataSource(AtomicReference<ModelData> sample) {
		super(sample);
		setSecondaryModelIds(Collections.singleton(EnvironmentalModelId.Inclinometer.getId()));
	}

	@Override
	protected Class<? extends ModelAccessor> getPrimaryModelAccessorType() {
		return GpsModelAccessor.class;
	}

	@Override
	protected SunSpecDeviceDatumDataSourceSupport getSettingsDefaultInstance() {
		return new SunSpecPositionalDatumDataSource();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public PositionalDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(getSourceId());
		final ModelData currSample;
		try {
			currSample = getCurrentSample();
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from SunSpec device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
		if ( currSample == null ) {
			return null;
		}
		return new PositionalDatum(currSample, sourceId);
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		PositionalDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.sunspec.pos";
	}

	@Override
	public String getDisplayName() {
		return "SunSpec Positional";
	}

	@Override
	protected String getStatusMessage(ModelData sample) {
		return "N/A";
	}

	@Override
	protected String getSampleMessage(ModelData sample) {
		StringBuilder buf = new StringBuilder();
		try {
			GpsModelAccessor gps = (sample != null ? sample.findTypedModel(GpsModelAccessor.class)
					: null);
			if ( gps != null ) {
				buf.append("lat: ").append(gps.getLatitude());
				buf.append(", lon: ").append(gps.getLongitude());
				buf.append(", el: ").append(gps.getAltitude());
			}

			InclinometerModelAccessor incl = (sample != null
					? sample.findTypedModel(InclinometerModelAccessor.class)
					: null);
			if ( incl != null ) {
				Incline in = incl.getIncline();
				if ( in != null ) {
					if ( buf.length() > 0 ) {
						buf.append(", ");
					}
					buf.append("incline: (").append(in.getInclineX()).append(",")
							.append(in.getInclineY()).append(",").append(in.getInclineZ()).append(")");
				}
			}
		} catch ( RuntimeException e ) {
			return "Unexpected error: " + e.getMessage();
		}

		if ( buf.length() < 1 ) {
			buf.append("N/A");
		} else if ( sample != null ) {
			buf.append("; sampled at ").append(sample.getDataTimestamp());
		}
		return buf.toString();
	}

}
