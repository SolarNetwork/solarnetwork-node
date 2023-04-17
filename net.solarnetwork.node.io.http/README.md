# SolarNode HTTP I/O

This plugin provides components that support other plugins that make HTTP requests.

# Install

The plugin can be installed via the **Plugins** page on your SolarNode. It appears under the **I/O**
category as **HTTP Support**. For more information, see the individual component documentation in
the following sections.

# HTTP Request Customizer

The HTTP Request Customizer service API provides a way to augment and change HTTP requests made
by other plugins. For example the [Basic Authorization](README-Request-Customizer-Auth-Basic.md)
service can add credentials to HTTP requests, using the HTTP Basic scheme.

 * [Basic Authorization](README-Request-Customizer-Auth-Basic.md) - adds HTTP Basic credentials
 * [Customizer Chain](README-Request-Customizer-Chain.md) - execute a list of customizers
