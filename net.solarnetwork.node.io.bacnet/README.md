# SolarNode BACnet I/O API

This plugin provides a generic API for communicating with BACnet devices. By itself
this plugin does not provide anything: another plugin that implements this API must
be deployed at runtime to provide BACnet integration support, for example the
[BACnet4J](../net.solarnetwork.node.io.bacnet.bacnet4j/) plugin.

# `BacnetNetwork`

The [`BacnetNetwork`](src/net/solarnetwork/node/io/bacnet/BacnetNetwork.java) API is the main entry
point for plugins that want to integrate with a BACnet device. This API models a single physical
BACnet network, regardless of the transport used by the implementation. Service provider plugins are
expected to provide an implementation of this API as a service for other plugins to use at runtime.

The main method used by clients is the `createConnection()` method. This returns a
[`BacnetConnection`](src/net/solarnetwork/node/io/bacnet/BacnetConnection.java) for the network.

```java
BacnetNetwork bacnet = getBacnetNetwork(); // e.g. lookup service in runtime
List<BacnetDeviceObjectPropertyRef> refs = getPropertyRefs(); // the props to read
try (BacnetConnection conn = bacnet.createConnection()) {
  conn.open();
  Map<BacnetDeviceObjectPropertyRef, ?> values = conn.propertyValues(refs);
  for ( Entry<BacnetDeviceObjectPropertyRef, ?> e : values.entrySet() ) {
    System.out.println(String.format("BACnet %s = [%s]", e.getKey(), e.getValue()));
  }
} catch (IOException e) {
  // handle error
}
```

# `BacnetConnection`

The [`BacnetConnection`](src/net/solarnetwork/node/io/bacnet/BacnetConnection.java)
API models a connection to the BACnet network. The API provides methods for
reading from and writing to BACnet object properties.
