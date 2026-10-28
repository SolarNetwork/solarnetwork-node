# SolarNode Bluetooth Setup

This plugin controls when the SolarNode Bluetooth setup radio is on. The radio is the BLE peripheral
provided by the `solarnode-bluetooth-setup` OS package (4.0 or later).

By default the radio is always on. With _Always On_ turned off, the radio is on only while at least
one of these applies:

1. **Schedule**: the [schedule](#schedule) allows it at the current time.
2. **Override**: the _Override Mode_ operational mode (`bt-setup`) is active. See
   [Overriding the schedule](#overriding-the-schedule).
3. **Offline**: the _Offline Mode_ operational mode (`net-offline`) has been active for _Offline
   Threshold_ minutes. The radio stays on until _Online Grace_ minutes after the mode clears. See
   [Offline detection](#offline-detection).

The plugin turns the radio on and off with `solarcfg bluetooth enable` and `disable`. It checks the
schedule every 15 seconds, and checks the actual radio state every _Check Interval_ seconds, so a
radio turned on by something else is turned off again.

# Settings

| Setting           | Default                  | Description                                                                                              |
|:------------------|:-------------------------|:---------------------------------------------------------------------------------------------------------|
| Status            |                          | The current radio state and why it is on.                                                                |
| Override For      | `0`                      | Minutes to turn the radio on for, starting now, at most _Maximum Override_. Returns to `0` once applied. |
| Always On         | on                       | Keep the radio on at all times.                                                                          |
| Schedule          |                          | The availability rules. See [Schedule](#schedule).                                                       |
| Time Zone         |                          | The zone to evaluate the schedule in, such as `Pacific/Auckland`. Empty for the system zone.             |
| Metadata Service  | `Node Metadata Service`  | The metadata service to read the schedule from, or the _Service Name_ of a User Metadata Service.        |
| Metadata Path     |                          | The metadata path to read the schedule from, such as `/pm/bluetooth/schedule`. Empty to use _Schedule_.  |
| Override Mode     | `bt-setup`               | The operational mode that turns the radio on while it is active.                                         |
| Override Duration | `1800`                   | Seconds an `enable` request lasts when it has no `duration`.                                             |
| Maximum Override  | `14400`                  | The longest an `enable` request can last, in seconds. `0` for no limit.                                  |
| Offline Trigger   | on                       | Turn the radio on while the network is offline.                                                          |
| Offline Threshold | `15`                     | Minutes _Offline Mode_ must be active before the radio turns on.                                         |
| Online Grace      | `10`                     | Minutes to keep the radio on after _Offline Mode_ clears.                                                |
| Check Interval    | `60`                     | Seconds between checks of _Offline Mode_ and the radio state. `0` disables them and the offline trigger. |
| Offline Mode      | `net-offline`            | The operational mode that means the network is offline. Empty disables the offline trigger.              |
| Command           | `{sn.home}/bin/solarcfg` | The `solarcfg` command.                                                                                  |
| Command Timeout   | `60`                     | Seconds to wait for the command before killing it.                                                       |

## Offline detection

The plugin doesn't test the network itself. It watches _Offline Mode_, which an **HTTP Ping**
control (`net.solarnetwork.node.control.ping`) enables while it can't reach its URL. The
`solarnode-config-ping` package adds one that pings `http://www.google.com` every 5 minutes. The
control's _Failure Op Mode_ must match _Offline Mode_. Without such a control the offline trigger
never fires.

"Offline" therefore means the ping URL can't be reached, not that SolarNetwork can't be. With the
default settings the radio turns on about 15 to 21 minutes into an outage.

The mode has no expiration. If the ping control is removed while the mode is active, the radio stays
on until the mode is cleared with a `DisableOperationalModes` instruction (`OpMode=net-offline`) or
_Offline Trigger_ is turned off. The offline timer isn't persisted, so after a restart the threshold
starts again.

# Schedule

The schedule uses the tariff schedule CSV format with a single `Available` column: a header row,
then one rule per line.

| Column      | Values                                                                           |
|:------------|:---------------------------------------------------------------------------------|
| `Month`     | `1-12` or `Jan-Dec`                                                              |
| `Day`       | `1-31`                                                                           |
| `Weekday`   | `1-7` (Monday is `1`) or `Mon-Sun`                                               |
| `Time`      | `HH:MM-HH:MM`, or whole hours `H-H`. The end is exclusive; `24:00` ends the day. |
| `Available` | `1` for available, `0` for off                                                   |

- An empty cell matches anything. A cell can hold several values or ranges, quoted: `"Sat,Sun"`.
- Ranges must be ascending, so write an overnight window as two rules.
- The first rule that matches decides, so a `0` rule first acts as a blackout.
- Lines starting with `#` are comments.
- Times are local to _Time Zone_. Many SolarNodeOS images run in UTC, so set it to the site's zone.
- An invalid schedule or time zone never applies. The error is shown in _Status_.

```
Month,Day,Weekday,Time,Available
# no setup on Christmas Day
Dec,25,,,0
,,Mon-Fri,07:00-18:00,1
,,"Sat,Sun",08:00-12:00,1
# Friday night into Saturday
,,Fri,22:00-24:00,1
,,Sat,00:00-06:00,1
```

## Schedule from metadata

To manage the schedule from SolarNetwork, set _Metadata Path_. The value is the same CSV as a
string, or an array of rows with the header first:

```json
{"pm":{"bluetooth":{"schedule":[
  ["Month","Day","Weekday","Time","Available"],
  ["","","Mon-Fri","07:00-18:00",1]
]}}}
```

- A schedule loaded from metadata replaces the _Schedule_ setting. If there is nothing at the path,
  the setting applies.
- Until metadata first loads, the _Schedule_ setting applies, so leave it empty or no wider than the
  metadata schedule.
- If the metadata can't be loaded, the last result is kept and the read is retried after 5 minutes.
- An invalid metadata schedule never applies, and the setting isn't used in its place.

Anyone who can write the metadata path can turn the radio on, and a user metadata path applies to
every node that reads it. Update node metadata with `POST`: `PUT` replaces all of the node's
metadata.

Node metadata is cached for an hour by default. To apply a change sooner, send a `Signal`
instruction with the parameter `net.solarnetwork.node.metadata.json` set to `clear-cache`. The User
Metadata Service doesn't support this signal yet.

# Overriding the schedule

From SolarNetwork, send an `EnableOperationalModes` instruction with `OpMode=bt-setup` and an
`Expiration` in milliseconds since the epoch. A `DisableOperationalModes` instruction ends the
override early.

The plugin also handles `SystemConfigure` instructions with `service` set to `/setup/bluetooth`,
including over the Bluetooth STOMP setup connection:

```
SEND
destination:/setup/bluetooth
action:enable
duration:600

^@
```

| `action`              | Effect                                                                                                |
|:----------------------|:------------------------------------------------------------------------------------------------------|
| `status` (or omitted) | Return the current status.                                                                            |
| `enable`              | Enable the override for `duration` seconds (default _Override Duration_, at most _Maximum Override_). |
| `disable`             | End the override. If nothing else keeps the radio on, it turns off, dropping the client's connection. |
| `restart`             | Restart the Bluetooth peripheral if it is running. This never turns on a radio that should be off.    |

Every action returns the status as its `result`:

```json
{
	"radioActive": true,
	"discoverable": true,
	"powered": true,
	"installEnabled": true,
	"opModeActive": false,
	"opModeExpires": null,
	"offline": false,
	"offlineSince": null,
	"alwaysOn": false,
	"schedule": ",,Mon-Fri,7-18,1",
	"scheduleActive": true,
	"scheduleError": null,
	"scheduleNext": 1791000000000,
	"scheduleSource": "metadata",
	"scheduleMetadataError": null,
	"timeZone": "Pacific/Auckland"
}
```

`scheduleSource` is `metadata` or `setting`. `scheduleNext` is when the schedule next changes.
`installEnabled` is whether the peripheral starts at boot.

# Notes

- Setting `CFG_BT_SETUP_GATED=1` when installing the OS package leaves the peripheral disabled, for
  nodes this plugin will gate from the start.
- Each enable or disable also sets whether the peripheral starts at boot. If this plugin is removed
  while the radio is off, run `sudo solarcfg bluetooth enable` to turn it back on.

