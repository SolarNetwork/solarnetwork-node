/* ==================================================================
 * Stabiliti30cData.java - 29/08/2019 10:38:26 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.idealpower.pc;

import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cUtils.ABORT2_SEVERITY;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cUtils.SORT_BY_FAULT_SEVERITY;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation for Stabiliti 30C series power control system data.
 * 
 * @author matt
 * @version 1.0
 */
public class Stabiliti30cData extends ModbusData implements Stabiliti30cDataAccessor {

	/**
	 * Default constructor.
	 */
	public Stabiliti30cData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public Stabiliti30cData(ModbusData other) {
		super(other);
	}

	/**
	 * Get a snapshot copy of this data.
	 * 
	 * @return the copy of data
	 */
	public Stabiliti30cData getSnapshot() {
		return new Stabiliti30cData(this);
	}

	@Override
	public ModbusData copy() {
		return getSnapshot();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		Stabiliti30cDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(8);
		String fwVersion = data.getFirmwareVersion();
		if ( fwVersion != null ) {
			result.put("Firmware Version", fwVersion);
		}
		String commVersion = data.getCommunicationsVersion();
		if ( commVersion != null ) {
			result.put("Comms Version", commVersion);
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		SortedSet<Stabiliti30cFault> faults = data.getFaults();
		if ( faults != null && !faults.isEmpty() ) {
			SortedSet<Stabiliti30cFault> faultsBySeverity = new TreeSet<>(SORT_BY_FAULT_SEVERITY);
			faultsBySeverity.addAll(faults);
			SortedSet<Stabiliti30cFault> severeFaults = faultsBySeverity.tailSet(ABORT2_SEVERITY);
			result.put("Abort2+ faults", commaDelimitedStringFromCollection(severeFaults));
		}
		return result;
	}

	private Float getCentiValueAsFloat(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.floatValue() / 10;
	}

	private Integer getCentaValueAsInteger(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.intValue() * 10;
	}

	@Override
	public Stabiliti30cAcPortType getP1PortType() {
		Number n = getNumber(Stabiliti30cRegister.ConfigP1PortType);
		if ( n != null ) {
			try {
				return Stabiliti30cAcPortType.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	@Override
	public Integer getP1ActivePower() {
		return getCentaValueAsInteger(Stabiliti30cRegister.PowerControlP1RealPower);
	}

	@Override
	public Float getP2Voltage() {
		Number n = getNumber(Stabiliti30cRegister.PowerControlP2Voltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getP2Power() {
		return getCentaValueAsInteger(Stabiliti30cRegister.PowerControlP2Power);
	}

	@Override
	public Float getP2Current() {
		return getCentiValueAsFloat(Stabiliti30cRegister.PowerControlP2Current);
	}

	@Override
	public Float getP3Voltage() {
		Number n = getNumber(Stabiliti30cRegister.PowerControlP3Voltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getP3Power() {
		return getCentaValueAsInteger(Stabiliti30cRegister.PowerControlP3Power);
	}

	@Override
	public Float getP3Current() {
		return getCentiValueAsFloat(Stabiliti30cRegister.PowerControlP3Current);
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(Stabiliti30cRegister.InfoSerialNumber, true);
	}

	@Override
	public String getFirmwareVersion() {
		Number major = getNumber(Stabiliti30cRegister.InfoFirmwareVersion);
		Number minor = getNumber(Stabiliti30cRegister.InfoBuildVersion);
		return (major != null && minor != null ? major.toString() + "." + minor.toString() : null);
	}

	@Override
	public String getCommunicationsVersion() {
		Number major = getNumber(Stabiliti30cRegister.InfoCommsVersion);
		Number minor = getNumber(Stabiliti30cRegister.InfoCommsBuildVersion);
		return (major != null && minor != null ? major.toString() + "." + minor.toString() : null);
	}

	@Override
	public Set<Stabiliti30cSystemInfo> getSystemInfo() {
		Number n = getNumber(Stabiliti30cRegister.StatusInfo);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), Stabiliti30cSystemInfo.class)
				: null);
	}

	@Override
	public Stabiliti30cOperatingMode getOperatingMode() {
		Number n = getNumber(Stabiliti30cRegister.StatusOperatingMode);
		return (n != null ? Stabiliti30cOperatingMode.forCode(n.intValue()) : null);
	}

	@Override
	public Set<Stabiliti30cSystemStatus> getSystemStatus() {
		Number n = getNumber(Stabiliti30cRegister.StatusSystem);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), Stabiliti30cSystemStatus.class)
				: null);
	}

	@Override
	public SortedSet<Stabiliti30cFault> getFaults() {
		Number f0 = getNumber(Stabiliti30cRegister.StatusFaultActive0);
		Number f1 = getNumber(Stabiliti30cRegister.StatusFaultActive1);
		Number f2 = getNumber(Stabiliti30cRegister.StatusFaultActive2);
		Number f3 = getNumber(Stabiliti30cRegister.StatusFaultActive3);
		return Stabiliti30cUtils.faultSet(f0 != null ? f0.intValue() : 0, f1 != null ? f1.intValue() : 0,
				f2 != null ? f2.intValue() : 0, f3 != null ? f3.intValue() : 0);
	}

}
