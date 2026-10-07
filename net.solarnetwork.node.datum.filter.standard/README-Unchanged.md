# SolarNode Unchanged Datum Filter

This component can discard **entire datum** that have not changed within a datum stream.

# Use

Once installed, a new **Unchanged Datum Filter** component will appear on the
**Settings > Datum Filter** page on your SolarNode. Click on the **Manage** button to configure
filters.

<img alt="Unchanged filter settings" src="docs/solarnode-unchanged-filter-settings@2x.png" width="697">

# Settings

Each filter configuration contains the following overall settings:

| Setting            | Description                                                       |
|:-------------------|:------------------------------------------------------------------|
| Service Name       | A unique ID for the filter, to be referenced by other components. |
| Service Group      | An optional service group name to assign.                         |
| Source ID          | The source ID(s) to filter.                                       |
| Required Mode      | If configured, an [operational mode](https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Operational-Modes) that must be active for this filter to be applied. |
| Required Tag       | Only apply the filter on datum with the given tag. A tag may be prefixed with `!` to invert the logic so that the filter only applies to datum **without** the given tag. Multiple tags can be defined using a `,` delimiter, in which case **at least one** of the configured tags must match to apply the filter. |
| Unchanged Max Seconds | When greater than `0` then the maximum number of seconds to refrain from publishing an unchanged datum within a single datum stream. |
| Property Pattern | A property name [pattern][regex] that limits the properties monitored for changes. Only property names that match this expression will be considered when determining if a datum differs from the previous datum within the datum stream. |
| Debounce Threshold | When greater than `0` then the minimum number of milliseconds a changed datum must remain unchanged before it is published. See [Debounce](#debounce) for more details. |

## Settings notes

 * **Source ID** — This is a case-insensitive [regular expression][regex] pattern to match against
   datum source ID values. If omitted then datum for _all_ source ID values will be filtered,
   otherwise only datum with _matching_ source ID values will be filtered.
 * **Unchanged Max Seconds** — Use this setting to ensure a datum stream has datum included
   occasionally, even if the property values have not changed. Having at least one datum per
   hour in a datum stream is recommended. This time period is always relative to the last
   unfiltered datum seen by the filter.

## Debounce

The **Debounce Threshold** setting can be used to ignore brief, unwanted changes from a flaky
input, such as a status value that occasionally flips to a different value and then quickly back
again. When configured, a changed datum is only published once the monitored properties have
remained at their changed values for at least the debounce threshold. Changes that revert before
the threshold elapses are discarded.

For example, with a **Debounce Threshold** of `5000` (5 seconds) and a datum sampled every
second, where `A` and `B` are values of a monitored `status` property:

```
time (s):  0  1  2  3  4  5  6  7  8  9  10 11
status:    A  B  B  A  B  B  B  B  B  B  B  B
output:    A  ·  ·  ·  ·  ·  ·  ·  ·  B  ·  ·
```

The `B` values at 1–2s revert to `A` at 3s, so they are discarded. The `B` value seen at 4s is
published at 9s, which is the first datum after it has remained unchanged for 5 seconds.

Some things to keep in mind when using a debounce threshold:

 * The filter only evaluates datum as they are captured, so it is designed for datum streams
   sampled on a regular schedule. A stable change is published on the first datum captured after
   the threshold has elapsed, so the published datum can be delayed by up to the threshold plus
   one sampling period from the actual change.
 * If **Unchanged Max Seconds** elapses while a change is not yet stable, the datum is published
   with its monitored properties set to their last published (stable) values. Other properties
   are published as captured.
 * Configure a **Property Pattern** that matches only the status-like properties you want to
   debounce. Without a pattern all properties are monitored, so continuously changing values like
   power readings would never be stable. In that case only the last stable datum values are
   published, once per **Unchanged Max Seconds**.

[regex]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html
