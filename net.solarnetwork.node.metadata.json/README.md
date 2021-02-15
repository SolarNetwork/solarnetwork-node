# SolarNet Data Source Metadata Service

This project provides SolarNode plugin that allows SolarNode to save metadata specific to data
sources collected by other plugins to SolarIn. It uses the SolarIn `/datum/meta/{nodeId}/{sourceId}`
API to [get][datum-meta-view] and [add][datum-meta-post] the metadata as JSON documents.

# Install

This is a core SolarNode plugin and must be deployed manually or as part of the base platform.

# Use

Once installed, a new **SolarNet Data Source Metadata Service** group will appear on the
 **Settings** page on your SolarNode.

![settings](docs/solarnode-settings-JsonDatumMetadataService.png)

## Settings

| Setting             | Description |
|:--------------------|:---------------------------------------------------------------------------------------------|
| Coalesce Seconds    | A minimum number of seconds between data source metadata updates to wait before persisting to SolarNetwork. This helps minimize network traffic. |

## Setting notes

The **Coalesce Seconds** setting affects how frequently SolarNode will persist and synchronize a
given data source's metdata. SolarNode both persists the metadata locally on the node as well as
posting it to SolarNetwork as datum source  metadata. Both persisting any especially posting the
metadata takes time, and if a data source collects datum at a high frequency SolarNode might not be
able to keep up and will consume a lot of network traffic.

[datum-meta-view]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarIn-API#node-datum-metadata-view
[datum-meta-post]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarIn-API#node-datum-metadata-add
