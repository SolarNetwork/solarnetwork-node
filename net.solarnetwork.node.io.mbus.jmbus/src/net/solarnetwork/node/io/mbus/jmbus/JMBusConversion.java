/* ==================================================================
 * JMBusConversion.java - 01/07/2020 12:48:05 pm
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

package net.solarnetwork.node.io.mbus.jmbus;

import java.time.Instant;
import org.openmuc.jmbus.Bcd;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DataRecord.Description;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataRecord;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.io.mbus.MBusMessage;
import net.solarnetwork.node.io.mbus.MBusSecondaryAddress;

/**
 * Helper functions for converting objects to and from JMBus.
 * 
 * @author alex
 * @version 2.1
 */
public final class JMBusConversion {

	/** A class-level logger. */
	private static final Logger log = LoggerFactory.getLogger(JMBusConversion.class);

	private JMBusConversion() {
		// not available
	}

	/**
	 * Convert a secondary address to JMBus
	 * 
	 * @param address
	 *        The secondary address to convert
	 * @return JMBus secondary address
	 */
	public static SecondaryAddress to(MBusSecondaryAddress address) {
		return SecondaryAddress.newFromLongHeader(address.getBytes(), 0);
	}

	/**
	 * Convert a JMBus secondary address
	 * 
	 * @param address
	 *        The JMBus secondary address to convert
	 * @return Secondary address
	 */
	public static MBusSecondaryAddress from(SecondaryAddress address) {
		return new MBusSecondaryAddress(address.asByteArray());
	}

