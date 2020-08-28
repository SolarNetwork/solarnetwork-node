# SolarNode GSS CozIR Series CO<sub>2</sub> Sensor Datum Source

This project provides a SolarNode plugin that can collect data from the Gas Sensing Solutions CovIR
series CO<sub>2</sub> sensors.

![settings](docs/solarnode-cozir-device-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **GSS CozIR CO2 Sensor Data Source**.

# Use

Once installed, a new **GSS CozIR Series CO2 Sensor** component will appear on the **Settings** page
on your SolarNode. Click on the **Manage** button to configure devices. You'll need to add one
configuration for each device you want to collect data from.

## Overall device settings

Each device configuration contains the following overall settings:

| Setting            | Description |
|:-------------------|:------------|
| Schedule           | A [cron schedule][cron] that determines when data is collected. |
| Service Name       | A unique name to identify this data source with. |
| Service Group      | A group name to associate this data source with. |
| Serial Port        | The service name of the [Serial Port][rxtx] component to use. |
| Sample Maximum Age | A minimum time to cache captured data, in milliseconds. |
| Source ID          | The SolarNetwork source ID to assign to captured datum. |

## Overall device settings notes

<dl>
	<dt>Serial Port</dt>
	<dd>This is the <i>service name</i> of the Serial Port component configured elsewhere
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

## Serial Port settings

The default serial port settings used by the CozIR device are shown below. The Serial Port component
referenced by the **Serial Port** setting must match the settings configured on the device.

| Setting      | Value |
|:-------------|:------|
| Baud         | 9600  |
| Data bits    | 8     |
| Parity       | None  |
| Stop bits    | 1     |
| Flow control | None  |

[cron]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Cron-Job-Syntax
[rxtx]: ../net.solarnetwork.node.io.serial.rxtx/
