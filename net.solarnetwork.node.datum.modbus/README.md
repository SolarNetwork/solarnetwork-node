# SolarNode Modbus Device Datum Source

This project provides SolarNode plugin that can collect arbitrary data from
Modbus enabled devices. This is an advanced plugin that requires specific
low-level knowledge of the device you want to collect data from.

![settings](docs/solarnode-modbus-device-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It
appears under the **Datum** category as **Generic Modbus Datum Source**.

# Use

Once installed, a new **Modbus Device** component will appear on the
**Settings** page on your SolarNode. Click on the **Manage** button to configure
devices. You'll need to add one configuration for each Modbus device you want to
collect data from.

## Overall device settings

Each device configuration contains the following overall settings:

| Setting            | Description                                                                      |
|--------------------|----------------------------------------------------------------------------------|
| Schedule           | A cron schedule that determines when data is collected.                          |
| Service Name       | A unique name to identify this data source with.                                 |
| Service Group      | A group name to associate this data source with.                                 |
| Modbus Port        | The service name of the Modbus port to use.                                      |
| Modbus Unit ID     | The ID of the Modbus device to collect data from, from 1 - 255.                  |
| Source ID          | The SolarNetwork unique source ID to assign to datum collected from this device. |
| Sample Maximum Age | A minimum time to cache captured Modbus data, in milliseconds.                   |
| Max Read Length    | The maximum number of Modbus registers to request at once.                       |
| Word Order         | For multi-register data types, the ordering to use when combining them.          |

## Overall device settings notes

<dl>
	<dt>Modbus Port</dt>
	<dd>This is the <i>service name</i> of the Modbus component configured elsewhere
	in SolarNode. You must configure that component with the proper connection settings
	for your Modbus network, configure a unique service name on that component, and then
	enter that same service name here.</dd>
	<dt>Source ID</dt>
	<dd>This value unique identifies the data collected from this device, by this node,
	 on SolarNetwork. Each configured device should use a different value.</dd>
	<dt>Sample Maximum Age</dt>
	<dd>SolarNode will cache the data collected from the Modbus device for at least
	this amount of time before refreshing data from the device again. Some devices
	do not refresh their values more than a fixed interval, so this setting can be
	used to avoid reading data unnecessarily. This setting also helps in highly
	dynamic configurations where other plugins request the current values from
	the device frequently.</dd>
	<dt>Max Read Length</dt>
	<dd>This plugin will try to read as many adjacent Modbus registers as possible
	when requesting data from the device. Some devices have trouble returning large
	numbers of registers at once, however. Configuring this setting to a smaller
	value will cause the plugin to make multiple smaller requests for data when
	necessary to work better with those devices.</dd>
</dl>

## Datum property settings

You must configure settings for each datum property you want to collect from each device.
You can configure as many property settings as you like, using the <kbd>+</kbd> and <kbd>-</kbd>
buttons to add/remove property configurations.

Each property configuration contains the following settings:

| Setting         | Description                                                                                             |
|-----------------|---------------------------------------------------------------------------------------------------------|
| Property        | The name of the datum property to save the Modbus value as.                                             |
| Property Type   | The type of datum property to use.                                                                      |
| Modbus Address  | The starting register address to read Modbus data from (zero-based).                                    |
| Modbus Function | The Modbus read function to execute.                                                                    |
| Data Type       | The type of data to expect from the read Modbus register(s).                                            |
| Data Length     | For variable length data types such as strings, the number of Modbus registers to read.                 |
| Unit Multiplier | For numeric data types, a multiplier to apply to the Modbus value to normalize it into a standard unit. |
| Decimal Scale   | For numeric data types, a maximum number of decimal places to round decimal numbers to.                 |

## Datum property settings notes

<dl>
	<dt>Property</dt>
	<dd>Property names represent what the associated data value is, and SolarNetwork
	has many standardized names that you should consider using. For example the
	<a href="https://github.com/SolarNetwork/solarnetwork-node/blob/master/net.solarnetwork.node/src/net/solarnetwork/node/domain/EnergyDatum.java">EnergyDatum</a>
	class defines properties such as <code>watts</code> and <code>wattHours</code>
	for electrical energy.</dd>
	<dt>Property Type</dt>
	<dd>Each property must be categorized as <code>Accumulating</code>, <code>Instantaneous</code>,
	or <code>Status</code>. <b>Accumulating</b> is used for properties that record
	a value that accumulates over time, such as <code>wattHours</code> captured from
	a power meter. <b>Instantaneous</b> is used for properties that capture values
	that record independent values over time, such as <code>watts</code>. <b>Status</b>
	is used for non-numeric values such as string status messages.</dd>
	<dt>Data Type</dt>
	<dd>The data type to interpret the values captured from the Modbus registers as.
	<b>Note</b> that only the <b>Status</b> property type can accept non-numeric
	data types such as strings.</dd>
	<dt>Unit Multiplier</dt>
	<dd>The property values stored in SolarNetwork should be normalized into standard
	base units if possible. For example if a power meter reports power in <i>kilowattts</i>
	then a unit multiplier of <code>1000</code> can be used to convert the values into
	<i>watts</i>.</dd>
	<dt>Decimal Scale</dt>
	<dd>This setting will round decimal numbers to at most this number of decimal places. Setting
	to <code>0</code> rounds decimals to whole numbers. Setting to <code>-1</code> disables
	rounding completely.</dd>
</dl>

## Virtual meter settings

Since version **1.3** you can ask SolarNode to generate accumulating property values derived
from instantaneous properties extrapolated across time. For example, if you have an
irradiance sensor that allows you to capture instantaneous <code>W / m<sup>2</sup></code>
power values, you could configure a virtual meter to generate <code>Wh /
m<sup>2</sup></code> energy values. You can configure as many virtual meters as you like,
using the <kbd>+</kbd> and <kbd>-</kbd> buttons to add/remove meter configurations.

![virtual-meter](docs/solarnode-virtual-meter-settings.png)

Each virtual meter works with a single instantaneous datum property. The derived
accumulating datum property will be named after that property with the time unit suffix
appended. For example, an instantaneous `irradiance` property using the `Hours` time unit
would result in an accumulating `irradianceHours` property. The value is calculated as
an **average** between the current instantaneous value and the previously captured
instantaneous value, multiplied by the amount of time that has elapsed between the two
samples.

Virtual meters require keeping track of the meter reading value over time along with the
previously captured value. This plugin uses the [SolarNetwork datum metadata
API][meta-api] for this, storing three metadata properties under a property key named for
the virtual meter property name. For example, continuing the `irradianceHours` example, an
example set of datum metadata would look like:

```json
{
  "pm": {
    "irradianceHours": {
      "vm-date": 123123123123,
      "vm-value": "1361",
      "vm-reading": "12390980.1231"
    }
  }
}
```

Each virtual meter configuration contains the following settings:

| Setting         | Description                                                                           |
|-----------------|---------------------------------------------------------------------------------------|
| Property        | The name of the instantaneous datum property to derive the virtual meter values from. |
| Time Unit       | The time unit to record meter readings as.                                            |
| Max Age         | The maximum time allowed between samples where the meter reading can advance.         |
| Meter Reading   | The current meter reading value.                                                      |

## Virtual meter settings notes

<dl>
	<dt>Time Unit</dt>
	<dd>This value affects the name of the virtual meter reading property: it will be appended to the
	end of the property name. It also affects the virtual meter reading values, as they will be calculated in
	this time unit.</dd>
	<dt>Max Age</dt>
	<dd>In case the node isn't collecting samples for a period of time, this setting prevents the plugin
	from calculating an unexpectedly large reading value jump. For example if a node was turned off for
	a day, the first sample it captures when turned back on would otherwise advance the reading as if the
	associated instantaneous property had been active over that entire time. With this restriction, the
	node will record the new sample date and value, but not advance the meter reading until another sample
	is captured within this time period.</dd>
	<dt>Meter Reading</dt>
	<dd>Generally this should <b>not</b> be changed, because it can impact how the values are aggregated and
	interpreted by SolarNetwork and applications using the data.</dd>
</dl>


## Expressions

Since version **1.5** properties can be defined using [expressions][expr]. Expressions allow you to
configure datum properties that are dynamically calculated from other properties or raw Modbus
register values.

![expressions-config](docs/solarnode-modbus-device-expression-settings.png)

### Expression root object

The root object is a [ExpressionRoot][ExpressionRoot] object, which has the following properties:

| Property | Type | Description |
|:---------|:-----|:------------|
| `datum` | `GeneralNodeDatum` | A [`GeneralNodeDatum`][GeneralNodeDatum] object, populated with data from all property and virtual meter configurations. |
| `props` | `Map<String,Object>` | Simple Map based access to the data in `datum`, to simplify expressions. |
| `sample` | `ModbusData` | A [`ModbusData`][ModbusData] object, populated with the raw Modbus data read from the device. |
| `regs` | `Map<Integer,Integer>` | Simple Map based access to the register data in `sample`, to simplify expressions all register values are returned as unsigned 16-bit integers. |

Any Modbus registers referenced via `reg[x]` expressions will be automatically read via the Modbus
**read holding register** function, unless that same register is referenced also in a normal
property configuration, in which case the Modus function defined there will be used.

### Expression examples

Given raw Modbus data like the following:

```
[000]: 0xfc1e, 0xf0c3, 0x02e3, 0x68e7, 0x0002, 0x1376, 0x1512, 0xdfee
[200]: 0x44f6, 0xc651, 0x4172, 0xd3d1, 0x6328, 0x8ce7
```

and assuming a property config that maps register **202** to a 64-bit floating point property `bigFloat`:

Then here are some example expressions and the results they would produce:

| Expression | Result | Comment |
|:-----------|:-------|:--------|
| `regs[0]` | `64542` | Returns register **0** directly, which is `0xfc1e`. |
| `sample.getInt32(regs[2])` | `48457959` | Returns registers **2** and **3** combined as a unsigned 32-bit integer `0x02e368e7`. |
| `sample.getFloat32(regs[200])` | `1974.1974` | Returns registers **200** and **201** as a IEEE-754 32-bit floating point: `0x44f6c651`. |
| `props['bigFloat'] - regs[0]` | `19677432.1974` | Returns difference of register **0** (`0xfc1e`) from datum property `bigFloat` (`0x4172d3d163288ce7`). |


[expr]: https://github.com/SolarNetwork/solarnetwork/wiki/Expression-Languages
[ExpressionRoot]: https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.datum.modbus/src/net/solarnetwork/node/datum/modbus/ExpressionRoot.java
[GeneralNodeDatum]: https://github.com/SolarNetwork/solarnetwork-node/blob/develop/net.solarnetwork.node/src/net/solarnetwork/node/domain/GeneralNodeDatum.java
[ModbusData]: https://github.com/SolarNetwork/solarnetwork-node/blob/develop/net.solarnetwork.node.io.modbus/src/net/solarnetwork/node/io/modbus/ModbusData.java
[meta-api]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarQuery-API#add-node-datum-metadata
