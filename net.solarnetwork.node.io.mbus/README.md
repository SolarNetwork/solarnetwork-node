# SolarNode M-Bus I/O API

This plugin provides a generic API for communicating with M-Bus devices. By itself this plugin does
not provide anything: another plugin that implements this API must be deployed at runtime to provide
M-Bus integration support, for example the [jMBus](../net.solarnetwork.node.io.mbus.jmbus/) plugin.

# `MBusNetwork` - main entry point

The [`MBusNetwork`](src/net/solarnetwork/node/io/mbus/MBusNetwork.java) API is the main entry point
for plugins that want to integrate with a M-Bus device. This API models a single physical M-Bus
network, regardless of the transport used by the implementation. Service provider plugins are
expected to provide an implementation of this API as a service for other plugins to use at runtime.

# `MBusConnection` - access to a single M-Bus device

The [`MBusConnection`](src/net/solarnetwork/node/io/mbus/MBusConnection.java) API models a
connection to a single M-Bus device. The API provides methods for reading MBus registers and writing
to registers, in a variety of formats.
