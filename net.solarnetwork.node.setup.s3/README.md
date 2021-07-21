# SolarNode S3 Setup Service

This project provides a cloud-based setup service to SolarNode.

# Use Case

The primary use case for this plugin is to be able to deploy SolarNode basic platform updates to a
set of nodes that are to be centrally managed. To achieve this, versioned package metadata and
associated data files are stored on Amazon S3, and the node can be instructed to download and
install the files for a specific version (or the _latest_ version).

Another use case for this plugin is to provide a way to update the base platform of the node. The
node's Plugin GUI allows managing only the _application_ plugins (in the `app/main` directory), not
the base platform (primarily the `app/boot` and `app/core` directories).

In other words, this provides a basic managed "update" mechanism for a set of nodes with similar
configuration needs.


# Limitations

This service is only designed to update the SolarNode platform. It will only install files within
the platform's home directory (e.g. `/home/solar`).


# Configuration

The service supports the following settings:

| Setting    | Key                      | Default            | Description                           |
|------------|--------------------------|--------------------|---------------------------------------|
| S3 Path    | `objectKeyPrefix`        | solarnode-backups/ | A prefix to add to all object keys.   |
| TODO       | `performFirstTimeUpdate` | true               | Install latest backup on first start. |

The **S3 Path** value can be empty.

The **performFirstTimeUpdate** value, when `true`, causes the plugin to install the _latest_
available package when the node platform starts up for the first time. For this to work as expected,
the S3 settings must already be configured -- see the information on the `S3SetupManager.cfg` file
at the end of this section.

The **Key** values are Configuration Admin keys for the
`net.solarnetwork.node.setup.s3.S3SetupManager` PID. That means you can configure these in a
`conf/services/net.solarnetwork.node.setup.s3.S3SetupManager.cfg` file on the node if you don't want
to manage them via the Setup GUI.


# S3 Structure

Packages are defined by a simple JSON document stored with a `setup-meta/` prefix. The object keys
must end with an ever-increasing integer number, which represents the _version_ of the package. The
keys may contain any suffix, for example `.json`. S3 always lists object keys lexicographically in
ascending order, so when a node is instructed to update to the latest package, it will use the
**last** available key with the `setup-meta/` prefix.

The **S3 Path** setting allows you to create different package sets for different groups of nodes.
If you need to manage one set of nodes deployed with a specific configuration set, and another set
of nodes with a _different_ configuration set, you could use different S3 Path values to manage
their configuration independently from one another. They can reference shared or different objects
as needed.

For example, imagine a S3 layout like this:

```
 + setup-1/
 | \-+ setup-meta/
 |     |-- 000001.json
 |     \-- 000002.json
 |
 + setup-2/
 | \-+ setup-meta/
 |     |-- 000001.json
 |     \-- 000002.json
 |
 + setup-data/
   |-- data-001.tgz
   |-- data-002.tgz
   \-- data-003.tgz
```

Here there are two different configuration sets, **setup-1** and **setup-2**. They each have their
own version history, but they are free to reference any object from the **setup-data** prefix as
needed.

# Package Metadata Structure

The package metadata is a JSON document that defines some general information about the package
along with a list of all other S3 objects included in the package. The object type is determined by
the object key, so the keys are expected to be named with file extensions appropriate to the object
content. Currently the only supported object type is **tar archives**. The archives can be
compressed using common formats (gzip, bzip2, and xz).

Here's a basic example of a package metadata file. Imagine this is stored in S3 with an object key
`setup-meta/000001.json` (leading zeros are used to keep versions sorted lexicographically):

```json
{
    "restartRequired":true,
    "objects":[
        "setup-data/foobar.txz"
    ]
}
```

Because the object key ends in `00001.json` the _version_ of the package is **00001**. This package
includes just one object: `setupdata/foobar.txz`. Let's assume the tar archive contains the
following files:

 * `conf/auto-settings.csv`
 * `app/main/super-duper-plugin-1.0.0.jar`

When the node is instructed to install this package, it will download the `foobar.txz` file from S3
and then extract all the files from it, into the platform's home directory.

Here are the available properties in the package metadata:

