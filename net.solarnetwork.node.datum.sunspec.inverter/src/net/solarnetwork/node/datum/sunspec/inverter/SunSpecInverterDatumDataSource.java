/* ==================================================================
 * SunSpecInverterDatumDataSource.java - 9/10/2018 10:51:45 AM
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

package net.solarnetwork.node.datum.sunspec.inverter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterDatum;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterNameplateRatingsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.hw.sunspec.support.SunSpecDeviceDatumDataSourceSupport;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * {@link DatumDataSource} for a SunSpec compatible inverter.
 *
 * @author matt
 * @version 2.3
 */
public class SunSpecInverterDatumDataSource extends SunSpecDeviceDatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	private Set<InverterOperatingState> ignoreStates = EnumSet.of(InverterOperatingState.Off,
			InverterOperatingState.Sleeping, InverterOperatingState.Standby);
	private boolean includePhaseMeasurements = false;

	/**
	 * Default constructor.
	 */
	public SunSpecInverterDatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 *
	 * @param sample
	 *        the sample data to use
	 */
	public SunSpecInverterDatumDataSource(AtomicReference<ModelData> sample) {
		super(sample);
	}

	@Override
	protected Class<? extends ModelAccessor> getPrimaryModelAccessorType() {
		return InverterModelAccessor.class;
	}

	@Override
	protected SunSpecDeviceDatumDataSourceSupport getSettingsDefaultInstance() {
		return new SunSpecInverterDatumDataSource();
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection connection, ModelData data)
			throws IOException {
		Map<String, Object> result = super.readDeviceInfo(connection, data);
		if ( result == null ) {
			return null;
		}

		// look for nameplate ratings
		InverterNameplateRatingsModelAccessor nameplateRatings = data
				.findTypedModel(InverterNameplateRatingsModelAccessor.class);
		if ( nameplateRatings != null ) {
			data.readModelData(connection, nameplateRatings);
			Map<String, Object> ratings = nameplateRatings.nameplateRatingsInfo();
			if ( ratings != null ) {
				result.put(DataAccessor.INFO_KEY_NAMEPLATE_RATINGS, ratings);
			}
		}

		return result;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(getSourceId());
		final ModelData currSample;
		try {
			currSample = getCurrentSample();
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from SunSpec inverter {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
		if ( currSample == null ) {
			return null;
		}
		InverterModelAccessor data = currSample.findTypedModel(InverterModelAccessor.class);
		if ( data == null ) {
			return null;
		}
		OperatingState opState = data.getOperatingState();
		if ( opState != null && ignoreStates != null ) {
			try {
				InverterOperatingState invState = InverterOperatingState.forCode(opState.getCode());
				if ( ignoreStates.contains(invState) ) {
					log.info("Ignoring data from inverter in {} state", invState);
					return null;
				}
			} catch ( IllegalArgumentException e ) {
				// ignore this
			}
		}
		InverterDatum d = new InverterDatum(data, sourceId, AcPhase.Total);
		Set<Integer> secondaryModelIds = getSecondaryModelIds();
		if ( secondaryModelIds != null
				&& secondaryModelIds.contains(InverterModelId.MultipleMpptInverterExtension.getId()) ) {
			// populate DC modules
			InverterMpptExtensionModelAccessor mppt = currSample
					.findTypedModel(InverterMpptExtensionModelAccessor.class);
			d.populateDcModulesProperties(mppt);
		}
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
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.sunspec.inverter";
	}

	@Override
	public String getDisplayName() {
		return "SunSpec Power Inverter";
	}

	@Override
	protected List<SettingSpecifier> getSettingSpecifiersWithDefaults(
			SunSpecDeviceDatumDataSourceSupport defaults) {
		List<SettingSpecifier> results = super.getSettingSpecifiersWithDefaults(defaults);

		if ( defaults instanceof SunSpecInverterDatumDataSource ) {
			SunSpecInverterDatumDataSource iDefaults = (SunSpecInverterDatumDataSource) defaults;
			results.add(new BasicTextFieldSettingSpecifier("ignoreStatesValue",
					iDefaults.getIgnoreStatesValue()));
			results.add(new BasicToggleSettingSpecifier("includePhaseMeasurements",
					iDefaults.includePhaseMeasurements));
		}

		return results;
	}

	@Override
	protected String getStatusMessage(ModelData sample) {
		StringBuilder buf = new StringBuilder();
		try {
			InverterModelAccessor data = (sample != null
					? sample.findTypedModel(InverterModelAccessor.class)
					: null);
			if ( data != null ) {
				buf.append("State: ");
				OperatingState state = data.getOperatingState();
				if ( state != null ) {
					buf.append(state.toString()).append(" (").append(state.getDescription()).append(")");
				} else {
					buf.append("N/A");
				}
				Set<ModelEvent> events = data.getEvents();
				if ( events != null && !events.isEmpty() ) {
					for ( ModelEvent event : events ) {
						buf.append("; ").append(event.getDescription());
					}
				}
			}
		} catch ( RuntimeException e ) {
			return "Unexpected error: " + e.getMessage();
		}
		return (buf.length() < 1 ? "N/A" : buf.toString());
	}

	@Override
	protected String getSampleMessage(ModelData sample) {
		if ( sample == null || sample.getDataTimestamp() == null ) {
			return "N/A";
		}
		InverterModelAccessor data;
		try {
			data = sample.findTypedModel(InverterModelAccessor.class);
			if ( data == null ) {
				return "N/A";
			}
		} catch ( RuntimeException e ) {
			return "Unexpected model: " + sample.getModel();
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", freq = ").append(data.getFrequency());
		buf.append(", Wh exp = ").append(data.getActiveEnergyExported());

		InverterMpptExtensionModelAccessor mppt = sample
				.findTypedModel(InverterMpptExtensionModelAccessor.class);
		if ( mppt != null ) {
			for ( InverterMpptExtensionModelAccessor.DcModule module : mppt.getDcModules() ) {
				Integer moduleId = module.getInputId();
				if ( moduleId == null ) {
					continue;
				}
				Float moduleCurrent = module.getDCCurrent();
				if ( moduleCurrent == null ) {
					continue;
				}
				buf.append(", DC module ").append(moduleId).append(" A = ").append(moduleCurrent);
				Float moduleVoltage = module.getDCVoltage();
				if ( moduleVoltage != null ) {
					buf.append(", DC module ").append(moduleId).append(" V = ").append(moduleVoltage);
				}

			}
		}

		buf.append("; sampled at ").append(data.getDataTimestamp());
		return buf.toString();
	}

	/**
	 * Set the states to ignore and not return datum for.
	 *
	 * @param states
	 *        the states to ignore
	 */
	public void setIgnoreStates(Set<InverterOperatingState> states) {
		this.ignoreStates = states;
	}

	/**
	 * Set the states to ignore via a comma-delimited list of
	 * {@link InverterOperatingState} names.
	 *
	 * @param states
	 *        the state names to set
	 */
	public void setIgnoreStatesValue(String states) {
		Set<String> names = StringUtils.commaDelimitedStringToSet(states);
		Set<InverterOperatingState> result = new LinkedHashSet<>();
		if ( names != null && !names.isEmpty() ) {
			for ( String n : names ) {
				try {
					result.add(InverterOperatingState.valueOf(n));
				} catch ( IllegalArgumentException e ) {
					log.warn("Ignoring invalid InverterOperatingState {}", n);
				}
			}
		}
		if ( result.isEmpty() ) {
			setIgnoreStates(null);
		} else {
			setIgnoreStates(EnumSet.copyOf(result));
		}
	}

	/**
	 * Get the states to ignore, as a comma-delimited list of
	 * {@link InverterOperatingState} names.
	 *
	 * @return the state names list
	 */
	public String getIgnoreStatesValue() {
		return StringUtils.commaDelimitedStringFromCollection(ignoreStates);
	}

	/**
	 * Toggle the inclusion of phase measurement properties in collected datum.
	 *
	 * @param includePhaseMeasurements
	 *        {@literal true} to collect phase measurements
	 * @since 2.1
	 */
	public void setIncludePhaseMeasurements(boolean includePhaseMeasurements) {
		this.includePhaseMeasurements = includePhaseMeasurements;
	}

}
