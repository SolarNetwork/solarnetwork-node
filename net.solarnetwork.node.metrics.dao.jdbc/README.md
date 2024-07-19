# SolarNode Metrics DB - JDBC

This project provides a JDBC implementation of the core metrics database API.

# DataSource configuration

This plugin requires a JDBC DataSource with a `db` service property of `node-metrics`. The
[example SolarNodeOS service configuration](./example/configuration/services/net.solarnetwork.jdbc.pool.hikari-solarnode-metrics-h2.cfg)
can be used as a starting point (copy that to the `${SOLARNODE_CONF}/services` directory to use).
