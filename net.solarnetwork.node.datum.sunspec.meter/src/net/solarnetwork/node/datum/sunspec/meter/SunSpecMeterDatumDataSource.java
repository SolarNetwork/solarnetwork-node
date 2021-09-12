/* ==================================================================
 * SunSpecMeterDatumDataSource.java - 23/05/2018 6:45:54 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.sunspec.meter;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.support.SunSpecDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} for a SunSpec compatible power meter.
 * 
 * @author matt
 * @version 2.0
 */
public class SunSpecMeterDatumDataSource extends SunSpecDeviceDatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	private boolean backwards = false;
	private boolean includePhaseMeasurements = false;

	/**
	 * Default constructor.
	 */
	public SunSpecMeterDatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SunSpecMeterDatumDataSource(AtomicReference<ModelData> sample) {
		super(sample);
		setSourceId("SunSpec-Meter");
	}

	@Override
	protected Class<? extends ModelAccessor> getPrimaryModelAccessorType() {
		return MeterModelAccessor.class;
	}

	@Override
	protected SunSpecDeviceDatumDataSourceSupport getSettingsDefaultInstance() {
		return new SunSpecMeterDatumDataSource();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		final ModelData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		MeterModelAccessor data = currSample.findTypedModel(MeterModelAccessor.class);
		SunSpecMeterDatum d = new SunSpecMeterDatum(data, resolvePlaceholders(getSourceId()),
				AcPhase.Total, this.backwards);
		if ( this.includePhaseMeasurements ) {
			d.populatePhaseMeasurementProperties(data);
		}
		return d;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		AcDcEnergyDatum datum = readCurrentDatum();
		// TODO: support phases
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.sunspec.meter";
	}

	@Override
	public String getDisplayName() {
		return "SunSpec Power Meter";
	}

	@Override
	protected List<SettingSpecifier> getSettingSpecifiersWithDefaults(
			SunSpecDeviceDatumDataSourceSupport defaults) {
		List<SettingSpecifier> results = super.getSettingSpecifiersWithDefaults(defaults);

		if ( defaults instanceof SunSpecMeterDatumDataSource ) {
			SunSpecMeterDatumDataSource mDefaults = (SunSpecMeterDatumDataSource) defaults;
			results.add(new BasicToggleSettingSpecifier("backwards", mDefaults.backwards));
			results.add(new BasicToggleSettingSpecifier("includePhaseMeasurements",
					mDefaults.includePhaseMeasurements));
		}

		return results;
	}

	@Override
	protected String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	@Override
	protected String getSampleMessage(ModelData sample) {
		if ( sample == null || sample.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		MeterModelAccessor data = sample.findTypedModel(MeterModelAccessor.class);
		if ( data == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", freq = ").append(data.getFrequency());
		buf.append(", Wh imp = ").append(data.getActiveEnergyImported());
		buf.append(", Wh exp = ").append(data.getActiveEnergyExported());
		buf.append("; sampled at ").append(Instant.ofEpochMilli(data.getDataTimestamp()));
		return buf.toString();
	}

	/**
	 * Get the "backwards" current direction flag.
	 * 
	 * @return {@literal true} to swap energy delivered and received values in
	 *         returned datum
	 */
	public boolean isBackwards() {
		return backwards;
	}

	/**
	 * Toggle the "backwards" current direction flag.
	 * 
	 * @param backwards
	 *        {@literal true} to swap energy delivered and received values
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

	/**
	 * Toggle the inclusion of phase measurement properties in collected datum.
	 * 
	 * @param includePhaseMeasurements
	 *        {@literal true} to collect phase measurements
	 * @since 1.2
	 */
	public void setIncludePhaseMeasurements(boolean includePhaseMeasurements) {
		this.includePhaseMeasurements = includePhaseMeasurements;
	}
}
