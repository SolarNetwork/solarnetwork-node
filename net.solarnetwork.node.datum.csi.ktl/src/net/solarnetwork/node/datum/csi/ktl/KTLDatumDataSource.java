/* ==================================================================
 * KTLDatumDataSource.java - 23/11/2017 3:06:48 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csi.ktl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLData;
import net.solarnetwork.node.hw.csi.inverter.KTLSupport;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodeACEnergyDatum}
 * with the CSI KTL inverter.
 * 
 * @author matt
 * @author maxieduncan
 * @version 1.0
 */
public class KTLDatumDataSource extends KTLSupport implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private String sourceId = "CSI";

	private KTLData getCurrentSample() {
		KTLData currSample = null;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(new ModbusConnectionAction<KTLData>() {

					@Override
					public KTLData doWithConnection(ModbusConnection connection) throws IOException {
						getSample().readInverterData(connection);
						return getSample().getSnapshot();
					}

				});
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read KTL data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
			}
		} else {
			currSample = getSample().getSnapshot();
		}
		return currSample;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final KTLData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		KTLDatum d = new KTLDatum(currSample);
		d.setSourceId(this.sourceId);
		if ( currSample.getInverterDataTimestamp() >= start ) {
			// we read from the inverter
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
		return "net.solarnetwork.node.datum.csi.ktl";
	}

	@Override
	public String getDisplayName() {
		return "CSI KTL Inverter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();

		KTLDatumDataSource defaults = new KTLDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

		return results;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
