/* ==================================================================
 * SunSpecMeteorologicalDatumDataSource.java - 10/07/2023 2:39:45 pm
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.environmental.BomTemperatureModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.EnvironmentalModelId;
import net.solarnetwork.node.hw.sunspec.environmental.IrradianceModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.MeteorologicalDatum;
import net.solarnetwork.node.hw.sunspec.environmental.MeteorologicalModelAccessor;
import net.solarnetwork.node.hw.sunspec.environmental.MiniMeteorologicalModelAccessor;
import net.solarnetwork.node.hw.sunspec.support.SunSpecDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * {@link DatumDataSource} for a SunSpec compatible environmental devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SunSpecMeteorologicalDatumDataSource extends SunSpecDeviceDatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * Constructor.
	 */
	public SunSpecMeteorologicalDatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SunSpecMeteorologicalDatumDataSource(AtomicReference<ModelData> sample) {
		super(sample);
		// @formatter:off
		setSecondaryModelIds(new TreeSet<>(Arrays.asList(
				EnvironmentalModelId.MiniMeteorolgical.getId(),
				EnvironmentalModelId.Irradiance.getId(),
				EnvironmentalModelId.BackOfModuleTemperature.getId())));
		// @formatter:on
	}

	@Override
	protected Class<? extends ModelAccessor> getPrimaryModelAccessorType() {
		return MeteorologicalModelAccessor.class;
	}

	@Override
	protected SunSpecDeviceDatumDataSourceSupport getSettingsDefaultInstance() {
		return new SunSpecMeteorologicalDatumDataSource();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public AtmosphericDatum readCurrentDatum() {
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
		MeteorologicalDatum d = new MeteorologicalDatum(currSample, sourceId);
		return d;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		AtmosphericDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.sunspec.met";
	}

	@Override
	public String getDisplayName() {
		return "SunSpec Meteorological";
	}

	@Override
	protected String getStatusMessage(ModelData sample) {
		return "N/A";
	}

	@Override
	protected String getSampleMessage(ModelData sample) {
		StringBuilder buf = new StringBuilder();
		try {
			MeteorologicalModelAccessor met = (sample != null
					? sample.findTypedModel(MeteorologicalModelAccessor.class)
					: null);
			if ( met != null ) {
				buf.append("temp: ").append(met.getAmbientTemperature());
				buf.append(", atm: ").append(met.getAtmosphericPressure());
			}

			MiniMeteorologicalModelAccessor mini = (sample != null
					? sample.findTypedModel(MiniMeteorologicalModelAccessor.class)
					: null);
			if ( mini != null ) {
				if ( buf.length() > 0 ) {
					buf.append(", ");
				}
				buf.append("temp: ").append(mini.getAmbientTemperature());
			}

			IrradianceModelAccessor irr = (sample != null
					? sample.findTypedModel(IrradianceModelAccessor.class)
					: null);
			if ( irr != null ) {
				if ( buf.length() > 0 ) {
					buf.append(", ");
				}
				buf.append("ghi: ").append(irr.getGlobalHorizontalIrradiance());
			}

			BomTemperatureModelAccessor bom = (sample != null
					? sample.findTypedModel(BomTemperatureModelAccessor.class)
					: null);
			if ( irr != null ) {
				if ( buf.length() > 0 ) {
					buf.append(", ");
				}
				buf.append("bom: ").append(bom.getBackOfModuleTemperature());
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
