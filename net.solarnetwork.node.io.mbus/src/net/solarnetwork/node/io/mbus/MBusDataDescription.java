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
	Energy,
	Volume,
	Mass,
	OnTime,
	OperatingTime,
	Power,
	VolumeFlow,
	VolumeFlowExt,
	MassFlow,
	FlowTemperature,
	ReturnTemperature,
	TemperatureDifference,
	ExternalTemperature,
	Pressure,
	Date,
	DateTime,
	Voltage,
	Current,
	AveragingDuration,
	ActualityDuration,
	FabricationNo,
	ModelVersion,
	ParameterSetId,
	HardwareVersion,
	FirmwareVersion,
	ErrorFlags,
	Customer,
	Reserved,
	OperatingTimeBattery,
	HCA,
	ReactiveEnergy,
	TemperatureLimit,
	MaxPower,
	ReactivePower,
	RelativeHumidity,
	Frequency,
	Phase,
	ExtendedIdentification,
	Address,
	NotSupported,
	ManufacturerSpecific,
	FutureValue,
	UserDefined,
	ApparentEnergy,
	CustomerLocation,
	AccessCodeOperator,
	AccessCodeUser,
	Password,
	AccessCodeSystemDeveloper,
	OtherSoftwareVersion,
	AccessCodeSytemOperator,
	ErrorMask,
	SecurityKey,
	DigitalInput,
	BaudRate,
	DigitalOutput,
	ResponseDelayTime,
	Retry,
	FirstStorageNumberCyclic,
	RemoteControl,
	LastStorageNumberCyclic,
	SizeStorageBlock,
	StorageInterval,
	TariffStart,
	DurationLastReadout,
	TimePoint,
	TariffDuration,
	OperatorSpecificData,
	TariffPeriod,
	NumberStops,
	LastCumulationDuration,
	SpecialSupplierInformation,
	ParameterActivationState,
	ControlSignal,
	WeekNumber,
	DayOfWeek,
	RemainingBatteryLifeTime,
	TimePointDayChange,
	CumulationCounter,
	RFLevel,
	ResetCounter;
}
