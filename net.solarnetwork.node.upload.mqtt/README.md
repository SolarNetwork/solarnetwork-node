# SolarNode MQTT SolarIn Connector

This project provides SolarNode plugin that uses a SolarIn MQTT server to
post datum to SolarNet and receive instructions from SolarNet, in real time.

# Install

This is a core SolarNode plugin and must be deployed manually or as part of
the base platform.

# Use

There are no user-visible settings for this plugin. Once installed, SolarNode
will connect to the SolarIn MQTT server returned as the **solarin-mqtt**
service URL reported by SolarIn.

Note that this plugin works in **conjuction** with the `BulkUploadService` 
(generally provided by the [Bulk JSON Web Uploader][bulkjsonwebpost] plugin).
If a connection to the MQTT server is not available for any reason, then datum
will be persisted locally, to be picked up by the bulk upload service, the same
as if this plugin were not installed.

# Instruction handling

This plugin listens for instructions from SolarNet as well, and attempts to
execute the instructions immediately and then post the resulting execution 
status to SolarNet (via MQTT). If any step in that process fails, the instruction
will be persisted locally, to be picked up by the batch instruction service, the
same as if this plugin were not installed. The batch instruction service is 
typically provided by the [Reactor Service][reactor.simple] plugin.

 [bulkjsonwebpost]: https://github.com/SolarNetwork/solarnetwork-node/tree/master/net.solarnetwork.node.upload.bulkjsonwebpost
 [reactor.simple]: https://github.com/SolarNetwork/solarnetwork-node/tree/master/net.solarnetwork.node.reactor.simple
