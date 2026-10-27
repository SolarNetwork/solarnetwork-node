# SolarNode Bluetooth Setup

This plugin controls when the SolarNode Bluetooth setup radio (the BLE
peripheral provided by the `solarnode-bluetooth-setup` OS package) is on.

By default the radio is always on. If you turn the _Always On_ setting off, the
radio is off unless one of the following applies:

1. **Schedule**: the _Schedule_ makes the radio available at the current time,
   in the _Time Zone_. For example the rule `,,Mon-Fri,07:00-18:00,1` makes the
   radio available on weekdays during business hours. See
   [Schedule syntax](#schedule-syntax).
2. **Override**: the override operational mode (default `bt-setup`) is active.
   SolarNetwork can enable it, with an expiration, using the
   `EnableOperationalModes` instruction, and a Bluetooth client can enable,
   extend, or end it with the `enable` and `disable` actions described below.
   Use this to turn the radio on outside the schedule, for example to keep a
   setup session going past the end of a window.
3. **Offline fallback**: the node has been unable to reach SolarNetwork for at
   least _Offline Threshold_ minutes. The radio stays on until _Online Grace_
   minutes after the connection comes back. This lets a technician on site
   connect to a node that can't be reached remotely.

When the desired state differs from the actual state the plugin runs
`solarcfg bluetooth enable` or `solarcfg bluetooth disable`. It re-checks the
actual state periodically, so a radio turned on by something else gets turned
off again. The schedule is checked every 15 seconds, so the radio turns on or
off within 15 seconds of a window starting or ending, including after a
restart.

# Settings

| Setting           | Default                                                    | Description                                                                                                                                                   |
|:------------------|:-----------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Status            |                                                            | The current radio state and why it is on, including the schedule state and the time zone in use.                                                              |
| Override For      | `0`                                                        | Enter a number of minutes to turn the radio on for now, regardless of the schedule. Transient: returns to `0` once applied.                                   |
| Always On         | on                                                         | Keep the radio on at all times. Turn off to gate the radio by the schedule, the override, and the offline fallback.                                           |
| Schedule          |                                                            | The availability rules, as tariff schedule CSV. See [Schedule syntax](#schedule-syntax). Ignored while _Always On_ is on. An invalid schedule never applies.  |
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

The schedule uses the same CSV format as the tariff datum filter's schedule,
with a single `Available` column. The first row is a header, and each following
row is a rule with five columns:

| Column      | Values                                                                                             |
|:------------|:---------------------------------------------------------------------------------------------------|
| `Month`     | `1-12`, or names `Jan-Dec`, `January-December`                                                     |
| `Day`       | Day of month, `1-31`                                                                               |
| `Weekday`   | `1-7` with `1` = Monday, or names `Mon-Sun`, `Monday-Sunday`                                       |
| `Time`      | `HH:MM-HH:MM`, or whole hours `H-H` (`0-24`). `24:00` is the end of the day. The end is exclusive. |
| `Available` | `1` to make the radio available, or `0` to keep it off                                             |

An empty cell matches every value. Each of the first four columns can hold a
comma-separated set of inclusive ranges (`a-b`) or single values; quote a cell
that contains a comma, like `"Sat,Sun"`. A single whole hour like `9` means that
hour (`9-10`). Lines starting with `#` are comments; a comment can't follow a
rule on the same line.

Each time range excludes its end: `07:00-18:00` applies from 07:00:00 up to but
not including 18:00:00, and with `"06:00-09:00,16:00-20:00"` the radio is off at
09:00. Ranges must be ascending, so `22:00-06:00`, `Fri-Mon` and `Dec-Jan` are
rejected; write an overnight window as two rules. A time range must not be empty
(`09:00` or `09:00-09:00`) or end after `24:00`. Names are matched in English.

The first rule that matches the current time decides it, so a `0` rule placed
before the others is a blackout. For example the first rule below keeps the
radio off on Christmas Day even though it is a weekday:

```
Month,Day,Weekday,Time,Available
# no setup on Christmas Day
Dec,25,,,0
# weekdays, business hours
,,Mon-Fri,07:00-18:00,1
,,Mon-Fri,"06:00-09:00,16:00-20:00",1
,,"Sat,Sun",08:00-12:00,1
# summer weekdays only
Jun-Aug,,Mon-Fri,07:00-18:00,1
# whole days, 24-26 December
Dec,24-26,,,1
# Friday night into Saturday
,,Fri,22:00-24:00,1
,,Sat,00:00-06:00,1
```

Two time ranges in the same cell that are exactly one minute apart, like
`"06:00-09:00,09:01-12:00"`, are rejected, because they would merge and make
the minute between them available. Put them in separate rules instead.

Rules use local wall-clock time in the _Time Zone_. A rule entirely inside a
daylight saving gap doesn't apply that day, and the repeated hour when clocks go
back applies twice. Many SolarNodeOS images run in UTC, so set _Time Zone_ to
the site's zone; the status shows which zone is in use.

If the schedule or time zone is invalid, the schedule never applies (so the
radio stays off unless something else turns it on). A warning is logged and the
error is shown in the _Status_ and in the `status` instruction result.

## Pushing a schedule from SolarNetwork

The schedule, time zone and _Always On_ settings are ordinary SolarNode
settings, so they can be set on many nodes with the standard `UpdateSetting`
instruction, one `key`/`type`/`value` triplet per setting:

```
POST /solaruser/api/v1/sec/instr/add/UpdateSetting
nodeId=123
&parameters[0].name=key&parameters[0].value=net.solarnetwork.node.setup.bluetooth.BluetoothSetupService
&parameters[1].name=type&parameters[1].value=schedule
&parameters[2].name=value&parameters[2].value=Month,Day,Weekday,Time,Available%0A,,Mon-Fri,07:00-18:00,1
&parameters[3].name=key&parameters[3].value=net.solarnetwork.node.setup.bluetooth.BluetoothSetupService
&parameters[4].name=type&parameters[4].value=timeZone
&parameters[5].name=value&parameters[5].value=Pacific/Auckland
&parameters[6].name=key&parameters[6].value=net.solarnetwork.node.setup.bluetooth.BluetoothSetupService
&parameters[7].name=type&parameters[7].value=alwaysOn
&parameters[8].name=value&parameters[8].value=false
```

The schedule value is CSV, so URL-encode its line breaks as `%0A`.
`UpdateSetting` completes even if the value is invalid, so afterwards send a
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

The plugin handles `SystemConfigure` instructions with `service` set to
`/setup/bluetooth`. Over the Bluetooth STOMP setup connection the destination is
used as the `service` parameter and other headers become instruction
parameters, so an authenticated client can send:

```
SEND
destination:/setup/bluetooth
action:enable
duration:600

^@
```

| `action`  | Parameters           | Effect                                                                                                                                              |
|:----------|:---------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------|
| `status`  |                      | Return the current status (the default when no action is given).                                                                                    |
| `enable`  | `duration` (seconds) | Enable the override mode for `duration` seconds (default _Override Duration_, capped at _Maximum Override_). Calling it again extends the override. |
| `disable` |                      | End the override. Unless the schedule, the offline fallback, or _Always On_ keeps it on, the radio turns off, which drops the client's connection.  |
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
	"schedule": ",,Mon-Fri,7-18,1",
	"scheduleActive": true,
	"scheduleError": null,
	"scheduleNext": 1791000000000,
	"timeZone": "Pacific/Auckland"
}
```

`schedule` is the configured schedule's rules in normalised form, without the
header and separated by `; `. `scheduleNext` is the next time the schedule
changes between available and not available, and `installEnabled` is whether the
peripheral starts at boot, which follows the last enable or disable.

The same instruction can also be queued from SolarNetwork, with
`service=/setup/bluetooth` and `action=enable`.

# Notes

- The offline state is not persisted. If the node restarts while offline the
  radio stays off until the _Offline Threshold_ elapses again.
- Setting `CFG_BT_SETUP_GATED=1` for the OS package leaves the peripheral
  disabled at install, for nodes this plugin will gate from the start. Without
  it the radio is on until this plugin turns it off.
- Each enable or disable also sets whether the peripheral starts at boot. If
  this plugin is removed while the radio is off, it stays off until
  `sudo solarcfg bluetooth enable` is run.
- This plugin requires `solarnode-bluetooth-setup` 4.0 or later, which provides
  the `solarcfg bluetooth` helper.
- The helper runs as root through `sudo`. It reads the adapter name from
  `/etc/solarnode/bluetooth-setup.env` without sourcing the file, because
  `/etc/solarnode` is writable by the SolarNode user.

