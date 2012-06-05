SolarNode Framework Packages
============================

This folder contains some packages of the SolarNode platform, which can
be used to deploy new nodes.

The **base-equinox-node.tgz** package contains the core platform based
on the Eclipse Equinox OSGi container. Expand this package into the
*solar* user's home directory to create the base platform.

Other packages included here add additional features to the base node.
They must be expanded from within the `app` directory created by the
base package. That is, the following steps illustrate adding an extra
package to the base platform:

		tar xzf /tmp/base-equinox-node.tgz
		cd app
		tar xzf /tmp/centameter-consumption-node-bundles.tgz
  
System Startup Scripts
----------------------

This folder also contains system startup scripts that can be used as
a starting point for starting the node as a service. These files are
shell scripts, named `*.sh`. Be sure to use the correct startup script
for the base node's OSGi container you've deployed. For example if the
base node package name contains `equinox` then the startup script name
should, too.
