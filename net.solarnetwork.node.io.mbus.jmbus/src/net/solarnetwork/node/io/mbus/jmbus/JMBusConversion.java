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

import org.openmuc.jmbus.Bcd;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DataRecord.Description;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.wireless.WMBusMessage;
import net.solarnetwork.node.io.mbus.MBusDataRecord;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.io.mbus.MBusMessage;
import net.solarnetwork.node.io.mbus.MBusSecondaryAddress;

/**
 * Helper functions for converting objects to and from JMBus.
 * 
 * @author alex
 * @version 1.0
 */
public class JMBusConversion {

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
	 * Convert a JMBus WMBus message
	 * 
	 * @param message
	 *        The JMBus WMBus message to convert
	 * @return A message
	 */
	public static MBusMessage from(WMBusMessage message) {
		final MBusMessage msg = new MBusMessage();
		for ( DataRecord record : message.getVariableDataResponse().getDataRecords() ) {
			final MBusDataRecord rec = from(record);
			if ( rec != null ) {
				msg.dataRecords.add(rec);
			}
		}
		return msg;
	}

	private static MBusDataRecord from(DataRecord record) {
		final MBusDataType type = from(record.getDescription());
		if ( type == null ) {
			return null;
		}
		switch (record.getDataValueType()) {
			case BCD:
				return new MBusDataRecord(type, ((Bcd) record.getDataValue()).doubleValue()
						* Math.pow(10, record.getMultiplierExponent()));
			case DATE:
				// TODO
				break;
			case DOUBLE:
				return new MBusDataRecord(type, (double) record.getDataValue());
			case LONG:
				// TODO
				break;
			case NONE:
				// TODO
				break;
			case STRING:
				// TODO
				break;
			default:
				break;
		}
		return null;
	}

