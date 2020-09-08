# SolarNode M-Bus Device Datum Source

This project provides SolarNode plugin that can collect arbitrary data from M-Bus (wired and
wireless) enabled devices. This is an advanced plugin that requires specific low-level knowledge of
the device you want to collect data from.

## Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **Generic M-Bus Datum Source**. Note that some M-Bus connection plugin
must also be installed, like the [jMBus](../net.solarnetwork.node.io.mbus.jmbus/) one.

## Use

Once installed, two new components will appear on the **Settings** page on your SolarNode: **M-Bus
Device** and **M-Bus (Wireless) Device**. Click on the **Manage** button to configure devices.
You'll need to add one configuration for each M-Bus device you want to collect data from.

# M-Bus Device Datum Source

This component lets you collect from M-Bus devices physically connected to SolarNode via a serial 
network.

![M-Bus device settings](docs/solarnode-mbus-device-settings.png)

## Overall device settings

Each device configuration contains the following overall settings:

| Setting            | Description                                                                      |
|--------------------|----------------------------------------------------------------------------------|
| Schedule           | A cron schedule that determines when data is collected.                          |
| Service Name       | A unique name to identify this data source with.                                 |
| Service Group      | A group name to associate this data source with.                                 |
| M-Bus Connection   | The service name of the M-Bus Connection to use.                                 |
| Primary Address    | The ID of the M-Bus device to collect data from, from 1 - 255.                   |
| Source ID          | The SolarNetwork unique source ID to assign to datum collected from this device. |

## Datum property settings

You must configure settings for each datum property you want to collect from each device. You can
configure as many property settings as you like, using the <kbd>+</kbd> and <kbd>-</kbd> buttons to
add/remove property configurations.

Each property configuration contains the following settings:

| Setting         | Description                                                |
|-----------------|------------------------------------------------------------|
| Property        | The name of the datum property to save the M-Bus value as. |
| Property Type   | The type of datum property to use. |
| Data Descriptor | The M-Bus data descriptor. |
| Data Type       | The M-Bus data type to read. |
| Unit Multiplier | For numeric data types, a multiplier to apply to the Modbus value to normalize it into a standard unit. |
| Decimal Scale   | For numeric data types, a maximum number of decimal places to round decimal numbers to. |


# M-Bus (Wireless) Device Datum Source

This component lets you collect from a wireless M-Bus transceiver device physically connected to
SolarNode via a serial network; the transceiver communicates with M-Bus devices wirelessly.

![Wireless M-Bus device settings](docs/solarnode-mbus-wireless-device-settings.png)

TODO
