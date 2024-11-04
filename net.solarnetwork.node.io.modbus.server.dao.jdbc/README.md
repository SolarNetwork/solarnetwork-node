# Modbus Server Persistence (JDBC)

This project provides SolarNode plugin that provides a persistence service for Modbus Server data.
This can be used by other plugins, primarily targeted at the [Modbus Server][modbus-server] plugin.

# Install

The plugin is meant for developers/integrators and can be manually installed and configured.

# Use

This plugin requires a `javax.sql.DataSource` with an OSGi service filter matching `(db=modbus-server)`.
That can be configured via a 
`${SOLARNODE_HOME}/conf/services/net.solarnetwork.jdbc.pool.hikari-solarnode-modbus-server.cfg` file, with
contents similar to this:

```
service.factoryPid = net.solarnetwork.jdbc.pool.hikari
serviceProperty.db = modbus-server
dataSourceFactory.filter = (osgi.jdbc.driver.class=org.h2.Driver)
pingTest.query = VALUES CURRENT_DATE
minimumIdle = 1
maximumPoolSize = 3
factory.exceptionHandlerSupport = true
dataSource.url = jdbc:h2:./var/solarnode-modbus-server-h2
dataSource.user = sa
dataSource.password = 
```

[modbus-server]: ../net.solarnetwork.node.io.modbus.server
