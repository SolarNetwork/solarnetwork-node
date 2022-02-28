# SolarNode S3 Backup Service

This project provides a cloud-based backup service to SolarNode.

![settings](docs/solarnode-s3-backup-settings.png)

# Configuration

The service supports the following settings:

| Setting       | Key               | Default            | Description                               |
|:--------------|:------------------|:-------------------|-------------------------------------------|
| AWS Token     | `accessToken`     |                    | AWS access token for authentication.      |
| AWS Secret    | `accessSecret`    |                    | AWS access token secret.                  |
| AWS Region    | `regionName`      | us-west-2          | AWS service region.                       |
| S3 Bucket     | `bucketName`      |                    | S3 bucket name to save backups to.        |
| S3 Path       | `objectKeyPrefix` | solarnode-backups/ | A prefix to add to all backup data files. |
| Storage Class | `storageClass`    | STANDARD           | An S3 supported storage class name.       |

All values are required. The `S3 Path` value can be empty. The **Key** values
are Configuration Admin keys for the `net.solarnetwork.node.backup.s3.S3BackupService`
PID. That means you can configure these in a 
`conf/services/net.solarnetwork.node.backup.s3.S3BackupService.cfg` file on the
node if you don't want to manage them via the Setup GUI. For example:

```
# Sample conf/services/net.solarnetwork.node.backup.s3.S3BackupService.cfg file

accessToken = 123abc
accessSecret = 234bcd
regionName = us-east-1
bucketName = mybucket
storageClass = STANDARD_IA
```

# S3 Structure

Backups are stored using a **shared** object structure, where individual backup resources are named
after the SHA256 digest of their content with a `backup-data/` prefix added. Metadata about each
backup (for example a listing of the resources included in the backup) is then stored as an object
named with the node ID and backup timestamp with a `backup-meta/` prefix added. The object names use
this pattern:

| Object Type | Path Template |
|:------------|:--------------|
| Metadata    | `backup-meta/node-{nodeId}-backup-{timestamp}` |
| Resource    | `backup-data/{sha256Hex}` |

| Placeholder | Description |
|:------------|:------------|
| `{nodeId}`    | The ID of the node that created the backup. |
| `{timestamp}` | The backup date, using the `yyyyMMdd'T'HHmmss` pattern. |
| `{sha256Hex}` | The SHA256 digest of the associated resource, encoded in hex. |

Here's a an example listing of the objects stored in S3 after a couple of backups have finished:

![objects](docs/solarnode-s3-backup-objects.png)

The **shared** aspect of the backups means that individual `backup-data/` objects can be referenced
by multiple backups. Once a resource is backed up to S3, it won't be uploaded to S3 again in future
backups unless the resource changes.

The **shared** aspect of the backups also means that multiple _nodes_ can be configured to save
backup data to the same S3 location. In that situation the same resources across the nodes will only
be stored once in `backup-data/`, and referenced by their respective backup metadata.

# Metadata Structure

The backup metadata is a JSON document that defines some general information about the backup along
with a list of all resources included in the backup. Each resource contains a SolarNode-specific
`providerKey` representing the provider of the resource, a `backupPath` defined by that provider,
and an `objectKey` that points to the S3 object that contains the data for that resource.

```json
{
  "complete": true,
  "date": 1507093749000,
  "key": "node-163-backup-20171004T180909",
  "nodeId": 163,
  "resourceMetadata": [
    {
      "backupPath": "net.solarnetwork.node.backup.FileBackupResourceProvider/app/main/net.solarnetwork.node.datum.samplefilter-1.1.0.jar",
      "modificationDate": 0,
      "objectKey": "solarnode-backups/backup-data/45a303aae39ad8e53fcf38162bf718cd7bc0b70d1344eaae619d7e9f665cc58d",
      "providerKey": "net.solarnetwork.node.backup.FileBackupResourceProvider"
    },
    {
      "backupPath": "net.solarnetwork.node.setup.impl.DefaultKeystoreService/node.jks",
      "modificationDate": 1471324554000,
      "objectKey": "solarnode-backups/backup-data/48b811a37e26ffba3743b2eb421c0c10d1772776c29ddceb83eb7b25c203b9b1",
      "providerKey": "net.solarnetwork.node.setup.impl.DefaultKeystoreService"
    },
    {
      "backupPath": "net.solarnetwork.node.settings.ca.CASettingsService/settings.csv",
      "modificationDate": -1,
      "objectKey": "solarnode-backups/backup-data/9754cb0ac2df3891fe1158905a307acc312dd2550fcb75926f7eaff2778afba5",
      "providerKey": "net.solarnetwork.node.settings.ca.CASettingsService"
    }
  ]
}
```

## Metadata object

The metadata JSON object has the following properties:

| Property           |  Type | Description |
|:-------------------|:------|:------------|
| `complete`         | boolean | `true` if the full backup was completed successfully. |
| `date`             | number | The date the backup was completed. |
| `key`              | string | The unique name of the backup, which is also used in the metadata object path. |
| `nodeId`           | number | The ID of the node that created the backup. |
| `resourceMetadata` | array  | List of resource metadata objects. |

## Resource metadata object

The `resourceMetadata` array contains a list of JSON objects, each of which represents an individual
backup resource. The SolarNode backup service does not actually control what should be included in
any given backup, nor understand what any resource represents. Instead it relies on SolarNode
plugins to contribute [Backup Resource Provider][BackupResourceProvider] services at runtime, each
of which contributes a set of resources to the backup, and can restore those resources when asked.
Thus to understand what a given backup resource actually is, you need to consult the documentation
of the Backup Resource Provider that provided the resource, which is uniquely defined by a `key`.

Each JSON object has the following properties:

| Property           |  Type | Description |
|:-------------------|:------|:------------|
| `providerKey`      | string | The unique ID of the [Backup Resource Provider][BackupResourceProvider] services that contributed the backup resource. |
| `objectKey`        | string | The absolute S3 object path that contains the backup resource data. |
| `backupPath`       | string | A SolarNode resource-specific path. These take the form of `{providerKey}/{path}`. |
| `modificationDate` | number | A millisecond epoch modification date associated with the resource. If less than `1` then the date is _unknown_. |

[BackupResourceProvider]: ../net.solarnetwork.node/src/net/solarnetwork/node/backup/BackupResourceProvider.java
