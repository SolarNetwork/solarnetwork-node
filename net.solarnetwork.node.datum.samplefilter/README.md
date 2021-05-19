# SolarNode Standard Datum Filters

This project provides a SolarNode plugin that provides a variety of datum filter components that can
be used to transform datum collected by other plugins before they are posted to SolarNetwork. These
filters can be useful for scenarios like this:

 1. a plugin collects datum with a lot of properties that you don't need
 2. you want to collect some properties less frequently than others within the same datum
 3. you want to collect entire datum less frequently than they are produced
 4. you want to generate an accumulating meter reading property out of another property

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **Datum Filters (Standard)**. For more information, see the individual
documentation:

 * [Property Datum Filter](README-Property.md) - to remove unwanted datum properties
 * [Throttle Datum Filter](README-Throttle.md) - to restrict the frequency of posting datum
 * [Downsample Datum Filter](README-Downsample.md) - to reduce the frequency of datum properties 
   using an average
 * [Expression Datum Filter](README-Expression.md) - generate dynamic datum properties by evaluating
   expressions against existing datum properties
 * [Virtual Meter Datum Filter](README-VirtualMeter.md) - derive accumulating meter-style reading
   datum properties out of other datum properties
