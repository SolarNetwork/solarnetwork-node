/* ==================================================================
 * EM5600Data.java - Mar 26, 2014 4:13:48 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.hc;

import java.util.Arrays;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusHelper;

/**
 * Encapsulates raw Modbus register data from the EM5600 meters.
 * 
 * @author matt
 * @version 1.2
 */
public class EM5600Data {

	// meter info
	public static final int ADDR_SYSTEM_METER_MODEL = 0x0;
	public static final int ADDR_SYSTEM_METER_HARDWARE_VERSION = 0x2; // length 2 ASCII characters
	public static final int ADDR_SYSTEM_METER_SERIAL_NUMBER = 0x10; // length 4 ASCII characters
	public static final int ADDR_SYSTEM_METER_MANUFACTURE_DATE = 0x18; // length 2 F10 encoding

	// current
	public static final int ADDR_DATA_I1 = 0x130;
	public static final int ADDR_DATA_I2 = 0x131;
	public static final int ADDR_DATA_I3 = 0x132;
	public static final int ADDR_DATA_I_AVERAGE = 0x133;

	// voltage
	public static final int ADDR_DATA_V_L1_NEUTRAL = 0x136;
	public static final int ADDR_DATA_V_L2_NEUTRAL = 0x137;
	public static final int ADDR_DATA_V_L3_NEUTRAL = 0x138;
	public static final int ADDR_DATA_V_NEUTRAL_AVERAGE = 0x139;
	public static final int ADDR_DATA_V_L1_L2 = 0x13B;
	public static final int ADDR_DATA_V_L2_L3 = 0x13C;
	public static final int ADDR_DATA_V_L3_L1 = 0x13D;
	public static final int ADDR_DATA_V_L_L_AVERAGE = 0x13E;

	// power
	public static final int ADDR_DATA_ACTIVE_POWER_TOTAL = 0x140;
	public static final int ADDR_DATA_REACTIVE_POWER_TOTAL = 0x141;
	public static final int ADDR_DATA_APPARENT_POWER_TOTAL = 0x142;
	public static final int ADDR_DATA_POWER_FACTOR_TOTAL = 0x143;
	public static final int ADDR_DATA_FREQUENCY = 0x144;
	public static final int ADDR_DATA_ACTIVE_POWER_P1 = 0x145;
	public static final int ADDR_DATA_REACTIVE_POWER_P1 = 0x146;
	public static final int ADDR_DATA_APPARENT_POWER_P1 = 0x147;
	public static final int ADDR_DATA_POWER_FACTOR_P1 = 0x148;
	public static final int ADDR_DATA_ACTIVE_POWER_P2 = 0x149;
	public static final int ADDR_DATA_REACTIVE_POWER_P2 = 0x14A;
	public static final int ADDR_DATA_APPARENT_POWER_P2 = 0x14B;
	public static final int ADDR_DATA_POWER_FACTOR_P2 = 0x14C;
	public static final int ADDR_DATA_ACTIVE_POWER_P3 = 0x14D;
	public static final int ADDR_DATA_REACTIVE_POWER_P3 = 0x14E;
	public static final int ADDR_DATA_APPARENT_POWER_P3 = 0x14F;
	public static final int ADDR_DATA_POWER_FACTOR_P3 = 0x150;
	public static final int ADDR_DATA_PHASE_ROTATION = 0x151;

	// energy
	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 0x160; // length 2
	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT = 0x162;
	public static final int ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT = 0x164;
	public static final int ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT = 0x166;

	// units
	public static final int ADDR_DATA_ENERGY_UNIT = 0x17E;
	public static final int ADDR_DATA_PT_RATIO = 0x200A;
	public static final int ADDR_DATA_CT_RATIO = 0x200B;

	private static final int ADDR_INPUT_REG_START = ADDR_DATA_I1;
	private static final int ADDR_INPUT_REG_END = ADDR_DATA_ENERGY_UNIT;

	private final int[] inputRegisters;
	private float ptRatio = 1;
	private float ctRatio = 1;
	private int energyUnit;
	private int energyFactor;
	private UnitFactor unitFactor = UnitFactor.EM5610;
	private long dataTimestamp = 0;
	private boolean backwards;

	/**
	 * Default constructor.
	 */
	public EM5600Data() {
		super();
		setEnergyUnit(0);
		inputRegisters = new int[ADDR_INPUT_REG_END - ADDR_INPUT_REG_START + 1];
		Arrays.fill(inputRegisters, 0);
		this.backwards = false;
	}

