# SolarFlux Upload Service

This project provides SolarNode plugin that posts datum captured by other SolarNode plugins to a
SolarFlux-compatible MQTT server.

![settings](docs/solarnode-solarflux-upload-settings.png)

# Install

The plugin is meant for developers and can be manually installed.

# Use

Once installed, a new **SolarFlux Upload Service** component will appear on the **Settings** page on
your SolarNode. Click on the **Manage** button to configure services. You'll need to add one
configuration for each SolarFlux server you want to upload data to.

## Overall device settings

Each device configuration contains the following overall settings:

| Setting | Description |
|---------|-------------|
| Host | The URI for the SolarFlux server to connect to. |
| Username | The MQTT username to use. |
| Password | The MQTT password to use. |

For TLS-encrypted connections, SolarNode will make the node's own X.509 certificate available
for client authentication.

## Overall device settings notes

<dl>
	<dt>Host</dt>
	<dd>The URL to the MQTT server to use. Use <code>mqtts</code> for a TLS encrypted connection,
	or <code>mqtt</code> for no encryption. For example: <code>mqtts://influx.solarnetwork.net:8884</code>.</dd>
	<dt>Password</dt>
	<dd>Note that SolarNode will provide its X.509 certificate on TLS connections, so a password
	might not be necessary.</dd>
</dl>
