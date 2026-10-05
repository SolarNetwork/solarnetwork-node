# SolarNode Bluetooth Setup

This plugin controls when the SolarNode Bluetooth setup radio (the BLE
peripheral provided by the `solarnode-bluetooth-setup` OS package) is on.

By default the radio is **always on**. Turning the _Always On_ setting off gates
the radio: it is then **off** unless at least one of the following holds:

1. **Schedule.** The _Schedule_ availability windows apply at the current time
   in the _Time Zone_. For example `Mon-Fri 07:00-18:00` makes the radio
   available on weekdays during business hours. See
   [Schedule syntax](#schedule-syntax) below.
2. **Override.** The override operational mode (default `bt-setup`) is active.
   SolarNetwork can enable this remotely, with an expiration, using the standard
   `EnableOperationalModes` instruction. A Bluetooth client can enable, extend,
   or end it with the `enable` and `disable` actions described below. This is
   how the radio is turned on outside a scheduled window, for example to hold a
   setup session past the end of a window.
3. **Offline fallback.** The node has been unable to reach SolarNetwork for at
   least the _Offline Threshold_ number of minutes. The radio stays on until the
   _Online Grace_ number of minutes after the connection is restored. This
   covers the case where a technician visits a site because the node is offline,
   when no remote trigger can reach it.

Whenever the desired state differs from the actual state the plugin runs
`solarcfg bluetooth enable` or `solarcfg bluetooth disable`. It also checks the
actual state periodically, so a radio left on by something else is turned off.
The schedule is checked every 15 seconds, so the radio follows a window edge
within that time, and a node that restarts inside a window turns the radio on
from its clock alone.

# Settings

| Setting           | Default                                                    | Description                                                                                                                                                   |
| ----------------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Status            |                                                            | The current radio state and why it is on, including the schedule state and the effective time zone.                                                           |
| Override For      | `0`                                                        | Enter a number of minutes to turn the radio on for now, regardless of the schedule. Transient: returns to `0` once applied.                                   |
| Always On         | on                                                         | Keep the radio on at all times. Turn off to gate the radio by the schedule, the override, and the offline fallback.                                           |
| Schedule          |                                                            | The availability windows, one per line. See [Schedule syntax](#schedule-syntax). Ignored while _Always On_ is on. An invalid schedule never applies.          |
| Time Zone         |                                                            | The IANA zone ID to evaluate the schedule in, such as `Pacific/Auckland`. Empty means the node system zone. An invalid zone means the schedule never applies. |
| Override Mode     | `bt-setup`                                                 | The operational mode that turns the radio on while it is active, regardless of the schedule.                                                                  |
| Override Duration | `1800`                                                     | Seconds to turn the radio on for when an `enable` request has no `duration`.                                                                                  |
| Maximum Override  | `14400`                                                    | The longest a single `enable` request can turn the radio on for, in seconds. `0` for no limit.                                                                |
| Offline Trigger   | on                                                         | Turn the radio on automatically while SolarNetwork is unreachable.                                                                                            |
| Offline Threshold | `15`                                                       | Minutes SolarNetwork must be unreachable before the radio turns on.                                                                                           |
| Online Grace      | `10`                                                       | Minutes to keep the radio on after SolarNetwork is reachable again.                                                                                           |
| Check Interval    | `60`                                                       | Seconds between SolarNetwork connectivity checks. `0` disables them. Does not affect the schedule check.                                                      |
| Ping Test ID      | `net\.solarnetwork\.node\.upload\.mqtt\.MqttUploadService` | Regular expression matching the ID of the ping test that indicates SolarNetwork connectivity. If no test matches, the offline trigger never fires.            |
| Command           | `{sn.home}/bin/solarcfg`                                   | The `solarcfg` command to execute.                                                                                                                            |
| Command Timeout   | `60`                                                       | Seconds to wait for the `solarcfg` command before giving up on it. A helper that exceeds this is killed and the radio state is left unchanged.                |

The connectivity check uses the node's own system health ping tests, so by
default it reflects whether the SolarIn/MQTT connection is established. Nodes
that upload over HTTP instead can point _Ping Test ID_ at a different test, or
configure an HTTP Ping control and use its ID.

# Schedule syntax

The schedule is a list of availability windows, one per line (or separated by
`;`). Blank lines are ignored and `#` starts a comment that runs to the end of
the line. Each window has up to four whitespace-separated columns, in the same
order as the tariff datum filter's schedule:

| Columns given | Meaning                              |
| ------------- | ------------------------------------ |
| 1             | `<times>`                            |
| 2             | `<weekdays> <times>`                 |
| 3             | `<days> <weekdays> <times>`          |
| 4             | `<months> <days> <weekdays> <times>` |

Leading columns can be left out, and a column of `*` matches every value. Each
column is a comma-separated set of inclusive ranges (`a-b`), or single values
for the month, day and weekday columns; a time must always be a range:

| Column     | Values                                                                                          |
| ---------- | ----------------------------------------------------------------------------------------------- |
| `months`   | `1-12`, or names `Jan-Dec`, `January-December`                                                  |
| `days`     | Day of month, `1-31`                                                                            |
| `weekdays` | `1-7` with `1` = Monday, or names `Mon-Sun`, `Monday-Sunday`                                    |
| `times`    | `HH:MM-HH:MM`, or whole hours `H-H` (`0-24`). `24:00` is the end of the day. **End exclusive.** |

Time ranges are inclusive-exclusive per range: `07:00-18:00` applies from
07:00:00 up to but not including 18:00:00, and in `06:00-09:00,16:00-20:00` the
radio is off at 09:00. Ranges must be ascending: `22:00-06:00` and `Fri-Mon` are
rejected, so an overnight window is written as two windows. Names are matched in
English first, then in the node's default language.

```
Mon-Fri 07:00-18:00                              # weekdays, business hours
Mon-Fri 06:00-09:00,16:00-20:00; Sat,Sun 08:00-12:00
Jun-Aug * Mon-Fri 07:00-18:00                    # summer weekdays only
Dec 24-26 * *                                    # whole days, 24-26 December
Fri 22:00-24:00                                  # Friday night ...
Sat 00:00-06:00                                  # ... into Saturday
```

Windows are evaluated as wall-clock time in the _Time Zone_. A window that falls
entirely inside a daylight-saving gap never applies that day, and the repeated
hour of a fall-back applies twice. Many SolarNodeOS images run in UTC, so set
_Time Zone_ to the site's zone; the status shows the zone in effect.

If the schedule or the time zone cannot be parsed the schedule never applies
(the radio fails **closed**), a warning is logged, and the reason is shown in
the _Status_ and in the `status` instruction result.

## Pushing a schedule from SolarNetwork

The schedule, time zone and _Always On_ settings are ordinary SolarNode
settings, so they can be set on many nodes with the standard `UpdateSetting`
instruction, one `key`/`type`/`value` triplet per setting:

```
POST /solaruser/api/v1/sec/instr/add/UpdateSetting
nodeId=123
&parameters[0].name=key&parameters[0].value=net.solarnetwork.node.setup.bluetooth.BluetoothSetupConfiguration
&parameters[1].name=type&parameters[1].value=schedule
&parameters[2].name=value&parameters[2].value=Mon-Fri 07:00-18:00
&parameters[3].name=key&parameters[3].value=net.solarnetwork.node.setup.bluetooth.BluetoothSetupConfiguration
&parameters[4].name=type&parameters[4].value=timeZone
&parameters[5].name=value&parameters[5].value=Pacific/Auckland
&parameters[6].name=key&parameters[6].value=net.solarnetwork.node.setup.bluetooth.BluetoothSetupConfiguration
&parameters[7].name=type&parameters[7].value=alwaysOn
&parameters[8].name=value&parameters[8].value=false
```

`UpdateSetting` completes whether or not the value is valid, so follow it with a
`SystemConfigure` instruction with `service=/setup/bluetooth` and
`action=status` and check that `scheduleError` is `null`.

# Overriding the schedule

## From SolarNetwork

Queue an `EnableOperationalModes` instruction for the node with the override
mode name and an expiration (milliseconds since the epoch). For example, with
the SolarUser API:

```
POST /solaruser/api/v1/sec/instr/add/EnableOperationalModes
nodeId=123&parameters[0].name=OpMode&parameters[0].value=bt-setup&parameters[1].name=Expiration&parameters[1].value=1791000000000
```

The node persists the mode across restarts and turns it off automatically at the
expiration. A `DisableOperationalModes` instruction with `OpMode=bt-setup` ends
the override early.

## Over Bluetooth (STOMP)

This plugin handles the `SystemConfigure` instruction topic when the `service`
parameter is `/setup/bluetooth`. Over the Bluetooth STOMP setup connection the
destination becomes the `service` parameter and any other headers become
instruction parameters, so an authenticated client can send:

```
SEND
destination:/setup/bluetooth
action:enable
duration:600

^@
```

| `action`  | Parameters           | Effect                                                                                                                                              |
| --------- | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `status`  |                      | Return the current status (the default when no action is given).                                                                                    |
| `enable`  | `duration` (seconds) | Enable the override mode for `duration` seconds (default _Override Duration_, capped at _Maximum Override_). Calling it again extends the override. |
| `disable` |                      | End the override. The radio turns off unless the schedule, the offline fallback, or _Always On_ holds, which **drops the client's own connection**. |
| `restart` |                      | Restart the Bluetooth peripheral service if it is running, then reconcile. It never turns a gated-off radio on.                                     |

The `result` of every action is a status object:

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
	"schedule": "* * Mon-Fri 07:00-18:00",
	"scheduleActive": true,
	"scheduleError": null,
	"scheduleNext": 1791000000000,
	"timeZone": "Pacific/Auckland"
}
```

`schedule` is the configured schedule in canonical form, `scheduleNext` is when
the schedule next starts or stops allowing the radio on, and `installEnabled` is
whether the OS will start the peripheral at boot, which the helper sets to the
last enable or disable decision.

The same instruction can be queued from SolarNetwork as well, with
`service=/setup/bluetooth` and `action=enable`.

# Notes

- The offline state is not persisted. If the node restarts while offline the
  radio stays off until the _Offline Threshold_ elapses again.
- The OS package's `CFG_BT_SETUP_GATED=1` option leaves the peripheral disabled
  at install, for nodes that will be gated by this plugin from the start.
  Without it the OS package keeps the radio on until this plugin turns it off.
- The OS helper records each enable or disable as the peripheral's boot state.
  Removing this plugin while the radio is scheduled off leaves it off until
  `sudo solarcfg bluetooth enable` is run.
- This plugin requires `solarnode-bluetooth-setup` 4.0 or later, which provides
  the `solarcfg bluetooth` helper.
- The helper runs as root via `sudo`. It reads the adapter name from
  `/etc/solarnode/bluetooth-setup.env` as data and never sources that file,
  because `/etc/solarnode` is writable by the SolarNode user.
