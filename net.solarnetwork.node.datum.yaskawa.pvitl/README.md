# SolarNode PVI-14-36TL Series Inverter Datum Source

This project provides SolarNode plugin that can collect data from the Yaskawa
Solectria PVI-14-36TL series inverters.

![settings](docs/solarnode-pvitl-device-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It
appears under the **Datum** category as **Solectria PVI-14-36TL Inverter Data Source**.

# Datum structure

This datum generates a [datum stream][datum] with the following properties:

| Property | Class | Units | Description |
|:---------|:------|:------|:------------|
| `apparentPower`    | `i` | VA | Apparent power |
| `dcCurrent`        | `i` | A | DC current, sum of PV1, PV2 |
| `dcCurrent1`       | `i` | A | DC current, PV1 |
| `dcCurrent2`       | `i` | A | DC current, PV2 |
| `dcPower`          | `i` | W | DC output power, sum of PV1, PV2 |
| `dcPower1`         | `i` | W | DC module 1 output power |
| `dcPower2`         | `i` | W | DC module 2 output power |
| `dcVoltage`        | `i` | V | DC voltage, average of PV1, PV2 |
| `dcVoltage1`       | `i` | V | DC module 1 voltage |
| `dcVoltage2`       | `i` | V | DC module 2 voltage |
| `current`          | `i` | A | AC current total, sum of phase A, B, C |
| `events`           | `s` | bit set | Base-10 bit set of [SunSpec-compatible event codes](#sunspec-event-codes) |
| `fault0`           | `s` | bit set | Base-10 bit set of [Fault codes](#fault-codes) (16-31, offset -16) |
| `fault1`           | `s` | bit set | Base-10 bit set of [Fault codes](#fault-codes) (32-47, offset -32) |
| `fault2`           | `s` | bit set | Base-10 bit set of [Fault codes](#fault-codes) (48-63, offset -48) |
| `fault3`           | `s` | bit set | Base-10 bit set of [Fault codes](#fault-codes) (64-79, offset -64) |
| `fault4`           | `s` | bit set | Base-10 bit set of [Fault codes](#fault-codes) (80-95, offset -80) |
| `frequency`        | `i` | Hz | AC frequency |
| `opState`          | `s` | enum | [Device Operating State][opstate] code |
| `opStates`         | `s` | enum | [Inverter state](#inverter-state) code |
| `permFault`        | `s` | bit set | Base-10 bit set of [Fault codes](#fault-codes) (0-15) |
| `powerFactor`      | `i` | PF | Decimal 0 - 1 |
| `temp`             | `i` | C | Internal temperature |
| `temp_heatSink`    | `i` | C | Heatsink temperature |
| `voltage`          | `i` | V | AC line voltage average, phase AB, BC, CA |
| `warn`             | `s` | bit set | Base-10 bit set of [Warning codes](#warning-codes) |
| `wattHours`        | `a` | Wh | Lifetime energy |
| `watts`            | `i` | W | Active power total |


```json
{
	"created": "2023-03-20 16:15:38.573Z",
	"nodeId": 123,
	"sourceId": "/INV/1",
	"localDate": "2023-03-20",
	"localTime": "12:15",
	"watts": 15900,
	"current": 55.4,
	"dcPower": 16212,
	"voltage": 494.33334,
	"dcPower1": 8103,
	"dcPower2": 8109,
	"dcVoltage": 387.85,
	"frequency": 60,
	"dcVoltage1": 387.7,
	"dcVoltage2": 388,
	"wattHours": 91361000
}
```

# Use

Once installed, a new **Solectria PVI-14-36TL Series Inverter** component will
appear on the **Settings** page on your SolarNode. Click on the **Manage**
button to configure devices. You'll need to add one configuration for each
Modbus device you want to collect data from.

## Overall device settings

Each device configuration contains the following overall settings:

| Setting            | Description                                                                      |
|--------------------|----------------------------------------------------------------------------------|
| Schedule           | A cron schedule that determines when data is collected.                          |
| Service Name       | A unique name to identify this data source with.                                 |
| Service Group      | A group name to associate this data source with.                                 |
| Modbus Port        | The service name of the Modbus Port to use.                                      |
| Sample Maximum Age | A minimum time to cache captured data, in milliseconds.                          |
| Unit ID            | The address of the inverter to collect datum from.                               |
| Source ID          | The SolarNetwork source ID to assign to captured datum.                          |

## Overall device settings notes

<dl>
	<dt>Modbus Port</dt>
	<dd>This is the <i>service name</i> of the Modbus Port component configured elsewhere
	in SolarNode. You must configure that component with the proper connection settings
	for your serial network, configure a unique service name on that component, and then
	enter that same service name here.</dd>
	<dt>Source ID</dt>
	<dd>This value unique identifies the data collected from this device, by this node,
	 on SolarNetwork. Each configured device should use a different value.</dd>
	<dt>Sample Maximum Age</dt>
	<dd>SolarNode will cache the data collected from the device for at least
	this amount of time before refreshing data from the device again. Some devices
	do not refresh their values more than a fixed interval, so this setting can be
	used to avoid reading data unnecessarily. This setting also helps in highly
	dynamic configurations where other plugins request the current values from
	the device frequently.</dd>
</dl>

# Inverter state

The `opStates` property can have the following possible values:

| Index | Name      | Description |
|:------|:----------|:------------|
| 2048  | `Derate`  | Derate      |
| 4096  | `Running` | Running     |
| 8192  | `Standby` | Standby     |
| 16384 | `Check`   | Check       |
| 32768 | `Fault`   | Fault       |

# Warning codes

The `warn` property is a bit set of the following possible values:

| Index | Name                | Description                   |
|:------|:--------------------|:------------------------------|
| 0     | `ExternalFan`       | External fan error            |
| 1     | `InternalFan`       | Internal fan error            |
| 2     | `InternalComms`     | Internal communication failed |
| 3     | `DspEeprom`         | DSP EEPROM fault              |
| 6     | `TemperatureSensor` | Temperature sensor fault      |
| 9     | `LcdEeprom`         | LCD EEPROM fault              |

# Fault codes

The `vendorEvents` property is a bit set of the following possible values:

| Index | Name                                | Description                            |
|:------|:------------------------------------|:---------------------------------------|
| 0     | `PermanentBusOverVoltage`           | Permanent bus (sum) over voltage       |
| 1     | `PermanentBusUnderVoltage`          | Permanent bus (sum) low voltage        |
| 2     | `PermanentBusImbalance`             | Permanent bus imbalance                |
| 3     | `PermanentGridRelay`                | Permanent grid relay                   |
| 4     | `StaticGfci`                        | Static GFCI                            |
| 6     | `Dci`                               | DCI                                    |
| 8     | `HardwareOverCurrent`               | Hardware over current                  |
| 12    | `PowerModule`                       | Power module                           |
| 13    | `InternalHardware`                  | Internal hardware                      |
| 14    | `PermanentInverterOpenLoopSelfTest` | Permanent inverter open-loop self-test |
| 15    | `PermanentControlBoard15VLow`       | 15V control board low                  |
| 16    | `BusOverVoltage`                    | Bus (sum) over voltage                 |
| 17    | `BusUnderVoltage`                   | Bus (sum) low voltage                  |
| 18    | `BusImbalance`                      | Bus imbalance                          |
| 19    | `BusSoftStartTimeout`               | Bus soft start timeout                 |
| 20    | `InverterSoftStartTimeout`          | Inverter soft start timeout            |
| 22    | `Pv1OverCurrent`                    | PV1 over current                       |
| 23    | `GridLineVoltage`                   | Grid line voltage out of range         |
| 24    | `GridPhaseVoltage`                  | Grid phase voltage out of range        |
| 25    | `InverterOverCurrent`               | Inverter over current                  |
| 26    | `GridOverFrequency`                 | Grid over frequency                    |
| 27    | `GridUnderFrequency`                | Grid under frequency                   |
| 28    | `LossOfMain`                        | Loss of main                           |
| 29    | `GridRelay`                         | Grid relay                             |
| 30    | `OverTemperature`                   | Over temperature protection            |
| 31    | `OutputCurrentSamplingOffset`       | Sampling offset of output current      |
| 32    | `InverterVoltageOffset`             | Inverter voltage offset                |
| 33    | `DciOffset`                         | DCI offset                             |
| 34    | `DciHigh`                           | DCI high                               |
| 35    | `InsulationResistanceLow`           | Insulation resistance low              |
| 36    | `DynamicLeakageCurrentHigh`         | Dynamic leakage current high           |
| 37    | `FrequencyDetection`                | Frequency detection                    |
| 39    | `McuProtection`                     | MCU protection                         |
| 40    | `InverterHardwareOverCurrent`       | Inverter hardware over current         |
| 41    | `GridVoltageImbalance`              | Grid voltage imbalance                 |
| 43    | `InverterCurrentImbalance`          | Inverter current imbalance             |
| 44    | `PowerModuleProtection`             | Power module protection                |
| 47    | `LeakageCurrentSensor`              | Leakage current sensor                 |
| 49    | `InternalHardware`                  | Internal hardware error                |
| 50    | `IoPowerMismatch`                   | Input/output power mismatch            |
| 51    | `Pv2InputReverseConnection`         | PV2 input reverse connection           |
| 52    | `Pv2OverCurrent`                    | PV2 over current                       |
| 53    | `Pv2OverVoltage`                    | PV2 over voltage                       |
| 54    | `PvAbnormalInput`                   | PV abnormal input                      |
| 55    | `InverterOpenLoopSelfTest`          | Inverter open-loop self-test error     |
| 57    | `Pv1InputReverseConnection`         | PV1 input reverse connection           |
| 58    | `Pv1OverVoltage`                    | PV1 over voltage                       |
| 61    | `ArcboardAbnormal`                  | Arcboad abnormal                       |
| 62    | `StaticGfiProtect`                  | Static GFI protect                     |
| 63    | `ArcProtection`                     | Arc protection                         |
| 79    | `ArcProtection3`                    | Arc protection                         |
| 80    | `ControlBoard15VLow`                | 15V control board low                  |
| 81    | `StaticGfciHigh`                    | Static GFCI high                       |
| 82    | `ArcBoard`                          | Arc board failure                      |
| 83    | `PvModuleConfiguration`             | PV module configuration error          |

# SunSpec event codes

This plugin will map specific fault codes into the SunSpec `events` property:

| Fault | Name | Event | Inverter Model Event |
|:------|:-----|:------|:---------------------|
| 0     | `PermanentBusOverVoltage`           | 1  | `DcOverVoltage` |
| 3     | `PermanentGridRelay`                | 4  | `GridDisconnect` |
| 16    | `BusOverVoltage`                    | 1  | `DcOverVoltage` |
| 23    | `GridLineVoltage`                   | 10 | `AcOverVoltage` |
| 24    | `GridPhaseVoltage`                  | 10 | `AcOverVoltage` |
| 26    | `GridOverFrequency`                 | 8  | `OverFrequency` |
| 27    | `GridUnderFrequency`                | 9  | `UnderFrequency` |
| 28    | `LossOfMain`                        | 4  | `GridDisconnect` |
| 30    | `OverTemperature`                   | 7  | `OverTemperature` |
| 53    | `Pv2OverVoltage`                    | 1  | `DcOverVoltage` |
| 58    | `Pv1OverVoltage`                    | 1  | `DcOverVoltage` |
| 14    | `PermanentInverterOpenLoopSelfTest` | 15 | `HwTestFailure` |
| 55    | `InverterOpenLoopSelfTest`          | 15 | `HwTestFailure` |
| 82    | `ArcBoard`                          | 15 | `HwTestFailure` |

[datum]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-global-objects#datum
[opstate]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-global-objects#standard-device-operating-states