	/**
	 * Create a data instance from variable data.
	 * 
	 * @param vds
	 *        the variable data
	 * @return the new data
	 */
	public static MBusData from(VariableDataStructure vds) {
		try {
			final MBusData data = new MBusData(Instant.now());
			data.status = vds.getStatus();
			vds.decode();
			for ( DataRecord record : vds.getDataRecords() ) {
				final MBusDataRecord rec = from(record);
				if ( rec != null ) {
					data.dataRecords.add(rec);
				}
			}
			return data;
		} catch ( DecodingException e ) {
			log.error("Failed to decode JMBus variable data structure: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Convert a JMBus WMBus message
	 * 
	 * @param message
	 *        The JMBus WMBus message to convert
	 * @return A message
	 */
	public static MBusMessage from(WMBusMessage message) {
		final MBusMessage msg = new MBusMessage(Instant.now());
		msg.status = message.getVariableDataResponse().getStatus();

		final VariableDataStructure vds = message.getVariableDataResponse();
		try {
			vds.decode();
		} catch ( DecodingException e ) {
			log.error("Failed to decode JMBus variable data structure: {}", e.getMessage());
		}
		for ( DataRecord record : vds.getDataRecords() ) {
			final MBusDataRecord rec = from(record);
			if ( rec != null ) {
				msg.dataRecords.add(rec);
			}
		}

		msg.moreRecordsFollow = message.getVariableDataResponse().moreRecordsFollow();
		return msg;
	}

	private static MBusDataRecord from(DataRecord record) {
		final MBusDataDescription description = from(record.getDescription());
		if ( description == null ) {
			return null;
		}
		switch (record.getDataValueType()) {
			case BCD:
				return new MBusDataRecord(description, MBusDataType.BCD,
						((Bcd) record.getDataValue()).longValue(), record.getMultiplierExponent());
			case DATE:
				return new MBusDataRecord(description,
						((java.util.Date) record.getDataValue()).toInstant());
			case DOUBLE:
				return new MBusDataRecord(description, (Double) record.getDataValue(),
						record.getMultiplierExponent());
			case LONG:
				return new MBusDataRecord(description, MBusDataType.Long, (Long) record.getDataValue(),
						record.getMultiplierExponent());
			case STRING:
				return new MBusDataRecord(description, (String) record.getDataValue());
			case NONE:
				return new MBusDataRecord();
			default:
				break;
		}
		return null;
	}

	private static MBusDataDescription from(Description description) {
		switch (description) {
			case ACCSESS_CODE_OPERATOR:
				return MBusDataDescription.AccessCodeOperator;
			case ACCSESS_CODE_SYSTEM_DEVELOPER:
				return MBusDataDescription.AccessCodeSystemDeveloper;
			case ACCSESS_CODE_SYSTEM_OPERATOR:
				return MBusDataDescription.AccessCodeSytemOperator;
			case ACCSESS_CODE_USER:
				return MBusDataDescription.AccessCodeUser;
			case ACTUALITY_DURATION:
				return MBusDataDescription.ActualityDuration;
			case ADDRESS:
				return MBusDataDescription.Address;
			case APPARENT_ENERGY:
				return MBusDataDescription.ApparentEnergy;
			case AVERAGING_DURATION:
				return MBusDataDescription.AveragingDuration;
			case BAUDRATE:
				return MBusDataDescription.BaudRate;
			case CONTROL_SIGNAL:
				return MBusDataDescription.ControlSignal;
			case CUMULATION_COUNTER:
				return MBusDataDescription.CumulationCounter;
			case CURRENT:
				return MBusDataDescription.Current;
			case CUSTOMER:
				return MBusDataDescription.Customer;
			case CUSTOMER_LOCATION:
				return MBusDataDescription.CustomerLocation;
			case DATE:
				return MBusDataDescription.Date;
			case DATE_TIME:
				return MBusDataDescription.DateTime;
			case DAY_OF_WEEK:
				return MBusDataDescription.DayOfWeek;
			case DIGITAL_INPUT:
				return MBusDataDescription.DigitalInput;
			case DIGITAL_OUTPUT:
				return MBusDataDescription.DigitalOutput;
			case DURATION_LAST_READOUT:
				return MBusDataDescription.DurationLastReadout;
			case ENERGY:
				return MBusDataDescription.Energy;
			case ERROR_FLAGS:
				return MBusDataDescription.ErrorFlags;
			case ERROR_MASK:
				return MBusDataDescription.ErrorMask;
			case EXTENDED_IDENTIFICATION:
				return MBusDataDescription.ExtendedIdentification;
			case EXTERNAL_TEMPERATURE:
				return MBusDataDescription.ExternalTemperature;
			case FABRICATION_NO:
				return MBusDataDescription.FabricationNo;
			case FIRMWARE_VERSION:
				return MBusDataDescription.FirmwareVersion;
			case FIRST_STORAGE_NUMBER_CYCLIC:
				return MBusDataDescription.FirstStorageNumberCyclic;
			case FLOW_TEMPERATURE:
				return MBusDataDescription.FlowTemperature;
			case FREQUENCY:
				return MBusDataDescription.Frequency;
			case FUTURE_VALUE:
				return MBusDataDescription.Frequency;
			case HARDWARE_VERSION:
				return MBusDataDescription.HardwareVersion;
			case HCA:
				return MBusDataDescription.HCA;
			case LAST_CUMULATION_DURATION:
				return MBusDataDescription.LastCumulationDuration;
			case LAST_STORAGE_NUMBER_CYCLIC:
				return MBusDataDescription.LastStorageNumberCyclic;
			case MANUFACTURER_SPECIFIC:
				return MBusDataDescription.ManufacturerSpecific;
			case MASS:
				return MBusDataDescription.Mass;
			case MASS_FLOW:
				return MBusDataDescription.MassFlow;
			case MAX_POWER:
				return MBusDataDescription.MaxPower;
			case MODEL_VERSION:
				return MBusDataDescription.ModelVersion;
			case NOT_SUPPORTED:
				return MBusDataDescription.NotSupported;
			case NUMBER_STOPS:
				return MBusDataDescription.NumberStops;
			case ON_TIME:
				return MBusDataDescription.OnTime;
			case OPERATING_TIME:
				return MBusDataDescription.OperatingTime;
			case OPERATING_TIME_BATTERY:
				return MBusDataDescription.OperatingTimeBattery;
			case OPERATOR_SPECIFIC_DATA:
				return MBusDataDescription.OperatorSpecificData;
			case OTHER_SOFTWARE_VERSION:
				return MBusDataDescription.OtherSoftwareVersion;
			case PARAMETER_ACTIVATION_STATE:
				return MBusDataDescription.ParameterActivationState;
			case PARAMETER_SET_ID:
				return MBusDataDescription.ParameterSetId;
			case PASSWORD:
				return MBusDataDescription.Password;
			case PHASE:
				return MBusDataDescription.Phase;
			case POWER:
				return MBusDataDescription.Power;
			case PRESSURE:
				return MBusDataDescription.Pressure;
			case REACTIVE_ENERGY:
				return MBusDataDescription.ReactiveEnergy;
			case REACTIVE_POWER:
				return MBusDataDescription.ReactivePower;
			case REL_HUMIDITY:
				return MBusDataDescription.RelativeHumidity;
			case REMAINING_BATTERY_LIFE_TIME:
				return MBusDataDescription.RemainingBatteryLifeTime;
			case REMOTE_CONTROL:
				return MBusDataDescription.RemoteControl;
			case RESERVED:
				return MBusDataDescription.Reserved;
			case RESET_COUNTER:
				return MBusDataDescription.ResetCounter;
			case RESPONSE_DELAY_TIME:
				return MBusDataDescription.ResponseDelayTime;
			case RETRY:
				return MBusDataDescription.Retry;
			case RETURN_TEMPERATURE:
				return MBusDataDescription.ReturnTemperature;
			case RF_LEVEL:
				return MBusDataDescription.RFLevel;
			case SECURITY_KEY:
				return MBusDataDescription.SecurityKey;
			case SIZE_STORAGE_BLOCK:
				return MBusDataDescription.SizeStorageBlock;
			case SPECIAL_SUPPLIER_INFORMATION:
				return MBusDataDescription.SpecialSupplierInformation;
			case STORAGE_INTERVALL:
				return MBusDataDescription.StorageInterval;
			case TARIF_DURATION:
				return MBusDataDescription.TariffDuration;
			case TARIF_PERIOD:
				return MBusDataDescription.TariffPeriod;
			case TARIF_START:
				return MBusDataDescription.TariffStart;
			case TEMPERATURE_DIFFERENCE:
				return MBusDataDescription.TemperatureDifference;
			case TEMPERATURE_LIMIT:
				return MBusDataDescription.TemperatureLimit;
			case TIME_POINT:
				return MBusDataDescription.TimePoint;
			case TIME_POINT_DAY_CHANGE:
				return MBusDataDescription.TimePointDayChange;
			case USER_DEFINED:
				return MBusDataDescription.UserDefined;
			case VOLTAGE:
				return MBusDataDescription.Voltage;
			case VOLUME:
				return MBusDataDescription.Volume;
			case VOLUME_FLOW:
				return MBusDataDescription.VolumeFlow;
			case VOLUME_FLOW_EXT:
				return MBusDataDescription.VolumeFlowExt;
			case WEEK_NUMBER:
				return MBusDataDescription.WeekNumber;
			default:
				return null;
		}
	}
}
