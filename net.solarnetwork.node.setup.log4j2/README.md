# SolarNode log4j2 Support

This plugin provides dynamic log4j2 management support to the SolarNode platform.

## LoggerContext configuration

In order for runtime logger level changes to work, log4j must be configured to use the
[BasicContextSelector][osgi-context]. The easiest way to ensure this is to add the following system
property argument to the JVM startup command:

```
-Dlog4j2.contextSelector=org.apache.logging.log4j.core.selector.BasicContextSelector
```

[osgi-context]: https://logging.apache.org/log4j/2.x/manual/logsep.html#osgi-applications

## SolarNetwork API logger level control

The [SolarUser Instruction API][instr-api] can be used to change the logger level of any logger, by
requesting the SolarNode to perform a [`LoggingSetLevel`][LoggingSetLevel] instruction and passing
`logger` and `level` parameters.

For example, to set the `net.solarnetwork.node.io.serial` logger to `trace` an HTTP `POST` like this
would work:

```
POST /solaruser/api/v1/sec/instr/add/LoggingSetLevel

{"nodeId":123,"params":{"logger":"net.solarnetwork.node.io.serial","level":"trace"}}
```

See the [`LoggingSetLevel`][LoggingSetLevel] documentation for more information.

[instr-api]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-API#queue-instruction
[LoggingSetLevel]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarUser-API-enumerated-types#loggingsetlevel
