# SolarNode External "SolarBackup" Backup Provider

This project supports managing backups of device resources via an external helper command
_solarbackup_. The [`sn-solarbackup`][sn-solarbackup] package provides this command in SolarNodeOS.

# Configuration

This plugin does not provide any UI-configurable settings in SolarNode. Instead it provides a
managed service factory using the `net.solarnetwork.node.backup.ext` factory PID. This can be
done manually, however the expected use case for this plugin is that other SolarNodeOS packages
contribute the necessary configuration to back up specific sets of device resources.

To register a service, you must configure a properties file in the SolarNode's service configuration
directory, typically `/etc/solarnode/services`, for each desired instance of this factory. The file
name of the properties file must follow this pattern:

```
net.solarnetwork.node.backup.ext-NAME.cfg
```

The `NAME` portion is a unique name for the backup provider instance. An example properties file is
available in the *example/configuration* directory of this project. For example:

```
name = network-devices
resourceBundleDir = /usr/share/solarnode/backup.d
useSudo = true
backupResourceExtension = .tgz
```

Most of these values have defaults designed to work with SolarNodeOS and don't actually need to
be specified. The `name` property is required at a minimum.

# Localization

Each backup service instance can provide localized details that SolarNode will use in its backup UI.
Standard Java resource properties files are used, with a base name that matches the `name` 
configured on the service instance, stored in the `resourceBundleDir` directory. For example, the
"default" localization for the `network-devices` service shown in the earlier example configuration would
be in `/usr/share/solarnode/backup.d/network-devices.properties`. Other localizations could be added, such
as:

 * `/usr/share/solarnode/backup.d/network-devices.properties`
 * `/usr/share/solarnode/backup.d/network-devices_en.properties`
 * `/usr/share/solarnode/backup.d/network-devices_en_GB.properties`
 
The supported message keys are:

| Key     | Description |
|:--------|:------------|
| `title` | A title for this service, e.g. _Network Devices_. |
| `desc`  | A description of the service, e.g. _Backs up network device settings._ |

An example localized message properties file looks like this:

```
title = Network Devices
desc = Backs up network device settings.
```

[sn-solarbackup]: https://github.com/SolarNetwork/solarnode-os-packages/tree/develop/solarbackup/debian