| Key               | Type          | Description                                                       |
|:------------------|:--------------|:------------------------------------------------------------------|
| `cleanPaths`      | array<string> | Optional node paths to files or directories to delete.            |
| `objects`         | array<string> | S3 object keys for setup resources to install.                    |
| `restartRequired` | boolean       | If `true` then restart SolarNode when the setup task is complete. |
| `syncPaths`       | array<string> | Optional node paths to directories to delete old files from.      |
| `packages`        | array<object> | Optional list of OS packages to install, see [below](#os-packages). |

Here is a fuller example package metadata:

```json
{
    "syncPaths":[
        "{sn.home}/app/boot",
        "{sn.home}/app/core",
        "{sn.home}/app/main"
    ],
    "cleanPaths":[
        "{osgi.configuration.area}/config.ini"
    ],
    "objects":[
        "solarnode-backups/setup-data/foobar.txz",
        "solarnode-backups/setup-data/foobar-app.txz"
    ],
    "packages":[
        {"action":"Install", "name":"solarnode-base", "version":"1.11.0-1"},
        {"action":"Install", "name":"solarnode-app-core", "version":"1.22.1-1"}
    ],
    "restartRequired":true
}
```


## Node path variables

Any node path in the package metadata may contain variables in the form `{variable}`. All SolarNode
system properties will be available for substitution. For example, a path like

```
{osgi.configuration.area}/config.ini
```

might resolve to `/run/solar/config.ini` so the Equinox startup configuration is re-loaded from
`/home/solar/conf/config.ini` after SolarNode restarts.

## Objects

An object is an S3 path for a resource to install. The resource can be either a compressed tar
archive or an OS package.

### Special objects

The following special objects are recognized:

| Object | Description |
|:-------|:------------|
| `_refreshPackages` | Ask the OS package cache to be refreshed. This can be useful if a subsequent object is itself an OS package that depends on updated OS packages. |


## Synchronized paths

The `syncPaths` list of paths represent directories whose contents you want to only contain the
files installed from any of the `objects` in the package. Essentially this causes this plugin to
delete any files **not** installed by the package in any of these directories.

Using the [example metadata from above](#package-metadata-structure), if we changed it to

```json
{
    "restartRequired":true,
    "objects":[
        "setup-data/foobar.txz"
    ],
    "syncPaths":[
        "{sn.home}/app/main"
    ]
}
```

Then after the setup task completes the `/home/solar/app/main` directory will **only** contain the
`super-duper-plugin-1.0.0.jar` (installed from the `setup-data/foobar.txz` object). Conversely, if
`syncPaths` were not defined and there happened to be other files in the `/home/solar/app/main`
directory, those other files would remain there after the setup task completed.

## OS Packages

Operating system packages from whatever repositories are configured on the node can be installed via
the `packages` configuration array of package configuration objects. Each package configuration
object contains the following keys:

| Property    | Type          | Description |
|:------------|:--------------|:------------|
| `action`    | string        | One of `Install`, `Remove`, or `Upgrade`. |
| `name`      | string        | The OS package name. |
| `version`   | string        | The specific version to install, or omit to install the latest available or for the `Remove` and `Upgrade` actions. |

### Install action

The `Install` action is used to install the OS package `name`. The `version` property can be used to
specify a specific version, or omitted to install the highest-available version.

### Remove action

The `Remove` action will remove the `name` package. The `version` property is not used.

### Upgrade action

The `Upgrade` action will upgrade all installed OS packages to their highest-available versions. The
`name` and `version` properties are not used.


# UpdatePlatform Instruction

The plugin responds to the `UpdatePlatform` instruction topic. The following
instruction parameters are supported:

| Parameter    | Description             |
|--------------|-------------------------|
| `Version`    | The version to install. |

If `Version` is not specified, then the _latest_ package will be installed.

Using the SolarUser API's [Queue Instruction][queue-instr] endpoint you can trigger this instruction
to install the latest package with a `POST` request like

```
/solaruser/api/v1/sec/instr/add?topic=UpdatePlatform&nodeId=123
```

To install a specific version, you'd use a `POST` request like

```
/solaruser/api/v1/sec/instr/add?topic=UpdatePlatform&nodeId=123&parameters%5B0%5D.name=Version&parameters%5B0%5D.value=000001
```


# Node Metadata

The plugin will maintain the following node property metadata under the `setup`
key:

| Key          | Type   | Description                                  |
|--------------|--------|----------------------------------------------|
| `s3-version` | number | The most recently installed package version. |


# S3 Configuration

This plugin works in tandem with with [S3 Backup][s3-backup] plugin, and depends on the same S3
configuration defined in that plugin. You can create a custom node image that includes the
`conf/services/net.solarnetwork.node.backup.s3.S3BackupService.cfg` configuration file to ensure
this service works from the start.


 [s3-backup]: https://github.com/SolarNetwork/solarnetwork-node/tree/master/net.solarnetwork.node.backup.s3
 [queue-instr]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-API#queue-instruction
