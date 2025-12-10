# SolarNode Standard Datum Filters

This project provides a SolarNode plugin that provides a variety of datum filter components that can
be used to transform datum collected by other plugins before they are posted to SolarNetwork. These
filters can be useful for scenarios like this:

 1. a plugin collects datum with a lot of properties that you don't need
 2. you want to collect some properties less frequently than others within the same datum
 3. you want to collect entire datum less frequently than they are produced
 4. you want to generate an accumulating meter reading property out of another property
 5. you'd like to downsample high-frequency datum into lower-frequency averages
 6. you'd like to toggle operational modes based on datum properties
 7. you need to combine the properties from multiple sources into a new source

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **Datum Filters (Standard)**. For more information, see the individual
documentation:

 * [Control Updater Datum Filter](README-ControlUpdater.md) - update control values with expressions
 * [Downsample Datum Filter](README-Downsample.md) - to reduce the frequency of datum properties
   using an average
 * [Expression Datum Filter](README-Expression.md) - generate dynamic datum properties by evaluating
   expressions against existing datum properties
 * [Join Datum Filter](README-Join.md) - combine the properties of multiple datum streams into a
   single new stream
 * [Operational Mode Datum Filter](README-OpMode.md) - evaluate expressions to toggle operational
   modes
 * [Parameter Expression Datum Filter](README-Parameter.md) - generate dynamic filter parameters
   by evaluating expressions against existing datum properties
 * [Property Datum Filter](README-Property.md) - to remove unwanted datum properties
 * [Split Datum Filter](README-Split.md) - split the properties of a datum stream into multiple
   datum streams
 * [Throttle Datum Filter](README-Throttle.md) - to restrict the frequency of posting datum
 * [Timeout Datum Filter](README-Timeout.md) - to generate datum after a timeout
 * [Unchanged Datum Filter](README-Unchanged.md) - discard unchanged datum within a stream
 * [Unchanged Property Datum Filter](README-UnchangedProperty.md) - discard unchanged datum properties within a stream
 * [Virtual Meter Datum Filter](README-VirtualMeter.md) - derive accumulating meter-style reading
   datum properties out of other datum properties
