# SolarNode - the distributed SolarNetwork platform

SolarNode is the platform for the distributed component of [SolarNetwork][sn]. It is
mainly responsible for:

 * collecting data from devices and posting it to SolarNetwork
 * handling instructions from SolarNetwork, and posting the results back to SolarNetwork

SolarNode is designed to be deployed on low-power devices that have some sort of network
connectivity.

To get started, please see the [SolarNetwork Developer Guide][dev-guide].

## Central Repository coordinates

The OSGi bundles are published to the [Central Repository](https://central.sonatype.com/) under the
`net.solarnetwork.node` group identifier.

## Versioning

The individual OSGi bundle projects define their own independent versions (in their `MANIFEST.MF`
resources). The entire repository is also tagged after bundles or groups of bundles have been
published to the Central Repository, as a sort of "simultaneous release" grouping version.

[dev-guide]: https://github.com/SolarNetwork/solarnetwork/wiki/Developer-Guide
[sn]: https://solarnetwork.net/
