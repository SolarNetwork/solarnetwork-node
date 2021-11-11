# Datum Data Source Operational Mode Invoker

This project provides a SolarNode plugin that allows you to configure groups of datum data sources
that respond to operational mode changes. As modes are activated, matching data sources can be
scheduled to poll for datum at a schedule specific to that mode. When modes are deactivated, the
polling configured for that schedule will stop.

![settings](docs/solarnode-datum-opmode-invoker-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It
appears under the **Datum** category as **Operational Mode Data Source Scheduler**.


# Use

Once installed, a new **Datum Data Source Operational Mode Invoker** component will appear on the
**Settings** page on your SolarNode. Click on the **Manage** button to configure services. You can
configure as many services as you like.


## Overall settings

Each device configuration contains the following overall settings:

| Setting | Description |
|---------|-------------|
| Service Name | A unique name to identify this service with. |
| Service Group | A group name to associate this service with. |
| Mode | The operational mode that must be active for this invoker configuration to be applied. |


## Data source configuration settings

Each data source configuration defines a schedule and which data sources to sample. The configuration
properties are joined logically as "and" conditions, meaning all conditions must be met for a data 
source to be included with the schedule.

**Note** if a configuration property is left empty, it matchs *all* values. Thus if all conditions
are left empty all data sources will be included in the configuration.

| Setting | Description |
|---------|-------------|
| Service Name | The service name of a data source to match. |
| Service Group | The service group of a data source to match. |
| Datum Type | The datum type the data source must collect. |
| Schedule | Either a number of seconds, or a cron expression, at which to sample datum from the data sources matching this configuration. |
| Persist | When enabled, then persist the polled datum in SolarNetwork. Otherwise the datum are still available for other plugins to use but not persisted. |


## Overall device settings notes

 * **Schedule:** if just a number, then the frequency in seconds at which to poll for datum. Otherwise a 
   [cron expression][cron-syntax] representing the schedule at which to poll for datum.
 * **Persist:** when disabled the datum polled from the configured data sources can be used by other plugins. For 
	example, the [SolarFlux Upload][solarflux-upload] plugin will upload the datum to [SolarFlux][solarflux]. Thus 
	you could use an operational mode to toggle higher-frequency datum sampling to SolarFlux on and off.


[cron-syntax]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Cron-Job-Syntax
[solarflux]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarFlux-API
[solarflux-upload]: https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.upload.flux
