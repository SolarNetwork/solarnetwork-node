/* ==================================================================
 * MBusDataDescription.java - 29/06/2020 09:39:13 AM
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

package net.solarnetwork.node.io.mbus;

/**
 * An enumeration of M-Bus data descriptors.
 * 
 * @author alex
 * @version 1.0
 */
public enum MBusDataDescription {
	/** Energy data. */
	Energy,

	/** Volume data. */
	Volume,

	/** Mass data. */
	Mass,

	/** On time. */
	OnTime,

	/** Operating time. */
	OperatingTime,

	/** Power. */
	Power,

	/** Volume flow. */
	VolumeFlow,

	/** Volume flow external. */
	VolumeFlowExt,

	/** Mass flow. */
	MassFlow,

	/** Flow temperature. */
	FlowTemperature,

	/** Return temperature. */
	ReturnTemperature,

	/** Temperature difference. */
	TemperatureDifference,

	/** External temperature. */
	ExternalTemperature,

	/** Pressure. */
	Pressure,

	/** Date. */
	Date,

	/** Date and time. */
	DateTime,

	/** Voltage. */
	Voltage,

	/** Current. */
	Current,

	/** Averaging duration. */
	AveragingDuration,

	/** Actuality duration. */
	ActualityDuration,

	/** Fabrication number. */
	FabricationNo,

	/** Model version. */
	ModelVersion,

	/** Parameter set ID. */
	ParameterSetId,

	/** Hardware version. */
	HardwareVersion,

	/** Firmware version. */
	FirmwareVersion,

	/** Error flags. */
	ErrorFlags,

	/** Customer. */
	Customer,

	/** Reserved. */
	Reserved,

	/** Operating time battery. */
	OperatingTimeBattery,

	/** HCA. */
	HCA,

	/** Reactive energy. */
	ReactiveEnergy,

	/** Temperature limit. */
	TemperatureLimit,

	/** Max power. */
	MaxPower,

	/** Reactive power. */
	ReactivePower,

	/** Relative humidity. */
	RelativeHumidity,

	/** Frequency. */
	Frequency,

	/** Phase. */
	Phase,

	/** Extended identification. */
	ExtendedIdentification,

	/** Address. */
	Address,

	/** Not supported. */
	NotSupported,

	/** Manufacturer specific. */
	ManufacturerSpecific,

	/** Future value. */
	FutureValue,

	/** User defined. */
	UserDefined,

	/** Apparent energy. */
	ApparentEnergy,

	/** Customer location. */
	CustomerLocation,

	/** Access code operator. */
	AccessCodeOperator,

	/** Access code user. */
	AccessCodeUser,

	/** Password. */
	Password,

	/** Access code system developer. */
	AccessCodeSystemDeveloper,

	/** Other software version. */
	OtherSoftwareVersion,

	/** Access code system operator. */
	AccessCodeSytemOperator,

	/** Error mask. */
	ErrorMask,

	/** Security key. */
	SecurityKey,

	/** Digital input. */
	DigitalInput,

	/** Baud rate. */
	BaudRate,

	/** Digital output. */
	DigitalOutput,

	/** Response delay time. */
	ResponseDelayTime,

	/** Retry. */
	Retry,

	/** First storage number cyclic. */
	FirstStorageNumberCyclic,

	/**
	 * Remote control. *. RemoteControl,
	 * 
	 * /** Last storage number cyclic.
	 */
	LastStorageNumberCyclic,

	/**
	 * Size storage block. *? SizeStorageBlock,
	 * 
	 * /** Storage interval.
	 */
	StorageInterval,

	/** Tariff start. */
	TariffStart,

	/** Duration last readout. */
	DurationLastReadout,

	/** Time point. */
	TimePoint,

	/** Tariff duration. */
	TariffDuration,

	/** Operator specific data. */
	OperatorSpecificData,

	/** Tariff period. */
	TariffPeriod,

	/** Number stops. */
	NumberStops,

	/** Last cumulation duration. */
	LastCumulationDuration,

	/** Special supplier information. */
	SpecialSupplierInformation,

	/** Parameter activation state. */
	ParameterActivationState,

	/** Control signal. */
	ControlSignal,

	/** Week number. */
	WeekNumber,

	/** Day of week. */
	DayOfWeek,

	/** Remaining battery life time. */
	RemainingBatteryLifeTime,

	/** Time point day change. */
	TimePointDayChange,

	/** Cumulation counter. */
	CumulationCounter,

	/** RF level. */
	RFLevel,

	/** Reset counter. */
	ResetCounter;
}
