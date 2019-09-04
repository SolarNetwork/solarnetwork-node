# SolarNode Stabiliti 30C Power Control System Integration

This project provides a SolarNode plugin that provides integration with the [Ideal
Power][ideal-power] Stabiliti 30C Power Control System.

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Control** category as **Stabiliti 30C Power Control**. Once installed, several new **Stabiliti**
components will appear on the **Settings** page on your SolarNode. Click on the **Manage** button to
configure price maps.

# Stabiliti 30C AC Export Manager

The **Stabiliti 30C AC Export Manager** component allows you to configure a control that can
respond to [`ShedLoad`][ShedLoad] instructions by requesting a constant supply of AC energy be
exported to the grid.

When this component starts, it configures the following Stabiliti device settings:

| Setting                | Register | Value    | Description |
|:-----------------------|:---------|:---------|:------------|
| P2 Control Method      | 129      | `0x0001` | Net mode |
| P1 Control Method      | 65       | `0x0402` | Grid power mode |
| P3 Control Method      | 193      | `0x0002` | MPPT mode |
| P1 Real Power Setpoint | 68       | `0x0000` | Zero export |

When a `ShedLoad` instruction with a positive value is handled by this component, it will update the
following settings:

| Setting                | Register | Value    | Description |
|:-----------------------|:---------|:---------|:------------|
| P1 Real Power Setpoint | 68       | _power_  | Set to the power amount requested by the `ShedLoad` instruction. |
| User Start             | 263      | `0x0001` | Start manual mode |

When a `ShedLoad` instruction with a value of `0` is handled by this component, it will update the
following settings:

| Setting                | Register | Value    | Description |
|:-----------------------|:---------|:---------|:------------|
| P1 Real Power Setpoint | 68       | `0x0000` | Zero export |
| User Stop              | 264      | `0x0001` | Stop manual mode |


## Stabiliti 30C AC Export Manager configuration

Each Stabiliti 30C AC Export Manager component contains the following settings:

| Setting            | Description |
|:-------------------|:------------|
| Service Name       | A unique name to associate this component with. |
| Service Group      | A group name to associate this component with. |
| Modbus Connection  | The service name of the Modbus connection to use. |
| Modbus Unit ID     | The ID of the Modbus device to control, from 1 - 255. |
| Sample Maximum Age | The maximum number of **milliseconds** any sampled data may be cached before refreshing it again from the device. |
| Control ID         | The ID to use for the SolarNode control. |


# Stabiliti 30C Watchdog

The **Stabiliti 30C Watchdog** component allows you to configure a watchdog task that manages the
safety count down timer available on the power control system.

![settings](docs/solarnode-stabiliti-30c-watchdog-settings.png)


## Stabiliti 30C Watchdog configuration

Each Stabiliti 30C Watchdog component contains the following settings:

| Setting            | Description |
|:-------------------|:------------|
| Service Name       | A unique name to associate this component with. |
| Service Group      | A group name to associate this component with. |
| Modbus Connection  | The service name of the Modbus connection to use. |
| Modbus Unit ID     | The ID of the Modbus device to control, from 1 - 255. |
| Timeout Seconds    | The number of seconds the Stabiliti should count down from, and if not updated before it reaches zero to shut the system down. This value should be **larger** than the configured **Update Frequency**. |
| Update Frequency   | The frequency at which this component should reset the watchdog timeout value on the Stabiliti to **Timeout Seconds**, essentially resetting the count down timer. This value should be **smaller** than the configured **Timeout Seconds**. |
| Delay Seconds      | The number of seconds to delay the start of the watchdog update task by. |

### Stabiliti 30C Watchdog configuration notes

<dl>
	<dt>Modbus Connection</dt>
	<dd>This is the <i>service name</i> of the Modbus component configured elsewhere
	in SolarNode. You must configure that component with the proper connection settings
	for your Modbus network, configure a unique service name on that component, and then
	enter that same service name here.</dd>
</dl>


[ideal-power]: http://www.idealpower.com/
[ShedLoad]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-API-enumerated-types#shedload
