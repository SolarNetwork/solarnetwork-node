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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.OperatingState;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.hw.sunspec.support.SunSpecDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * {@link DatumDataSource} for a SunSpec compatible inverter.
 * 
 * @author matt
 * @version 1.0
 */
public class SunSpecInverterDatumDataSource extends SunSpecDeviceDatumDataSourceSupport
		implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private Set<InverterOperatingState> ignoreStates = EnumSet.of(InverterOperatingState.Off,
			InverterOperatingState.Sleeping, InverterOperatingState.Standby);

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
		setSourceId("SunSpec-Inverter");
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
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final ModelData currSample = getCurrentSample();
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
		SunSpecInverterDatum d = new SunSpecInverterDatum(data, ACPhase.Total);
		d.setSourceId(getSourceId());
		if ( currSample.getDataTimestamp() >= start ) {
			// we read from the device
			postDatumCapturedEvent(d);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		GeneralNodeACEnergyDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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
		if ( sample == null || sample.getDataTimestamp() < 1 ) {
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
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(data.getDataTimestamp())));
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

}
