# MQTT Persistence (JDBC)

This project provides SolarNode plugin that provides a persistence service for MQTT messages.
This can be used by other plugins, like the [SolarFlux Upload Service][solarflux-upload] plugin.

# Install

The plugin is meant for developers/integrators and can be manually installed and configured.

# Use

This plugin requires a `javax.sql.DataSource` with an OSGi service filter matching `(db=node-data)`.
That can be configured via a 
`${SOLARNODE_HOME}/conf/services/net.solarnetwork.jdbc.pool.hikari-solarnode-data.cfg` file, with
contents similar to this:

```
service.factoryPid = net.solarnetwork.jdbc.pool.hikari
serviceProperty.db = node-data
dataSourceFactory.filter = (osgi.jdbc.driver.class=org.apache.derby.jdbc.EmbeddedDriver)
pingTest.query = VALUES CURRENT_DATE
minimumIdle = 1
maximumPoolSize = 3
dataSource.url = jdbc:derby:/var/lib/solarnode/var/solarnode-data;create=true;upgrade=true
dataSource.user = solarnode
dataSource.password = solarnode
```

[solarflux-upload]: ../net.solarnetwork.node.upload.flux
