# SolarNode Auto Settings

This project provides SolarNode plugin that can apply settings from a CSV file when SolarNode starts.

# Install

The plugin is meant for developers and can be manually installed.

# Use

There are no user-visible configuration settings for this plugin. When SolarNode starts up,
the plugin looks for a `conf/auto-settings.csv` file. If found, the settings in that file are
applied.

## CSV settings format

The settings format is the same as exported via the _Settings backup & restore_ section on
the SolarNode Setup screen. Exporting the settings for an already-configured node is a great
way to discover the required format of settings for the component you're interested in applying
auto-settings to.

The CSV **must** include a header row, which is skipped. All other rows will be processed as settings.

The columns required are:

 1. `key` - the setting key
 2. `type` - the setting type
 3. `value` - the setting value
 4. `flags`- should be set to `0`
 5. `modified` - a date in `yyyy-MM-dd HH:mm:ss` format
 