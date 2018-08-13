# SolarNode OS Statistics Datum Source

This project provides SolarNode plugin that can collect data from an external
helper program that provides OS statistic information in CSV form.

![](solarnode-os-stats-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It
appears under the **Datum** category as **OS Statistics Data Source**.

# Use

Once installed, a new **OS Statistics** component will appear on the 
**Settings** page on your SolarNode. Click on the**Manage** button to configure
sources.

## Overall settings

Each configuration contains the following overall settings:

| Setting            | Description                                                                      |
|--------------------|----------------------------------------------------------------------------------|
| Schedule           | A cron schedule that determines when data is collected.                          |
| Service Name       | A unique name to identify this data source with.                                 |
| Service Group      | A group name to associate this data source with.                                 |
| Sample Maximum Age | A minimum time to cache captured Modbus data, in milliseconds.                   |
| Statistics         | A list of statistic types to collect.                                            |
| Filesystems        | A list of filesystem paths to collect statistics from.                           |
| Network Devices    | A list of network device names to collect statistics from.                       |
| Command            | The external command to execute that provides the statistic data.                |

## Overall device settings notes

<dl>
	<dt>Statistics</dt>
	<dd>This is a list of pre-determined statistic types to collect. Each one will be passed
	to the external helper program as an argument.</dd>
	<dt>Command</dt>
	<dd>The helper command that provides the statistics. This program is OS dependent but
	must accept a single statistic type (from the <b>Statistics</b> setting) and return CSV
	formatted data of the expected format.</dd>
</dl>


## Command statistic types

The external helper command must support the following statistic types and return data in the
following formats. The output must always include a header row before any data rows.

### `cpu-use`

Average CPU utilization information, inspired by `sysstat`, returning the following columns:

<dl>
  <dt>date</dt>
  <dd>The date of the data, in <code>YYYY-MM-DD HH:MM:SS UTC</code> form.</dd>
  <dt>period-secs</dt>
  <dd>The length of time, in seconds, the data was averaged over.</dd>
  <dt>user</dt>
  <dd>Percentage of CPU time in user programs, from 0-100.</dd>
  <dt>system</dt>
  <dd>Percentage of CPU time in the kernel, from 0-100.</dd>
  <dt>idle</dt>
  <dd>Percentage of idle CPU time, from 0-100.</dd>
</dl>

An example output looks like:

```
date,period-secs,user,system,idle
2018-08-12 23:15:01 UTC,600,0.03,0.03,99.93
```

Any number of rows of data may be returned, but only the last row of data may be used.

