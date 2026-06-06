# SolarNode Mobile Network Setup

This plugin provides a configurable UI within SolarNode, and `SystemConfigure` instruction
support, for resetting/restarting the node's mobile (cellular/4G) network connection.

It is modeled on the [WiFi Setup][wifi] plugin.

# Install

This plugin must be manually installed, along with an OS support package that provides the `mobile`
service script for the `solarcfg` helper. On SolarNodeOS that support is provided by the
[`sn-mobile-mm`][sn-mobile-mm] package, which manages cellular connectivity via ModemManager and
installs the service script to `/usr/share/solarnode/cfg.d/mobile.sh`.

# Use

Once installed, a new **Mobile Network** section appears on the **Settings** page on your SolarNode,
with a **Reset Connection** toggle (the toggle returns to off after the reset is requested).

# `SystemConfigure` instruction support

The `SystemConfigure` instruction topic can be used to get the mobile status and to reset/restart
the connection. The `service` parameter must be `/setup/network/mobile`. The `action` parameter
selects the operation:

| `action`              | Description                                           |
| :-------------------- | :---------------------------------------------------- |
| `status` (or omitted) | Return the current mobile connection status.          |
| `reset`               | Reset (disconnect + reconnect) the mobile connection. |
| `restart`             | Restart the mobile networking service.                |

## Status result

For the `status` action the `result` parameter is an object:

| Property  | Type           | Description                                                         |
| :-------- | :------------- | :------------------------------------------------------------------ |
| `present` | `boolean`      | `true` if a mobile modem is available (and so a reset is possible). |
| `active`  | `boolean`      | `true` if the mobile connection is currently active.                |
| `info`    | `List<String>` | Optional detail lines (operator, access technology, signal, state). |

A client can query `status` first and only offer/perform a `reset` when `present` is `true`, to
avoid attempting a reset on a node that has no mobile modem. If the plugin is not installed on the
node at all, the `SystemConfigure` instruction returns a not-found status instead of a result.

Example `result`, expressed in JSON:

```json
{
	"present": true,
	"active": false,
	"info": [
		"operator: Spark NZ",
		"access: lte",
		"signal: 64%",
		"state: registered"
	]
}
```

## STOMP usage

Via the SolarNode STOMP setup server, after authenticating, send a frame whose `destination` is the
service name. For example, to check whether a mobile connection is available before resetting:

```
SEND
destination:/setup/network/mobile
action:status

^@
```

and to reset the 4G connection:

```
SEND
destination:/setup/network/mobile
action:reset

^@
```

[wifi]: ../net.solarnetwork.node.setup.wifi/
[sn-mobile-mm]: https://github.com/SolarNetwork/solarnode-os-packages/tree/develop/mobile-mm/debian
