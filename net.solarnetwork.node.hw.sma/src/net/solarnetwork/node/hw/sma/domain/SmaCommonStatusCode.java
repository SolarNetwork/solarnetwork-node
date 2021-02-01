/* ==================================================================
 * SmaCommonStatusCode.java - 14/09/2020 8:22:29 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.domain;

/**
 * Common statuc code values used in various enumerations.
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaCommonStatusCode implements SmaCodedValue {

	Unknown(-1),

	Closed(51),

	InstantaneousValue(276, "Instantaneous value"),

	Mpp(295, "MPP"),

	Off(303),

	On(308),

	Operation(309),

	Open(311),

	ContactManufacturer(336, "Contact manufacturer"),

	ContactInstaller(337, "Contact installer"),

	Invalid(338),

	Stop(381),

	ConstantVoltage(443, "Constant voltage"),

	Warning(455),

	SMA(461, "Manufacturer specification"),

	TemperatureDerating(557, "Temperature derating is active"),

	PowerSpecificationViaCurve(565, "Power specification via characteristic curve"),

	NotSet(973, "Not set"),

	Capacitive(1041),

	Inductive(1042),

	ReactivPowerVoltageCharacteristic(1069, "Reactive power/Voltage characteristic Q(U)"),

	ReacitvePowerQDirectDefault(1070, "Reactive power Q, direct default setting"),

	ReactivePowerConstantQ(1071, "Reactive power const. Q (kvar)"),

	ReacitvePowerQPlantDefault(1072, "Reactive power Q, default setting via plant control"),

	ReactivePowerQ(1073, "Reactive power Q(P)"),

	PowerFactorDirect(1074, "cos φ, direct specification"),

	PowerFactorPlantDefault(1075, "cos φ, default setting via plant control"),

	PowerFactorCurve(1076, "cos φ(P) characteristic curve"),

	ActivePowerLimit(1077, "Active power limitation P (W)"),

	ActivePowerLimitPercent(1078, "Active power limitation P in (%) of PMAX"),

	ActivePowerLimitPlant(1079, "Active power limitation P via plant control"),

	ReactivePowerQAnalogDefault(1387, "Reactive power Q, default setting via analog input"),

	PowerFactorAnalogDefault(1388, "cos φ, default setting via analog input"),

	ReactivePowerVoltageCharacteristicWithHysteresis(
			1389,
			"Reactive power/Voltage characteristic curve Q(U) with hysteresis and deadband"),

	ActivePowerLimitAnalog(1390, "Active power limitation P via analog input"),

	ActivePowerLimitDigital(1391, "Active power limitation P via digital inputs"),

	Error(1392),

	WaitForPvVoltage(1393, "Wait for PV voltage"),

	WaitForAcGrid(1394, "Wait for valid AC grid"),

	DcRange(1395, "DC range"),

	AcGrid(1396, "AC grid"),

	GridMode(1440, "Grid mode"),

	SeparateGridMode(1441, "Separate grid mode"),

	PhaseGuard(1442, "Phase guard"),

	PowerGuard(1443, "Power guard"),

	FaultGaurd(1444, "Fault gaurd"),

	EmergencyStop(1455, "Emergency Stop"),

	Waiting(1466),

	Starting(1467),

	MppSearch(1468, "MPP search"),

	ShutDown(1469, "Shut-down"),

	Disruption(1470),

	WarningErrorMailOk(1471, "Warning/Error mail OK"),

	WarningErrorMailError(1472, "Warning/Error mail not OK"),

	PlantInfoMailOk(1473, "Plant information mail OK"),

	PlantInfoMailError(1474, "Plant information mail not OK"),

	ErrorMailOk(1475, "Error mail OK"),

	ErrorMailError(1476, "Error mail not OK"),

	WarningMailOk(1477, "Warning mail OK"),

	WarningMailError(1478, "Warning mail not OK"),

	WaitAfterGirdInterruption(1479, "Wait after grid interruption"),

	WaitForElectricitySupplier(1480, "Wait for electricity supplier"),

	PowerBalancing(2100, "Power limitation to avoid unbalanced load"),

	;

	private final int code;
	private final String description;

	private SmaCommonStatusCode(int code) {
		this.code = code;
		this.description = this.name();
	}

	private SmaCommonStatusCode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration value for a code value.
	 * 
	 * @param code
	 *        the code
	 * @return the enumeration, never {@literal null} and set to
	 *         {@link #Unknown} if not any other valid code
	 */
	public static SmaCommonStatusCode forCode(int code) {
		for ( SmaCommonStatusCode v : values() ) {
			if ( v.code == code ) {
				return v;
			}
		}
		return SmaCommonStatusCode.Unknown;
	}

}
