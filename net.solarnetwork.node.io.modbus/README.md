# SolarNode Modbus I/O API

This plugin provides a generic API for communicating with Modbus devices. By itself
this plugin does not provide anything: another plugin that implements this API must
be deployed at runtime to provide Modbus integration support, for example the
[Jamod](../net.solarnetwork.node.io.modbus.jamod/) plugin.

# `ModbusNetwork` - main entry point

The [`ModbusNetwork`](src/net/solarnetwork/node/io/modbus/ModbusNetwork.java) API
is the main entry point for plugins that want to integrate with a Modbus device.
This API models a single physical Modbus network, regardless of the transport
used by the implementation. Service provider plugins are expected to provide an
implementation of this API as a service for other plugins to use at runtime.

The main method used by clients is the `createConnection(int unitId)` method. This
returns a [`ModbusConnection`](src/net/solarnetwork/node/io/modbus/ModbusConnection.java)
for a specific device on the network.

The `performAction(ModbusConnectionAction<T> action, int unitId)` method is
a handy way for clients to perform an action such as read or write to a Modbus
device safely. Here's an example that reads 8 "coil" registers from device 123:

```java
ModbusNetwork modbus = getModbusNetwork(); // e.g. lookup service in runtime
BitSet result = modbus.performAction(123, conn -> {
	return conn.readDiscreetValues(0, 8);
});
```

# `ModbusConnection` - access to a single Modbus device

The [`ModbusConnection`](src/net/solarnetwork/node/io/modbus/ModbusConnection.java)
API models a connection to a single Modbus device. The API provides methods for
reading Modbus registers and writing to registers, in a variety of formats.

# Supporting client classes

The [`net.solarnetwork.node.io.modbus.support`](src/net/solarnetwork/node/io/modbus/support/)
package contains several useful classes for working with this Modbus API. For
example the [`ModbusDataDatumDataSourceSupport`](src/net/solarnetwork/node/io/modbus/support/ModbusDataDatumDataSourceSupport.java)
class can be used as a starting point for `net.solarnetwork.node.DatumDataSource`
implementations. It uses the [`ModbusData`](src/net/solarnetwork/node/io/modbus/ModbusData.java)
class which makes it easy to store the data captured from Modbus devices and populate
`net.solarnetwork.node.domain.Datum` instances from that data in a thread-safe manner.
