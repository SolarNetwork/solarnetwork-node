# SolarNode Metrics

This project provides core _metric_ support to SolarNode. A _metric_ is like a timestamped,
named data point value. You might describe a metric like _the power output of a solar
inverter at noon on 2024-07-18_.

# About metrics

A _metric_ in SolarNode has this basic structure:

| Property | Example | Description |
|:---------|:--------|:------------|
| `timestamp` | `2024-07-18 19:48:50.003767Z` | The date the metric was created. |
| `type`      | `s` | A classifier. The `s` type represents a raw "sample". Other types can be used to represent other metric types, such as computed aggregation values. |
| `name`      | `gridPrice` | An arbitrary name for the metric. Typically these should be concise and use an expression-friendly syntax of just letters, numbers, and underscores. |
| `value`     | `123.4` | The metric numeric value, as a 64-bit floating-point number. |

# Working with metrics

This plugin only provides the core metrics API. Other plugins must provide ways to capture metrics
or work with them in some way. For example the [Metric Harvester](../net.solarnode.node.metrics.harvester)
plugin provides a Datum Filter that can capture metrics from datum data sources.
