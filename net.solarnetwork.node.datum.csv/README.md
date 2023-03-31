# SolarNode CSV Resource Datum Source

This project provides SolarNode plugin that can collect data from URLs returning
comma-separated-value (CSV) data, either as a node or location datum stream.

<img title="CSV Resource Datum Source settings" src="docs/solarnode-csv-settings.png" width="604">

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **CSV Datum Source**.


# Use

Once installed, new **CSV Location Resource** and **CSV Resource** components will appear on the
**Settings** page on your SolarNode. Click on the **Manage** button to configure components. You'll
need to add one configuration for each CSV lookup you want to collect data from.


# CSV Column References

All CSV column reference settings accept a list of column ranges. When more than one column is
configured, then all column values are joined together with a ` ` (space) character to form a single
final value.

A column can be referenced as a number, starting from `1`, or a letter, starting from `A` (common
in spreadsheet applications). After `Z` comes `AA`, `AB`, `AC`, and so on.

A range of columns can be referenced using a `-` (dash) delimiter. For example `1-3` means columns
1, 2, and 3 will be included. Similarly `A-C` would include the same columns. Letters and numbers
can be freely mixed. For example `1-C` would include the same columns. The letters are also
case-insensitive, so `a-c` would include the same columns.

Multiple columns, or ranges of columns, can be referenced using a `,` (comma) delimiter. For example
`1,3` means columns 1 and 3 would be included. Similarly `A,C` would include the same columns. A
more complex example of `1,F-H,3` would include columns 1, 6, 7, 8, and 3. The order of the
references are preserved when joining the columns together with a ` ` (space) character.


# CSV Resource

The CSV Resource component collects CSV data as a **node** datum stream. See [CSV Location
Resource](#csv-location-resource) if you need to collect a **location** datum stream.

## Settings

Each configuration contains the following settings:

| Setting            | Description |
|--------------------|-------------|
| Schedule           | A cron schedule that determines when data is collected.  |
| Service Name       | A unique name to identify this data source with. |
| Service Group      | A group name to associate this data source with. |
| Source ID          | The SolarNetwork unique source ID to assign to datum collected from this device. |
| URL                | The URL of the CSV resource to fetch. The following parameters are allowed: `{date}` the current date, formatted using the **URL Date Format** setting. |
| Character Encoding | The text encoding of the CSV resource, as a [](http://www.ietf.org/rfc/rfc2278.txt) value. For example `UTF-8`, `US-ASCII`, `ISO-LATIN-1`. |
| Timeout            | A network timeout, in milliseconds. |
| Skip Rows          | The number of CSV rows to skip. This is useful for skipping a "header" row. If negative, then return rows from the end of the data. For example `-1` would return the last row. |
| Keep Rows          | The number of CSV rows to turn into datum. Each row becomes a datum. |
| Source ID Column   | If configured, then extract the datum source ID from this CSV column. See [CSV Column References](#csv-column-references) for the allowed syntax. |
| URL Date Format    | The [date format][datepat] to format the `{date}` URL parameter. |
| Date Column        | The CSV column to parse the datum timestamp from. See [CSV Column References](#csv-column-references) for the allowed syntax. |
| Date Format        | The [date format][datepat] to use for parsing the **Date Column** value. |
| Time Zone          | The time zone to use when parsing and formatting dates. |
| Sample Maximum Age      | A maximum time to cache captured CSV data, in milliseconds. |
| Property Configurations | A list of CSV row property settings. Any number of property configurations can be added, to capture any number of CSV columns. |

# CSV Location Resource

TODO

## Locations

The **Location** and **Location Type** settings are used to associate the data with a SolarNetwork
location datum stream. These locations must be created by SolarNetwork administrators. You can use
the [Location Request API][loc-req] to request a new location if a suitable one does not exist
already.


[expr]: https://github.com/SolarNetwork/solarnetwork/wiki/Expression-Languages
[datepat]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns
[ExpressionRoot]: https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.datum.modbus/src/net/solarnetwork/node/datum/modbus/ExpressionRoot.java
[GeneralNodeDatum]: https://github.com/SolarNetwork/solarnetwork-node/blob/develop/net.solarnetwork.node/src/net/solarnetwork/node/domain/GeneralNodeDatum.java
[loc-req]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-Location-Request-API
[ModbusData]: https://github.com/SolarNetwork/solarnetwork-node/blob/develop/net.solarnetwork.node.io.modbus/src/net/solarnetwork/node/io/modbus/ModbusData.java
[meta-api]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarQuery-API#add-node-datum-metadata
[metadata-key-path]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-global-objects#metadata-filter-key-paths
[regex]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html#sum
[sn-cron-syntax]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Cron-Job-Syntax
[sn-expressions]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Expressions
