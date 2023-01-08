# SolarNode Modbus I/O - modbus4j

This plugin provides configurable Modbus network connections for other SolarNode components.

## TCP Modbus connection

TCP Modbus connections work via TCP socket connections to a remote Modbus device.
You configure this type of connection with a host name (or IP address) and a port
number.

![TCP Modbus settings](docs/modbus-tcp-settings.png)

## Serial Modbus connection

Serial Modbus connections for modbus4j are provided by other plugins, such as the
[modbus4j jSerialComm](../net.solarnetwork.node.io.modbus.modbus4j.jsc/) plugin.
