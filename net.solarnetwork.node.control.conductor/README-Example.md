# SolarNode OrchestrateControls example

This document shows an example use case of the `OrchestrateControls` instruction that is supported
by the Control Conductor component provided by this plugin. At a high level, the goal of this
example is to schedule a "demand response event" where a "mode" is adjusted on some device,
several times over time. You can think of this as a sequence of "mode" changes, where the mode
changes from `0` → `3` → `4` → `0` over time.

A Control Conductor component named **HVAC DR** is configured with 3 tasks, that execute at an
**orchestration date** specified in the `OrchestrateControls` instruction:

 1. **1 minute before** the orchestration date, set `hvac/1/mode` to `1`, `2`, or `3` based on an
    expression using the `mode` parameter in the `OrchestrateControls` instruction
 2. **at** the orchestration date, set `hvac/1/mode` to `4`
 3. **X minutes after** the orchestration date, as defined by a `duration` parameter in the
    `OrchestrateControls` instruction, set the `hvac/1/mode` to `0`

 <img alt="Control Conductor settings" src="docs/solarnode-control-conductor-settings.png" width="618">

Here is a concrete example of issuing an `OrchestrateControls` instruction, using the above
configuration:

## 1) Post `OrchestrateControls` instruction

First a `OrchestrateControls` is posted, using the `/instr/add` API, passing in the following
parameters:

| Parameter | Value | Description |
|:----------|:------|:------------|
| `service`  | `HVAC DR` | The **Service Name** of the Control Conductor to trigger. |
| `date`     | `2023-04-05T03:10:00Z` | The **orchestration date** to use; essentially some date in the future. |
| `mode`     | `Heat` | This is provided so task 1's **Value** expression can decide what value to set on the `hvac/1/mode` control. |
| `duration` | `PT1M` | The desired duration used by task 3's **Offset** parameterized value. |

```json
{"nodeId":179, "topic":"OrchestrateControls", "params":{
  "service"  : "HVAC DR",
  "date"     : "2023-04-05T03:10:00Z",
  "mode"     : "Heat",
  "duration" : "PT1M"
}}
```

## 2) SolarNode receives the `OrchestrateControls` instruction

Here are logs from SolarNode, after receiving the  `OrchestrateControls` instruction:

```
15:07:17 INFO  Instruction 11537521 OrchestrateControls received with parameters: {service=HVAC DR, mode=Heat, date=2023-04-05T03:10:00Z, duration=PT1M}
15:07:17 INFO  Posted Instruction 11537521 [OrchestrateControls] acknowledgement status: Executing
15:07:17 INFO  Scheduling Signal instruction on behalf of OrchestrateControls instruction [11537521] task [HVAC DR.1] @ 2023-04-05T03:09:00Z
15:07:17 INFO  Scheduling Signal instruction on behalf of OrchestrateControls instruction [11537521] task [HVAC DR.2] @ 2023-04-05T03:10:00Z
15:07:17 INFO  Scheduling Signal instruction on behalf of OrchestrateControls instruction [11537521] task [HVAC DR.3] @ 2023-04-05T03:11:00Z
15:07:17 INFO  Posted Instruction 11537521 [OrchestrateControls] acknowledgement status: Completed
```

These logs show that SolarNode received the `OrchestrateControls` instruction and then **scheduled**
3 future-dated `Signal` instructions, one for each task configured on the Control Conductor. The
Control Conductor knows how to handle these particular `Signal` instructions. Notice the dates of
each of the `Signal` instructions:

```
...task [HVAC DR.1] @ 2023-04-05T03:09:00Z
...task [HVAC DR.2] @ 2023-04-05T03:10:00Z
...task [HVAC DR.3] @ 2023-04-05T03:11:00Z
```

Those dates are based on the **orchestration date** and `{duration}` parameter provided in the
`OrchestrateControls` instruction:

| Task | Time | Description |
|:-----|:-----|:------------|
| `HVAC DR.1` | `03:09:00Z` | This is 1 minute **before** the orchestration date, because the task's **Offset** was configured as `-PT1M` (minus 1 minute). |
| `HVAC DR.2` | `03:10:00Z` | This is **on** the orchestration date, because the **Offset** was configured as `0` (0 milliseconds). |
| `HVAC DR.3` | `03:11:00Z` | This is 1 minute **after** the orchestration date, because the task's **Offset** was configured as `{duration}` and the `duration` parameter value was specified as `PT1M` (plus 1 minute) in the original `OrchestrateControls` instruction. |

## 3) Orchestration date T-1 minute

At `15:09` local time, which is `03:09Z`, 1 minute before the orchestration date, SolarNode executes
task 1's `Signal` instruction, evaluating the **Value** expression and setting `hvac/1/mode` to `3`:

```
15:09:49 INFO  Executing SetControlParameter instruction for orchestrated task [HVAC DR.1] to set control [hvac/1/mode] to [3]
15:09:49 INFO  Setting hvac/1/mode value to 3
15:09:49 TRACE Wrote 1 Holding values to 2 @ localhost:5552#1: 00 03
15:09:49 INFO  Instruction 1680664015581 [SetControlParameter] state changed to Completed
15:09:49 INFO  Instruction 1680664015578 Signal status changed to Completed
```

## 4) Orchestration date

At `15:10` local time, which is `03:10Z`, SolarNode executes task 2's `Signal` instruction, setting
`hvac/1/mode` to `4`:

```
15:10:49 INFO  Executing SetControlParameter instruction for orchestrated task [HVAC DR.2] to set control [hvac/1/mode] to [4]
15:10:49 INFO  ModbusControl - Setting hvac/1/mode value to 4
15:10:49 TRACE Wrote 1 Holding values to 2 @ localhost:5552#1: 00 04
15:10:49 INFO  Instruction 1680664015582 [SetControlParameter] state changed to Completed
15:10:49 INFO  Instruction 1680664015579 Signal status changed to Completed
```

## 5) Orchestration date T+1 minute

At `15:11` local time, which is `03:11Z`, SolarNode executes task 3's `Signal` instruction, setting
`hvac/1/mode` to `0`, which ends up writing to a Modbus register:

```
15:11:49 INFO  Executing SetControlParameter instruction for orchestrated task [HVAC DR.3] to set control [hvac/1/mode] to [0]
15:11:49 INFO  Setting hvac/1/mode value to 0
15:11:49 TRACE Wrote 1 Holding values to 2 @ localhost:5552#1: 00 00
15:11:49 INFO  Instruction 1680664015583 [SetControlParameter] state changed to Completed
15:11:49 INFO  Instruction 1680664015580 Signal status changed to Completed
```
