# SolarNode Downsample Datum Filter

This component provides a way to down-sample higher-frequency datum samples into lower-frequency
(averaged) datum samples. The filter will collect a configurable number of samples and then generate
a down-sampled sample where an **average** of each collected _instantaneous_ property is included.
In addition **minimum** and **maximum** values of each averaged property are added.

# Use

Once installed, a new **Downsample Datum Filter** component will appear on the 
**Settings > Datum Filter** page on your SolarNode. Click on the **Manage** button to configure 
filters.

<img alt="Downsample filter settings" src="docs/solarnode-downsample-filter-settings@2x.png" width="934">

# Settings

Each filter configuration contains the following overall settings:

| Setting            | Description                                                       |
|:-------------------|:------------------------------------------------------------------|
| Service Name          | A unique ID for the filter, to be referenced by other components. |
| Service Group         | An optional service group name to assign. |
| Source ID             | The source ID(s) to filter. |
| Required Mode         | If configured, an [operational mode](https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Operational-Modes) that must be active for this filter to be applied. |
| Required Tag          | Only apply the filter on datum with the given tag. A tag may be prefixed with <code>!</code> to invert the logic so that the filter only applies to datum **without** the given tag. Multiple tags can be defined using a `,` delimiter, in which case **at least one** of the configured tags must match to apply the filter. |
| Sample Count          | The number of samples to average over. |
| Sample Duration       | A duration in seconds to collect samples within before generating an averaged output. Overrides the **Sample Count** setting. |
| Decimal Scale         | A maximum number of digits after the decimal point to round to. Set to`0` to round to whole numbers. |
| Property Excludes     | A list of property names to exclude. |
| Min Property Template | A string format to use for computed minimum property values. Use `%s` as the placeholder for the original property name, e.g. `%s_min`. |
| Max Property Template | A string format to use for computed maximum property values. Use `%s` as the placeholder for the original property name, e.g. `%s_max`. |

## Settings notes

<dl>
	<dt>Source ID</dt>
	<dd>This is a case-insensitive regular expression pattern to match against datum source ID values.
	<b>Only</b> datum with matching source ID values will be filtered. This is required.</dd>
</dl>
