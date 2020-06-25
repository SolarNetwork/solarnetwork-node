# SolarNode External "SolarBackup" Backup Provider

This project supports managing backups of device resources via an external helper command
_solarbackup_. The [`sn-solarbackup`][sn-solarbackup] package provides this command in SolarNodeOS.

# Configuration

This plugin provides a managed service factory using the `net.solarnetwork.node.backup.ext` factory
PID. You must configure a properties file in the SolarNode's service configuration directory,
typically `/etc/solarnode/services`, for each desired instance of this factory. The file name
of the properties file must follow this pattern:

```
net.solarnetwork.node.backup.ext-NAME.cfg
```

The `NAME` portion is a unique name for the backup provider instance. An example properties file is
available in the *example/configuration* directory of this project. For example:

```
name = foobar
resourceBundleDir = /usr/share/solarnode/backup.d
useSudo = true
backupResourceExtension = .tgz
```

Most of these values have defaults designed to work with SolarNodeOS and don't actually need to
be specified. The `name` property is required at a minimum.

[sn-solarbackup]: https://github.com/SolarNetwork/solarnode-os-packages/tree/develop/solarbackup/debian
