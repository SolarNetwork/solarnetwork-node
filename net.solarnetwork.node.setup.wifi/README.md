# SolarNode WiFi Setup

This plugin provides a configurable UI within SolarNode for configuring WiFi connection settings.

![Settings](docs/solarnode-wifi-settings.png)

# Install

This plugin is must be manually installed, along with an associated OS support package, such as 
[`sn-wifi`][sn-wifi] for SolarNodeOS.

# Use

Once installed, a new **WiFi Connection** section will appear on the **Settings** page on your
SolarNode.

## Overall device settings

Each device configuration contains the following overall settings:

| Setting      | Description |
|:-------------|:------------|
| Country      | The 2-character country code of the WiFi network. |
| Network Name | The name of the WiFi network to connect to (also known as the SSID). |
| Password     | The WiFi password to use. |

# `SystemConfigure` instruction support

The `SystemConfigure` instruction topic can be used to both get the current WiFi device status
and to update the WiFi settings. The `service` parameter must be `/setup/network/wifi`. 

## Status instruction

To get the device status, pass no additional instruction parameters. The result parameter `result`
will be a status object with the following properties:

| Property | Type | Description |
|:---------|:-----|:------------|
| `active` | `boolean` | Will be `true` if the WiFi connection is active. |
| `addresses` | `List<String>` | A list of IP addresses associated with the WiFi device. |

Here's an example result, expressed in JSON:

```json
{
  "active": true,
  "addresses": [
    "192.168.1.134",
    "2406:e006:3093:b301:65b1:4726:2af:d721"
  ]
}
```

## Update instruction

To update any of the WiFi settings, the following instruction parameters can be included. If a given
parameter is not provided, it will remain unchanged from its current value.

| Parameter | Description |
|:----------|:------------|
| `country`  | The 2-character country code of the WiFi network. |
| `ssid`     | The name of the WiFi network to connect to. |
| `password` | The WiFi password to use. |

[sn-wifi]: https://github.com/SolarNetworkFoundation/solarnetwork-ops/tree/master/packages/wifi/debian
