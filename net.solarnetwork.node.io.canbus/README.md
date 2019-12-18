# SolarNode CAN bus

This project provides SolarNode plugin that integrates SolarNode with a CAN bus 
network. The plugin integrates with CAN via the [`socketcand`][socketcand] network 
daemon.

# SolarNode KCD extension

## Building JAXB bindings

```
xjc src/net/solarnetwork/node/io/canbus/schema/SN-Definition-Datum.xsd \
  -b src/net/solarnetwork/node/io/canbus/schema/SN-Definition-Datum.xjb.xml \
  -d src
```

You might need to specify a `JAVA_HOME` property that points to a JDK with the `xjc` tool
available, for example if your default JDK is Java 11 which doesn't include this tool.
For example:

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home xjc \
  src/net/solarnetwork/node/io/canbus/schema/SN-Definition-Datum.xsd \
  -b src/net/solarnetwork/node/io/canbus/schema/SN-Definition-Datum.xjb.xml \
  -d src
```

[socketcand]: https://github.com/linux-can/socketcand
