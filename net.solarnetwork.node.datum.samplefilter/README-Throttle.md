# SolarNode Throttle Datum Filter

This component can throttle **entire datum** over time, so that they are posted to SolarNetwork less
frequently than a plugin that collects the data produces them. This can be useful if you need a
plugin to collect data at a high frequency for use internally by SolarNode but don't need to save
such high resolution of data in SolarNetwork. For example, a plugin that monitors a device and
responds quickly to changes in the data might be configured to sample data every second, but you
only want to capture that data once per minute in SolarNetwork.

The general idea for filtering datum is to configure rules that define which datum **sources** you
want to filter, along with **time limit** to throttle matching datum by. Any datum matching the
sources that are captured faster than the time limit will filtered and **not** uploaded to
SolarNetwork.

# Use

Once installed, a new **Throttle Datum Filter** component will appear on the 
**Settings > Datum Filter** page on your SolarNode. Click on the **Manage** button to configure 
filters.

![Throttle filter settings](docs/solarnode-throttle-filter-settings.png)

# Settings

Each filter configuration contains the following overall settings:

| Setting            | Description                                                       |
|:-------------------|:------------------------------------------------------------------|
| Service Name       | A unique ID for the filter, to be referenced by other components. |
| Service Group      | An optional service group name to assign.                         |
| Source ID          | The source ID(s) to filter.                                       |
| Require Mode       | If configured, an [operational mode](https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Operational-Modes) that must be active for this filter to be applied. |
| Limit Seconds      | A throttle limit, in seconds, to apply to matching datum.         |

## Settings notes

<dl>
	<dt>Source ID</dt>
	<dd>This is a case-insensitive regular expression pattern to match against datum source ID values.
	<b>Only</b> datum with matching source ID values will be filtered. This is required.</dd>
	<dt>Limit Seconds</dt>
	<dd>The throttle limit is applied to datum by source ID. Before each datum is uploaded
	to SolarNetwork, the filter will check how long has elapsed since a datum with the
	same source ID was uploaded. If the elapsed time is less than the configured limit,
	the datum will not be uploaded.</dd>
</dl>
