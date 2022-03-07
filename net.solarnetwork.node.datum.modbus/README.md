# SolarNode Modbus Device Datum Source

This project provides SolarNode plugin that can collect arbitrary data from
Modbus enabled devices. This is an advanced plugin that requires specific
low-level knowledge of the device you want to collect data from.

![settings](docs/solarnode-modbus-device-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **Modbus Datum Source**.

# Use

Once installed, a new **Modbus Device** component will appear on the **Settings** page on your
SolarNode. Click on the **Manage** button to configure devices. You'll need to add one configuration
for each Modbus device you want to collect data from.

## Overall device settings

Each device configuration contains the following overall settings:

| Setting            | Description |
|:-------------------|:------------|
| Schedule           | A cron schedule that determines when data is collected. |
| Service Name       | A unique name to identify this data source with. |
| Service Group      | A group name to associate this data source with. |
| Modbus Connection  | The **service name** of the Modbus connection to use. |
| Modbus Unit ID     | The ID of the Modbus device to collect data from, from 1 - 255. |
| Source ID          | The SolarNetwork unique source ID to assign to datum collected from this device. |
| Sample Maximum Age | A minimum time to cache captured Modbus data, in milliseconds. |
| Max Read Length    | The maximum number of Modbus registers to request at once. |
| Word Order         | For multi-register data types, the ordering to use when combining them. |
| Datum Filter Service | The **service name** of a datum filter to apply. |
| Sub-sample Frequency | If configured, the frequency at which samples should be collected from the device, in milliseconds. Set to `0` (or empty) to disable. Typically this would be combined with a **Datum Filter Service** to transform the sub-samples.

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

## Metadata settings

Since version **3.1**, metadata settings allow you to add datum source metadata to the configured
datum stream.

![Metadata settings](docs/solarnode-modbus-device-metadata-settings.png)

Each metadata configuration contains the following settings:

| Setting         | Description |
|:----------------|:------------|
| Name            | The metadata key. If starts with a `/` then will be treated as a [key-path][metadata-key-path]. Valid paths start with one of `/m/`, `/pm/`, and `/t`. |
| Value           | The metadata value. |


## Datum property settings

You must configure settings for each datum property you want to collect from each device. You can
configure as many property settings as you like, using the <kbd>+</kbd> and <kbd>-</kbd> buttons to
add/remove property configurations.

![Property settings](docs/solarnode-modbus-device-property-settings.png)

Each property configuration contains the following settings:

| Setting         | Description                                                                                             |
|:----------------|:--------------------------------------------------------------------------------------------------------|
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

## Expressions

Since version **1.5** properties can be defined using [expressions][expr]. Expressions allow you to
configure datum properties that are dynamically calculated from other properties or raw Modbus
register values.

![expressions-config](docs/solarnode-modbus-device-expression-settings.png)

### Expression root object

The root object is a `DatumExpressionRoot` that lets you treat all datum properties as expression
variables directly. See the [SolarNode Expressions][sn-expressions] guide for more information. In 
addition, the following properties are available:

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
[metadata-key-path]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-global-objects#metadata-filter-key-paths
[sn-expressions]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Expressions
