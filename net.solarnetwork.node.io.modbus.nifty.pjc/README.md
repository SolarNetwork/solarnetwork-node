# SolarNode Modbus I/O - Nifty Modbus serial (PureJavaComm)

This plugin provides configurable Modbus serial network connections for other SolarNode components.
It uses the PureJavaComm library for serial port access.

Serial Modbus connections work via device-specific serial ports made available on the
host operating system. The name of each port is operating-system specific. Some
common examples are `/dev/ttyS0` (Linux serial port), `/dev/ttyUSB0` (Linux USB serial
port), and `COM1` (Windows serial port).

![Modbus serial settings](docs/modbus-serial-settings.png)

# Component settings

Each device configuration contains the following overall settings:

| Setting            | Description |
|:-------------------|:------------|
| Service Name       | A unique name to identify this network. Other plugins will refer to this name. |
| Serial port        | The operating system-specific serial device name to use, for example `/dev/ttyUSB0` `COM1`. |
| Baud               | The maximum communication speed to use, in bits-per-second. |
| Data bits          | The number of data bits per message. |
| Stop bits          | The number of stop bits per message. |
| Parity             | The serial port parity setting to use. |
| Receive timeout    | The maximum amount of time to wait to receive data, in milliseconds. |
| Flow control in    | The serial port input flow control setting to use. May be one of `none`, `xon/xoff in`, or `rts/cts in`. |
| Flow control out   | The serial port output flow control setting to use. May be one of `none`, `xon/xoff out`, or `rts/cts out`. |
| Keep open          | The number of seconds to keep the connection open for reuse by multiple Modbus transactions. Set to `0` to open and close the connection for each transaction. |
| Max Ports          | The maximum number of serial ports that can be opened at once, or `0` for no limit. |
| Wire Logging       | Enable to support wire-level Modbus message logging. See [below](#logging) for more information. |

# Unknown serial port

In case the configured serial port is not found, the plugin will log the name of all serial ports
it has discovered. For example:

```
12:45:00 WARN  PjcSerialPort - Invalid serial port [/dev/tty.usbserial-FTYS9FWO]; known ports are: [
	cu.BLTH,
	tty.BLTH,
	cu.Bluetooth-Incoming-Port,
	tty.Bluetooth-Incoming-Port
]
```

# Logging

Application logs are generated under the base name `net.solarnetwork.node.io.modbus.nifty` and
low-level Nifty Modbus logs are under `net.solarnetwork.io.modbus`. For wire-level logging, the
logger name will add the description of the serial port, such as
`net.solarnetwork.io.modbus./dev/ttyUSB0 4800 8N1`. Wire-level logs print out the low-level
message bytes send and received, but each log message might contain just a portion of an overall
Modbus message. The wire-level logs look similar to this (which is a partial Modbus message):

```
2022-12-20 22:36:46,395 TRACE twork.io.modbus./dev/ttyUSB_1.3 4800 8N1; [id: 0xa1e2f8bf, L:localhost - R:/dev/ttyUSB_1.3] READ: 5B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 02 00 dd 78 1d                                  |...x.           |
+--------+-------------------------------------------------+----------------+
2022-12-20 22:36:46,397 TRACE twork.io.modbus./dev/ttyUSB_1.3 4800 8N1; [id: 0xa1e2f8bf, L:localhost - R:/dev/ttyUSB_1.3] READ COMPLETE
```

> **Note** that for wire-level log message to appear the **Wire Logging** setting must be enabled
> **and** the `net.solarnetwork.io.modbus.X` logger must have `TRACE` level logging enabled in
> SolarNode's logging configuration.

# TCP Modbus connection

TCP Modbus connections with Nifty Modbus are provided by the core
[Nifty Modbus](../net.solarnetwork.node.io.modbus.nifty) plugin.
