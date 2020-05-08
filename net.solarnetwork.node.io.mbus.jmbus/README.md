# SolarNode M-Bus I/O - jM-Bus

This plugin provides configurable M-Bus network connections for other SolarNode components. There
are two types of networks supported: serial and TCP.

## Serial M-Bus connection

Serial M-Bus connections work via device-specific serial ports made available on the host operating
system. The name of each port is operating-system specific. Some common examples are `/dev/ttyS0`
(Linux serial port), `/dev/ttyUSB0` (Linux USB serial port), and `COM1` (Windows serial port).

## TCP M-Bus connection

TCP M-Bus connections work via TCP socket connections to a remote M-Bus device. You configure this
type of connection with a host name (or IP address) and a port number.
