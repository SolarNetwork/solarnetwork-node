# SolarNode LC Tech Hardware Support

This project contains an OSGi bundle that provides supporting tools for working
with LC Tech hardware devices.

## USB Relay

The [UsbRelayUtils](src/net/solarnetwork/node/hw/lctech/relay/UsbRelayUtils.java) class
provides support for USB relays via a serial port connection. The serial settings are
typically:

| Setting   | Value |
|:----------|:------|
| Baud      | 9600 |
| Data bits | 8 |
| Stop bits | 1 |
| Parity    | None |