	private static MBusDataType from(Description description) {
		switch (description) {
			case ACCSESS_CODE_OPERATOR:
				return MBusDataType.AccessCodeOperator;
			case ACCSESS_CODE_SYSTEM_DEVELOPER:
				return MBusDataType.AccessCodeSystemDeveloper;
			case ACCSESS_CODE_SYSTEM_OPERATOR:
				return MBusDataType.AccessCodeSytemOperator;
			case ACCSESS_CODE_USER:
				return MBusDataType.AccessCodeUser;
			case ACTUALITY_DURATION:
				return MBusDataType.ActualityDuration;
			case ADDRESS:
				return MBusDataType.Address;
			case APPARENT_ENERGY:
				return MBusDataType.ApparentEnergy;
			case AVERAGING_DURATION:
				return MBusDataType.AveragingDuration;
			case BAUDRATE:
				return MBusDataType.BaudRate;
			case CONTROL_SIGNAL:
				return MBusDataType.ControlSignal;
			case CUMULATION_COUNTER:
				return MBusDataType.CumulationCounter;
			case CURRENT:
				return MBusDataType.Current;
			case CUSTOMER:
				return MBusDataType.Customer;
			case CUSTOMER_LOCATION:
				return MBusDataType.CustomerLocation;
			case DATE:
				return MBusDataType.Date;
			case DATE_TIME:
				return MBusDataType.DateTime;
			case DAY_OF_WEEK:
				return MBusDataType.DayOfWeek;
			case DIGITAL_INPUT:
				return MBusDataType.DigitalInput;
			case DIGITAL_OUTPUT:
				return MBusDataType.DigitalOutput;
			case DURATION_LAST_READOUT:
				return MBusDataType.DurationLastReadout;
			case ENERGY:
				return MBusDataType.Energy;
			case ERROR_FLAGS:
				return MBusDataType.ErrorFlags;
			case ERROR_MASK:
				return MBusDataType.ErrorMask;
			case EXTENDED_IDENTIFICATION:
				return MBusDataType.ExtendedIdentification;
			case EXTERNAL_TEMPERATURE:
				return MBusDataType.ExternalTemperature;
			case FABRICATION_NO:
				return MBusDataType.FabricationNo;
			case FIRMWARE_VERSION:
				return MBusDataType.FirmwareVersion;
			case FIRST_STORAGE_NUMBER_CYCLIC:
				return MBusDataType.FirstStorageNumberCyclic;
			case FLOW_TEMPERATURE:
				return MBusDataType.FlowTemperature;
			case FREQUENCY:
				return MBusDataType.Frequency;
			case FUTURE_VALUE:
				return MBusDataType.Frequency;
			case HARDWARE_VERSION:
				return MBusDataType.HardwareVersion;
			case HCA:
				return MBusDataType.HCA;
			case LAST_CUMULATION_DURATION:
				return MBusDataType.LastCumulationDuration;
			case LAST_STORAGE_NUMBER_CYCLIC:
				return MBusDataType.LastStorageNumberCyclic;
			case MANUFACTURER_SPECIFIC:
				return MBusDataType.ManufacturerSpecific;
			case MASS:
				return MBusDataType.Mass;
			case MASS_FLOW:
				return MBusDataType.MassFlow;
			case MAX_POWER:
				return MBusDataType.MaxPower;
			case MODEL_VERSION:
				return MBusDataType.ModelVersion;
			case NOT_SUPPORTED:
				return MBusDataType.NotSupported;
			case NUMBER_STOPS:
				return MBusDataType.NumberStops;
			case ON_TIME:
				return MBusDataType.OnTime;
			case OPERATING_TIME:
				return MBusDataType.OperatingTime;
			case OPERATING_TIME_BATTERY:
				return MBusDataType.OperatingTimeBattery;
			case OPERATOR_SPECIFIC_DATA:
				return MBusDataType.OperatorSpecificData;
			case OTHER_SOFTWARE_VERSION:
				return MBusDataType.OtherSoftwareVersion;
			case PARAMETER_ACTIVATION_STATE:
				return MBusDataType.ParameterActivationState;
			case PARAMETER_SET_ID:
				return MBusDataType.ParameterSetId;
			case PASSWORD:
				return MBusDataType.Password;
			case PHASE:
				return MBusDataType.Phase;
			case POWER:
				return MBusDataType.Power;
			case PRESSURE:
				return MBusDataType.Pressure;
			case REACTIVE_ENERGY:
				return MBusDataType.ReactiveEnergy;
			case REACTIVE_POWER:
				return MBusDataType.ReactivePower;
			case REL_HUMIDITY:
				return MBusDataType.RelativeHumidity;
			case REMAINING_BATTERY_LIFE_TIME:
				return MBusDataType.RemainingBatteryLifeTime;
			case REMOTE_CONTROL:
				return MBusDataType.RemoteControl;
			case RESERVED:
				return MBusDataType.Reserved;
			case RESET_COUNTER:
				return MBusDataType.ResetCounter;
			case RESPONSE_DELAY_TIME:
				return MBusDataType.ResponseDelayTime;
			case RETRY:
				return MBusDataType.Retry;
			case RETURN_TEMPERATURE:
				return MBusDataType.ReturnTemperature;
			case RF_LEVEL:
				return MBusDataType.RFLevel;
			case SECURITY_KEY:
				return MBusDataType.SecurityKey;
			case SIZE_STORAGE_BLOCK:
				return MBusDataType.SizeStorageBlock;
			case SPECIAL_SUPPLIER_INFORMATION:
				return MBusDataType.SpecialSupplierInformation;
			case STORAGE_INTERVALL:
				return MBusDataType.StorageInterval;
			case TARIF_DURATION:
				return MBusDataType.TariffDuration;
			case TARIF_PERIOD:
				return MBusDataType.TariffPeriod;
			case TARIF_START:
				return MBusDataType.TariffStart;
			case TEMPERATURE_DIFFERENCE:
				return MBusDataType.TemperatureDifference;
			case TEMPERATURE_LIMIT:
				return MBusDataType.TemperatureLimit;
			case TIME_POINT:
				return MBusDataType.TimePoint;
			case TIME_POINT_DAY_CHANGE:
				return MBusDataType.TimePointDayChange;
			case USER_DEFINED:
				return MBusDataType.UserDefined;
			case VOLTAGE:
				return MBusDataType.Voltage;
			case VOLUME:
				return MBusDataType.Volume;
			case VOLUME_FLOW:
				return MBusDataType.VolumeFlow;
			case VOLUME_FLOW_EXT:
				return MBusDataType.VolumeFlowExt;
			case WEEK_NUMBER:
				return MBusDataType.WeekNumber;
			default:
				return null;
		}
	}
}
