/* ==================================================================
 * AE250TxMainFault.java - 12/08/2021 9:28:24 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.tx;

/**
 * AE250TX system faults.
 * 
 * @author matt
 * @version 1.0
 * @since 3.2
 */
public enum AE250TxSystemFault implements AE250TxFault {

	Ground(0, "Ground fault, check the PV array field wiring."),

	AcContactor(1, "AC contactor fault."),

	DcContactor(2, "DC contactor fault."),

	Watchdog(3, "Watchdog timer fault."),

	CpuLoad(4, "CPU load fault."),

	RestartLimit(5, "Too many fault restarts."),

	Configuration(6, "Configuration fault."),

	CurrentImbalance(7, "AC current imbalance."),

	AcVoltageSense(8, "No AC voltage detected."),

	ThermalSwitchOpen(9, "Thermal switch open."),

	DisconnectOpen(10, "Disconnect open."),

	DcMiswire(11, "DC mis-wired for configured grounding, check DC wiring."),

	;

	private final int bit;
	private final String description;

	private AE250TxSystemFault(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
