# Datum Stream Reactor

This SolarNode plugin provides a component that can monitor one or more datum streams and and issue
an instruction to a control with a value resulting from evaluating an expression.

![Datum Stream Reactor settings](docs/solarnode-datum-stream-reactor-settings.png)

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the
**Control** category as **Datum Stream Reactor**. Once installed, a **Datum Stream Reactor**
component will become available.

# Use

Once configured, this component will monitor the configured datum stream(s) and evaluate the
configured **Expression** on each datum generated in the stream(s). If the expression returns a
non-`null` result, an instruction (defaulting to [`SetControlParameter`][SetControlParameter])
 will be generated for the configured control, with the corresponding value being the expression
 result.

# Configuration

Each service configuration contains the following settings:

| Setting             | Description  |
|:--------------------|:-------------|
| Service Name        | A unique name to identify this data source with. |
| Service Group       | A group name to associate this data source with. |
| Source ID           | A regular expression to match the source ID(s) of the datum stream(s) to monitor. |
| Instruction Topic   | The instruction topic to generate. |
| Control ID          | The control ID to manage. |
| Minimum Value       | An optional minimum value to enforce, applied on number expression evaluation results. |
| Maximum Value       | An optional maximum value to enforce, applied on number expression evaluation results. |
| Expression          | The [expression][expr] to evaluate. See [below](#expressions) for more info. |
| Expression Language | The expression language to write **Expression** in. |

# Expressions

The expression input is a `Datum`. See the [SolarNode Expression][expr] guide for more details. In
addition to the variables and functions documented there, the following additional variables will
be available:

| Variable | Type | Description |
|:----------|:-----|:------------|
| `minValue` | Number | The **Minimum Value** configured on this component. |
| `maxValue` | Number | The **Maximum Value** configured on this component. |

[expr]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Expressions
[SetControlParameter]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-API-enumerated-types#setcontrolparameter
