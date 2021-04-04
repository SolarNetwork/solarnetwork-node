/* ==================================================================
 * InverterNameplateRatingsRegister.java - 15/10/2018 11:27:01 AM
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

package net.solarnetwork.node.hw.sunspec.inverter;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.DistributedEnergyResourceType;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for SunSpec model 120.
 * 
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public enum InverterNameplateRatingsRegister implements SunspecModbusReference {

	/** The DER type, see {@link DistributedEnergyResourceType}. */
	DerType(0, UInt16, Enumeration),

	// Active (Real) Power (W)

	/** Active power rating, reported in W. */
	ActivePowerRating(1, UInt16),

	/** Active power rating scale factor, as *10^X. */
	ScaleFactorActivePowerRating(2, Int16, ScaleFactor),

	// Apparent Power (VA)

	/** Apparent power rating, reported in VA. */
	ApparentPowerRating(3, UInt16),

	/** Apparent power rating scale factor, as *10^X. */
	ScaleFactorApparentPowerRating(4, Int16, ScaleFactor),

	// Reactive Power (VAR)

	/** Reactive power rating Q1, reported in VAR. */
	ReactivePowerQ1Rating(5, Int16),

	/** Reactive power rating Q2, reported in VAR. */
	ReactivePowerQ2Rating(6, Int16),

	/** Reactive power rating Q3, reported in VAR. */
	ReactivePowerQ3Rating(7, Int16),

	/** Reactive power rating Q4, reported in VAR. */
	ReactivePowerQ4Rating(8, Int16),

	/** Reactive power rating scale factor, as *10^X. */
	ScaleFactorReactivePowerRating(9, Int16, ScaleFactor),

	// Current

	/** AC current rating, in A. */
	CurrentRating(10, UInt16),

	/** AC current rating scale factor, as *10^X. */
	ScaleFactorCurrentRating(11, Int16, ScaleFactor),

	// Power factor

	/** AC power factor rating Q1, reported a a decimal percentage -1..1. */
	PowerFactorQ1Rating(12, Int16),

	/** AC power factor rating Q2, reported a a decimal percentage -1..1. */
	PowerFactorQ2Rating(13, Int16),

	/** AC power factor rating Q3, reported a a decimal percentage -1..1. */
	PowerFactorQ3Rating(14, Int16),

	/** AC power factor rating Q4, reported a a decimal percentage -1..1. */
	PowerFactorQ4Rating(15, Int16),

	/** AC power factor rating scale factor, as *10^X. */
	ScaleFactorPowerFactorRating(16, Int16, ScaleFactor),

	// Energy storage

	/** Stored energy capacity, in Wh. */
	StoredEnergyRating(17, UInt16),

	/** Stored energy capacity scale factor, as *10^X. */
	ScaleFactorStoredEnergyRating(18, Int16, ScaleFactor),

	/** Stored charge capacity, in Ah. */
	StoredChargeCapacity(19, UInt16),

	/** Stored charge capacity scale factor, as *10^X. */
	ScaleFactorStoredChargeCapacity(20, Int16, ScaleFactor),

	/** Stored energy maximum charge rating, in W. */
	StoredEnergyImportPowerRating(21, UInt16),

	/** Stored energy maximum charge rating scale factor, as *10^X. */
	ScaleFactorStoredEnergyImportPowerRating(22, Int16, ScaleFactor),

	/** Stored energy maximum discharge rating, in W. */
	StoredEnergyExportPowerRating(23, UInt16),

	/** Stored energy maximum discharge rating scale factor, as *10^X. */
	ScaleFactorStoredEnergyExportPowerRating(24, UInt16, ScaleFactor);

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private InverterNameplateRatingsRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private InverterNameplateRatingsRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private InverterNameplateRatingsRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private InverterNameplateRatingsRegister(int address, ModbusDataType dataType, int wordLength,
			DataClassification classification) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
		this.classification = classification;
	}

	@Override
	public int getAddress() {
		return address;
	}

	@Override
	public ModbusDataType getDataType() {
		return dataType;
	}

	@Override
	public ModbusReadFunction getFunction() {
		return ModbusReadFunction.ReadHoldingRegister;
	}

	/**
	 * Get the data type word length.
	 * 
	 * @return the word length
	 */
	@Override
	public int getWordLength() {
		return wordLength;
	}

	@Override
	public DataClassification getClassification() {
		return classification;
	}

}