	/**
	 * Default constructor.
	 */
	public EM5600Data(boolean backwards) {
		super();
		setEnergyUnit(0);
		inputRegisters = new int[ADDR_INPUT_REG_END - ADDR_INPUT_REG_START + 1];
		Arrays.fill(inputRegisters, 0);
		this.backwards = backwards;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public EM5600Data(EM5600Data other) {
		super();
		inputRegisters = other.inputRegisters.clone();
		ptRatio = other.ptRatio;
		ctRatio = other.ctRatio;
		energyUnit = other.energyUnit;
		energyFactor = other.energyFactor;
		unitFactor = other.unitFactor;
		dataTimestamp = other.dataTimestamp;
		backwards = other.backwards;
	}

	@Override
	public String toString() {
		return "EM5600Data{U=" + unitFactor + ",PTR=" + ptRatio + ",CTR=" + ctRatio + ",EU=" + energyUnit
				+ ",EF=" + energyFactor + ",V1=" + getVoltage(ADDR_DATA_V_L1_NEUTRAL) + ",V2="
				+ getVoltage(ADDR_DATA_V_L2_NEUTRAL) + ",V3=" + getVoltage(ADDR_DATA_V_L3_NEUTRAL)
				+ ",A1=" + getCurrent(ADDR_DATA_I1) + ",A2=" + getCurrent(ADDR_DATA_I2) + ",A3="
				+ getCurrent(ADDR_DATA_I3) + ",PF=" + getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL)
				+ ",Hz=" + getFrequency(ADDR_DATA_FREQUENCY) + ",W="
				+ getPower(ADDR_DATA_ACTIVE_POWER_TOTAL) + ",var="
				+ getPower(ADDR_DATA_REACTIVE_POWER_TOTAL) + ",VA="
				+ getPower(ADDR_DATA_APPARENT_POWER_TOTAL) + ",Wh-I="
				+ getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT) + ",varh-I="
				+ getEnergy(ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT) + ",Wh-E="
				+ getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT) + ",varh-E="
				+ getEnergy(ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT) + "}";
	}

	/**
	 * Read data from the meter and store it internally. If data is populated
	 * successfully, the {@link dataTimestamp} will be updated to the current
	 * system time. <b>Note</b> this does <b>not</b> call
	 * {@link #readEnergyRatios(ModbusConnection)}. Those values are not
	 * expected to change much, so those values should be called manually as
	 * needed.
	 * 
	 * @param conn
	 *        the Modbus connection
	 */
	public void readMeterData(final ModbusConnection conn) {
		int[] data = conn.readInts(ADDR_DATA_I1, (ADDR_DATA_PHASE_ROTATION - ADDR_DATA_I1 + 1));

		// re-read some data to get signed values... these are mixed with unsigned values
		// so we are reading some registers twice, but in fewer transactions
		short[] signedData = conn.readSignedShorts(ADDR_DATA_ACTIVE_POWER_TOTAL,
				(ADDR_DATA_POWER_FACTOR_P3 - ADDR_DATA_APPARENT_POWER_TOTAL + 1));
		final int signedDataOffset = ADDR_DATA_ACTIVE_POWER_TOTAL - ADDR_DATA_I1;
		for ( int i = 0; i < signedData.length; i++ ) {
			final int addr = ADDR_DATA_ACTIVE_POWER_TOTAL + i;
			switch (addr) {
				case ADDR_DATA_APPARENT_POWER_P1:
				case ADDR_DATA_APPARENT_POWER_P2:
				case ADDR_DATA_APPARENT_POWER_P3:
				case ADDR_DATA_APPARENT_POWER_TOTAL:
				case ADDR_DATA_FREQUENCY:
					// these are unsigned values, so skip as they were populated in the first tx
					continue;

				default:
					data[i + signedDataOffset] = signedData[i];
			}
		}
		setCurrentVoltagePower(data);

		data = conn.readInts(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT,
				(ADDR_DATA_ENERGY_UNIT - ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT + 1));
		setEnergy(data);
		dataTimestamp = System.currentTimeMillis();
	}

	/**
	 * Read the PT ratio, and CT ratio values from the meter. If the
	 * {@code unitFactor} is set to {@link UnitFactor#EM5610} then this method
	 * will not actually query the meter, as the values are fixed for that
	 * meter.
	 * 
	 * @param conn
	 *        the Modbus connection
	 */
	public void readEnergyRatios(final ModbusConnection conn) {
		int[] eUnit = conn.readInts(ADDR_DATA_ENERGY_UNIT, 1);
		if ( eUnit != null && eUnit.length > 0 ) {
			// a value of 0 here means we should treat the energy unit as 1, e.g. 5610
			int eu = eUnit[0];
			inputRegisters[ADDR_DATA_ENERGY_UNIT - ADDR_INPUT_REG_START] = eu;
			setEnergyUnit(eu < 0 ? 0 : eu);
		}
		int[] transformerRatios = conn.readInts(ADDR_DATA_PT_RATIO, 2);
		if ( transformerRatios != null && transformerRatios.length > 1 ) {
			int ptr = transformerRatios[0];
			ptRatio = (ptr < 1 ? 1 : ptr / 10);
			int ctr = transformerRatios[1];
			ctRatio = (ctr < 1 ? 1 : ctr / 10);
		}
	}

	/**
	 * Get the system time for the last time data was successfully populated via
	 * {@link #readMeterData(ModbusConnection)}.
	 * 
	 * @return the system time
	 */
	public long getDataTimestamp() {
		return dataTimestamp;
	}

	/**
	 * Set the raw Modbus current, voltage, and power register data. This
	 * corresponds to the register range 0x130 - 0x151.
	 * 
	 * @param current
	 *        the data
	 */
	public void setCurrentVoltagePower(int[] data) {
		if ( data == null ) {
			return;
		}
		System.arraycopy(data, 0, inputRegisters, (ADDR_DATA_I1 - ADDR_INPUT_REG_START),
				Math.min(data.length, (ADDR_DATA_PHASE_ROTATION - ADDR_DATA_I1 + 1)));
	}

	/**
	 * Set the raw Modbus energy register data. This corresponds to the register
	 * range 0x160 - 0x17E.
	 * 
	 * @param power
	 *        the data
	 */
	public void setEnergy(int[] energy) {
		if ( energy == null ) {
			return;
		}
		System.arraycopy(energy, 0, inputRegisters,
				(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT - ADDR_INPUT_REG_START), Math.min(energy.length,
						(ADDR_DATA_ENERGY_UNIT - ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT + 1)));
	}

	/**
	 * Get the value of an input register. This will not return useful data
	 * until after {@link #readMeterData(ModbusConnection)} has been called. Use
	 * the various {@code ADDR_*} constants to query specific supported
	 * registers.
	 * 
	 * @param addr
	 *        the input register address to get the value of
	 * @return the register value
	 * @throws IllegalArgumentException
	 *         if the provided address is out of range
	 */
	public int getInputRegister(final int addr) {
		inputRegisterRangeCheck(addr);
		return inputRegisters[addr - ADDR_DATA_I1];
	}

	private void inputRegisterRangeCheck(final int addr) {
		if ( addr < ADDR_INPUT_REG_START || addr > ADDR_INPUT_REG_END ) {
			throw new IllegalArgumentException("Input register ddress " + addr + " out of range");
		}
	}

	/**
	 * Get the {@link UnitFactor} to use for calculating effective values.
	 * 
	 * @return the unit factor
	 */
	public UnitFactor getUnitFactor() {
		return unitFactor;
	}

	/**
	 * Set the {@link UnitFactor} to use for calculating effective values. This
	 * defaults to {@link EM5610}.
	 * 
	 * @param unitFactor
	 */
	public void setUnitFactor(UnitFactor unitFactor) {
		assert unitFactor != null;
		this.unitFactor = unitFactor;
	}

	/**
	 * Get the effective PT ratio. This will return {@code 1} unless the
	 * {@code unitFactor} is set to {@code EM5630_5A}, in which case it will
	 * return {@link #getPtRatio()} which has presumably be set by reading from
	 * the meter via {@link #readEnergyRatios(ModbusConnection)}.
	 * 
	 * @return effective PT ratio
	 */
	public float getEffectivePtRatio() {
		return (unitFactor == UnitFactor.EM5630_5A ? ptRatio : 1);
	}

	/**
	 * Get the effective CT ratio. This will return {@code 1} unless the
	 * {@code unitFactor} is set to {@code EM5630_5A}, in which case it will
	 * return {@link #getCtRatio()} which has presumably be set by reading from
	 * the meter via {@link #readEnergyRatios(ModbusConnection)}.
	 * 
	 * @return effective PT ratio
	 */
	public float getEffectiveCtRatio() {
		return (unitFactor == UnitFactor.EM5630_5A ? ctRatio : 1);
	}

	/**
	 * Get an effective voltage value in V.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a voltage value
	 */
	public float getVoltage(int addr) {
		return (getInputRegister(addr) * getEffectivePtRatio() * unitFactor.getU().floatValue());
	}

	/**
	 * Get an effective current value in A.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a current value
	 */
	public float getCurrent(int addr) {
		return (getInputRegister(addr) * getEffectiveCtRatio() * unitFactor.getA().floatValue());
	}

	/**
	 * Get an effective frequency value in Hz.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a frequency value
	 */
	public float getFrequency(int addr) {
		return (getInputRegister(addr) * 2) / 1000f;
	}

	/**
	 * Get an effective power factor value (cosine of the phase angle).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power factor value
	 */
	public float getPowerFactor(int addr) {
		return (getInputRegister(addr) / 10000f);
	}

	/**
	 * Get an effective power value in W (active), Var (reactive) or VA
	 * (apparent).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power value
	 */
	public int getPower(int addr) {
		return (int) Math.ceil(getInputRegister(addr) * getEffectivePtRatio() * getEffectiveCtRatio()
				* unitFactor.getP().floatValue());
	}

	/**
	 * Get an effective energy value in Wh (real), Varh (reactive).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as an energy value
	 */
	public long getEnergy(int addr) {
		inputRegisterRangeCheck(addr);
		inputRegisterRangeCheck(addr + 1);
		Long l = ModbusHelper.parseInt32(inputRegisters, addr - ADDR_INPUT_REG_START);
		return (l == null ? 0 : l.longValue() * energyFactor);
	}

	public float getPtRatio() {
		return ptRatio;
	}

	public float getCtRatio() {
		return ctRatio;
	}

	public int getEnergyUnit() {
		return energyUnit;
	}

	public void setPtRatio(int ptRatio) {
		this.ptRatio = ptRatio;
	}

	public void setCtRatio(int ctRatio) {
		this.ctRatio = ctRatio;
	}

	public void setEnergyUnit(int energyUnit) {
		assert energyUnit >= 0 && energyUnit <= 6;
		this.energyUnit = energyUnit;
		this.energyFactor = (int) Math.pow(10, energyUnit);
	}

	/**
	 * Get a string of data values, useful for debugging. The generated string
	 * will contain a register address followed by two register values per line,
	 * printed as hexidecimal integers, with a prefix and suffix line. The
	 * register addresses will be printed as {@bold 1-based} values, to match
	 * Schneider's documentation. For example:
	 * 
	 * <pre>
	 * EM5600Data{
	 *      3000: 0x4141, 0x727E
	 *      3002: 0xFFC0, 0x0000
	 *      ...
	 *      3240: 0x0000, 0x0000
	 * }
	 * </pre>
	 * 
	 * @return debug string
	 */
	public String dataDebugString() {
		final StringBuilder buf = new StringBuilder("EM5600Data{\n");
		EM5600Data snapshot = new EM5600Data(this);
		int[] data = snapshot.inputRegisters;
		boolean odd = true;
		for ( int i = 0; i < data.length; i++ ) {
			if ( odd ) {
				buf.append("\t").append(String.format("%5d", ADDR_INPUT_REG_START + i + 1)).append(": ");
			}
			buf.append(String.format("0x%04X", data[i]));
			if ( odd ) {
				buf.append(", ");
			} else {
				buf.append("\n");
			}
			odd = !odd;
		}
		if ( !odd ) {
			buf.append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	/**
	 * Populate measurements into a {@link GeneralNodeACEnergyDatum} instance.
	 * 
	 * @param phase
	 *        the phase to populate data for
	 * @param datum
	 *        the datum to populate data into
	 * @since 1.2
	 */
	public void populateMeasurements(final ACPhase phase, final GeneralNodeACEnergyDatum datum) {
		switch (phase) {
			case Total:
				populateTotalMeasurements(this, datum);
				break;

			case PhaseA:
				populatePhaseAMeasurements(this, datum);
				break;

			case PhaseB:
				populatePhaseBMeasurements(this, datum);
				break;

			case PhaseC:
				populatePhaseCMeasurements(this, datum);
				break;
		}
	}

	private static void populateTotalMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		final boolean backwards = sample.isBackwards();

		datum.setEffectivePowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL));
		datum.setFrequency(sample.getFrequency(ADDR_DATA_FREQUENCY));

		Long whImport = sample.getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT);
		Long whExport = sample.getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT);

		if ( backwards ) {
			datum.setWattHourReading(whExport);
			datum.setReverseWattHourReading(whImport);
		} else {
			datum.setWattHourReading(whImport);
			datum.setReverseWattHourReading(whExport);
		}

		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_TOTAL));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I_AVERAGE));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L_L_AVERAGE));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_TOTAL));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_NEUTRAL_AVERAGE));
		datum.setWatts((backwards ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
	}

	private static void populatePhaseAMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P1));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I1));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L1_L2));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P1));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P1));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P1));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L1_NEUTRAL));
		datum.setWatts((sample.isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P1));
	}

	private static void populatePhaseBMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P2));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I2));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L2_L3));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P2));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P2));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P2));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L2_NEUTRAL));
		datum.setWatts((sample.isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P2));
	}

	private static void populatePhaseCMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P3));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I3));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L3_L1));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P3));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P3));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P3));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L3_NEUTRAL));
		datum.setWatts((sample.isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P3));
	}

	/**
	 * Toggle the backwards flag.
	 * 
	 * @param backwards
	 *        {@literal true} to set into "installed backwards" mode
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

	/**
	 * Get the backwards flag.
	 * 
	 * @return backwards flag
	 * @since 1.2
	 */
	public boolean isBackwards() {
		return backwards;
	}
}
