# SolarNode Modbus I/O - modbus4j serial (JSC)

This plugin provides configurable Modbus network connections for other SolarNode components.
It uses the jSerialComm library for serial port access.

## Serial Modbus connection

Serial Modbus connections work via device-specific serial ports made available on the
host operating system. The name of each port is operating-system specific. Some 
common examples are `/dev/ttyS0` (Linux serial port), `/dev/ttyUSB0` (Linux USB serial
port), and `COM1` (Windows serial port).

![Modbus serial settings](docs/modbus-serial-settings.png)

## TCP Modbus connection

TCP Modbus connections for modbus4j are provided by the core 
[modbus4j](../net.solarnetwork.node.io.modbus.modbus4j) plugin.
