# Numato USB GPIO Control

This SolarNode plugin provides a component that can manage a Numato USB GPIO device as a SolarNode
control. The GPIO addresses can be read as digital on/off values or analog voltages, as supported
by the device. GPIO addresses can also be set.

![Numato USB GPIO settings](docs/solarnode-numato-usb-gpio-control-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Datum** category as **Control Datum Source**. Once installed, a **Numato USB GPIO Control**
component will become available.

# Configuration

Each service configuration contains the following settings:

| Setting                 | Description  |
|:------------------------|:-------------|
| Service Name            | A unique name to identify this data source with. |
| Service Group           | A group name to associate this data source with. |
| Serial Port             | The **Service Name** of the of the serial port component to use. |
| Property Configurations | A list of control configurations. |

> :warning: **Note** that the serial port should have any receive buffering disabled. To do this
> the **Receive threshold** property can be set to `-1`. Additionally the **Port lock** should 
> be disabled, as this plugin will manage the serial port itself.

Each Property Configuration contains the following settings:

| Setting         | Description  |
|:----------------|:-------------|
| Control ID      | The control ID. |
| Property        | The datum property name. |
| Property Type   | The datum property type. |
| Address         | The GPIO address. |
| GPIO Type       | Can be `Digital` for a boolean on/off control value or `Analog` for number control value between 0-1023.  |
| GPIO Direction  | Can be `Input` for a read-only control value or `Output` for a write-only control value. |
| Unit Offset     | An offset to apply to number values to shift their output range. This is the first transform applied. Set to `0` for no offset. |
| Unit Multiplier | A multiplication factor to apply to number values to normalize the value into some other unit. This is the second transform applied, after the Unit Offset. Set to `1` to leave the input unchanged. |
| Multiplier      | A multiplication factor to apply to number values to normalize the value into some other unit. This is the third transform applied, after the Unit Multiplier. Set to `1` to leave the input unchanged. |
| Offset          | An offset to apply to number values to shift their output range. This is the forth transform applied, after the Multiplier. Set to `0` for no offset. |
| Decimal Scale   | A maximum scale (number of digits after the decimal point) to round decimal values to. This is applied after all transforms. Set to `0` to round to whole numbers. Set to `-1` to disable rounding. |

## Property value transform

The **Unit Offset**, **Unit Multiplier**, **Mulitplier**, and **Offset** settings allow you to 
transform the raw device value into a different unit or scale. If we assign each of these settings
to a variable according to the following table:

| Setting | Variable |
|:--------|:---------|
| Unit Offset     | `B` |
| Unit Multiplier | `M` |
| Multiplier      | `m` |
| Offset          | `b` |

Then, for a raw device value `𝛼` we can calculate the transformed output `𝜸` as:

```
𝜸 = (m × (M × (𝛼 + B))) + b
```
