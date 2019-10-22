# SolarNode CAN bus

This project provides SolarNode plugin that integrates SolarNode with a CAN bus 
network. The plugin integrates with CAN via the [`socketcand`][socketcand] network 
daemon.

# SolarNode KCD extension

## Building JAXB bindings

```
xjc src/net/solarnetwork/node/io/canbus/schema/SN-Definition-Datum.xsd \
  -b src/net/solarnetwork/node/io/canbus/schema/SN-Definition-Datum.xjb.xml \
  -d src -mark-generated
```

[socketcand]: https://github.com/linux-can/socketcand
