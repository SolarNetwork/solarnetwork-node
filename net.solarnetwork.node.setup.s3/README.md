# SolarNode S3 Setup Service

This project provides a cloud-based setup service to SolarNode.

# Use Case

The primary use case for this plugin is to be able to deploy SolarNode basic
platform updates to a set of nodes that are to be centrally managed. To achieve
this, versioned package metadata and associated data files are stored on Amazon
S3, and the node can be instructed to download and install the files for a
specific version (or the _latest_ version).

Another use case for this plugin is to provide a way to update the base
platform of the node. The node's Plugin GUI allows managing only the
_application_ plugins (in the `app/main` directory), not the base platform
(primarily the `app/boot` and `app/core` directories).

In other words, this provides a basic managed "update" mechanism for a set
of nodes with similar configuration needs.


# Limitations

This service is only designed to update the SolarNode platform. It will only
install files within the platform's home directory (e.g. `/home/solar`).


# Configuration

The service supports the following settings:

| Setting    | Key             | Default            | Description                         |
|------------|-----------------|--------------------|-------------------------------------|
| S3 Path    | objectKeyPrefix | solarnode-backups/ | A prefix to add to all object keys. |

The `S3 Path` value can be empty. The **Key** values
are Configuration Admin keys for the `net.solarnetwork.node.setup.s3.S3SetupManager`
PID. That means you can configure these in a
`conf/services/net.solarnetwork.node.setup.s3.S3SetupManager.cfg` file on the
node if you don't want to manage them via the Setup GUI.

# S3 Structure

Packages are defined by a simple JSON document stored with a `setup-meta/`
prefix. The object keys must end with an ever-increasing integer number, which
represents the _version_ of the package. The keys may contain any suffix, for
example `.json`. S3 always lists object keys lexicographically in ascending
order, so when a node is instructed to update to the latest package, it will use
the **last** available key with the `setup-meta/` prefix.

The **S3 Path** setting allows you to create different package sets for different
groups of nodes. If you need to manage one set of nodes deployed with a specific
configuration set, and another set of nodes with a _different_ configuration set,
you could use different S3 Path values to manage their configuration independently
from one another. They can reference shared or different objects as needed.

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

Here there are two different configuration sets, **setup-1** and **setup-2**.
They each have their own version history, but they are free to reference
any object from the **setup-data** prefix as needed.

# Package Metadata Structure

The package metadata is a JSON document that defines some general information
about the package along with a list of all other S3 objects included in the
package. The object type is determined by the object key, so the keys are
expected to be named with file extensions appropriate to the object content.
Currently the only supported object type is **tar archives**. The archives can
be compressed using common formats (gzip, bzip2, and xz).

Here's a basic example of a package metadata file. Imagine this is stored in S3
with an object key `setup-meta/000001.json` (leading zeros are used to keep
versions sorted lexicographically):

```json
{
    "objects":[
        "setup-data/foobar.txz"
    ]
}
```

Because the object key ends in `00001.json` the _version_ of the package is
**1**. This package includes just one object: `setupdata/foobar.txz`. Let's
assume the tar archive contains the following files:

 * `conf/auto-settings.csv`
 * `app/main/super-duper-plugin-1.0.0.jar`

When the node is instructed to install this package, it will download the
`foobar.txz` file from S3 and then extract all the files from it, into the
platform's home directory.


# UpdatePlatform Instruction

The plugin responds to the `UpdatePlatform` instruction topic. The following
instruction parameters are supported:

| Parameter    | Description                                              |
|--------------|----------------------------------------------------------|
| `Version`    | The _full_ version to install (including leading zeros). |


# Node Metadata

The plugin will maintain the following node property metadata under the `setup`
key:

| Key          | Type   | Description                                  |
|--------------|--------|----------------------------------------------|
| `s3-version` | number | The most recently installed package version. |


# S3 Configuration

This plugin works in tandem with with [S3 Backup][s3-backup] plugin, and depends
on the same S3 configuration defined in that plugin. You can create a custom
node image that includes the
`conf/services/net.solarnetwork.node.backup.s3.S3BackupService.cfg`
configuration file to ensure this service works from the start.


 [s3-backup]: https://github.com/SolarNetwork/solarnetwork-node/tree/master/net.solarnetwork.node.backup.s3
