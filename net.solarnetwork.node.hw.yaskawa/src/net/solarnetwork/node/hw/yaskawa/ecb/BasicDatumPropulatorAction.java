/* ==================================================================
 * BasicDatumPropulatorAction.java - 18/05/2018 5:33:37 PM
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

package net.solarnetwork.node.hw.yaskawa.ecb;

import static net.solarnetwork.node.domain.EnergyDatum.WATTS_KEY;
import static net.solarnetwork.node.domain.EnergyDatum.WATT_HOUR_READING_KEY;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;

/**
 * Serial port connection action to populate values onto a new
 * {@link GeneralNodeACEnergyDatum} object.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumPropulatorAction implements SerialConnectionAction<GeneralNodeACEnergyDatum> {

	private final int unitId;

	public BasicDatumPropulatorAction(int unitId) {
		super();
		this.unitId = unitId;
	}

	@Override
	public GeneralNodeACEnergyDatum doWithConnection(SerialConnection conn) throws IOException {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setCreated(new Date());

		Packet power = PacketUtils.sendPacket(conn,
				PVI3800Command.MeterReadAcCombinedActivePower.request(unitId));
		if ( power != null ) {
			d.putInstantaneousSampleValue(WATTS_KEY, new BigInteger(1, power.getBody()));
		}

		Packet energy = PacketUtils.sendPacket(conn,
				PVI3800Command.MeterReadLifetimeTotalEnergy.request(unitId));
		if ( power != null ) {
			d.putAccumulatingSampleValue(WATT_HOUR_READING_KEY, new BigInteger(1, energy.getBody()));
		}

		return d;
	}

}
