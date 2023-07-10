# SunSpec Support

This plugin provides a generic API for integrating with SunSpec compatible devices. It relies on
the [Modbus API](../net.solarnetwork.node.io.modbus) for communication.

## Example use

To get started, you create a `ModelData` instance:

```java
ModelDataFactory factory = ModelDataFactory.getInstance();
ModbusConnection conn = getModbusConnection();
ModelData data = factory.getModelData(conn);
```

That will discover all available SunSpec models supported by the device, and load their property values.
Alternatively you can discover all available SunSpec models _without_ loading their property values,
which can be helpful to execute more quickly:

```java
ModelData data = factory.discoverModels(conn);
InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);
data.readModelData(conn, model);
```

## ModelAccessor API

To read a specific SunSpec model, obtain a `ModelAccessor` sub-interface, that provides methods
to read the properties. For example:

```java
InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);
Float current = model.getCurrent();
Float currentPhaseA = model.accessorForPhase(PhaseA).getCurrent();
Float currentPhaseB = model.accessorForPhase(PhaseB).getCurrent();
Float currentPhaseC = model.accessorForPhase(PhaseC).getCurrent();
```

## Supported models

| Model | Accessor API |
|:------|:---------------|
| 101, 102, 103, 111, 112, 113 | net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor |
| 120 | net.solarnetwork.node.hw.sunspec.inverter.InverterNameplateRatingsModelAccessor |
| 121 | net.solarnetwork.node.hw.sunspec.inverter.InverterBasicSettingsModelAccessor |
| 160 | net.solarnetwork.node.hw.sunspec.inverter.InverterMpptExtensionModelAccessor |
| 201, 202, 203, 204, 211, 212, 213, 214 | net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor |
| 302 | net.solarnetwork.node.hw.sunspec.environmental.IrradianceModelAccessor |
| 303 | net.solarnetwork.node.hw.sunspec.environmental.BomTemperatureModelAccessor |
| 304 | net.solarnetwork.node.hw.sunspec.environmental.InclinometerModelAccessor |
| 305 | net.solarnetwork.node.hw.sunspec.environmental.GpsModelAccessor |
| 306 | net.solarnetwork.node.hw.sunspec.environmental.ReferencePointModelAccessor |
| 307 | net.solarnetwork.node.hw.sunspec.environmental.MeteorologicalModelAccessor |
| 308 | net.solarnetwork.node.hw.sunspec.environmental.MiniMeteorologicalModelAccessor |
| 401, 403 | net.solarnetwork.node.hw.sunspec.combiner.StringCombinerModelAccessor |
| 402, 404 | net.solarnetwork.node.hw.sunspec.combiner.StringCombinerAdvancedModelAccessor |
