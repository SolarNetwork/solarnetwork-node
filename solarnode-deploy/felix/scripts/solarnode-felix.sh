#!/bin/sh
### BEGIN INIT INFO
# Provides:          solarnode
# Required-Start:    $remote_fs
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: SolarNode daemon
# Description:       The SolarNode daemon is for collecting energy related
#                    data and uploading that to the central SolarNet service,
#                    as well as intelligently responding to "smart grid"
#                    events such as load shedding.
### END INIT INFO
# 
# SysV init script for the SolarNode daemon for Apache Felix. Designed
# to be run as an /etc/init.d service by the root user.
#
# chkconfig: 3456 99 01
# description: Control the SolarNode Felix server
#
# Set JAVA_HOME to the path to your JDK or JRE.
# 
# Set SOLARNODE_HOME to the directory that contains the following:
# 
# + <SOLARNODE_HOME>/
# |
# +--+ felix/                     <-- Felix install dir
# |  |
# |  +--+ bin/
# |     |
# |     +-- felix.jar
# |
# +--+ conf/                      <-- configuration
# |  |
# |  +-- config.properties        <-- main Felix configuration
# |  +-- system.properties        <-- custom system properties
# |  +-- solarnode.xml
# |
# +--+ app/                      
#    |
#    +--+ boot/                   <-- OSGi bootstrap bundles
#    +--+ main/                   <-- SolarNode OSGi bundles
#
#
# Set PID_FILE to the path to the same path as specified in 
# solarnode.properties for the node.pidfile setting.
# 
# Set RUNAS to the name of the user to run the process as. The script
# will use "su" to run the node as this user, in the background.
# 
# Modify the APP_ARGS and JVM_ARGS variables as necessary.

JAVA_HOME=/usr
SOLARNODE_HOME=/home/solar
LOG_DIR=${SOLARNODE_HOME}/var/log
FELIX_CACHE=${SOLARNODE_HOME}/var/felix-cache
FELIX_HOME=${SOLARNODE_HOME}/felix
PID_FILE=${SOLARNODE_HOME}/var/solarnode-felix.pid
APP_ARGS="-Dfelix.config.properties=file:${SOLARNODE_HOME}/conf/config.properties -Dfelix.system.properties=file:${SOLARNODE_HOME}/conf/system.properties -Dsn.home=${SOLARNODE_HOME}"
JVM_ARGS="-Xmx48m"
#JVM_ARGS="-Dcom.sun.management.jmxremote"
#JVM_ARGS="Xdebug -Xnoagent -Xrunjdwp:server=y,transport=dt_socket,address=9142,suspend=y"

RUNAS=solar

START_CMD="${JAVA_HOME}/bin/java ${JVM_ARGS} ${APP_ARGS} -jar ${FELIX_HOME}/bin/felix.jar ${FELIX_CACHE}"
START_SLEEP=30s

# Parse command line parameters.
case $1 in
	start)
		echo -n "Starting SolarNode server... "
		if [ ! -e ${LOG_DIR} ]; then
			if [ -z "${RUNAS}" ]; then
				mkdir ${LOG_DIR}
			else
				su -c "mkdir ${LOG_DIR}" $RUNAS
			fi
		fi
		if [ -z "${RUNAS}" ]; then
			${START_CMD} 1>${LOG_DIR}/stdout.log 2>&1 &
		else
			su -l -c "${START_CMD} 1>${LOG_DIR}/stdout.log 2>&1 &" $RUNAS
		fi
		echo -n "sleeping for ${START_SLEEP} to check PID... "
		sleep ${START_SLEEP}
		if [ -e $PID_FILE ]; then
			echo "Running as PID" `cat $PID_FILE`
		else
			echo SolarNode does not appear to be running.
		fi
		;;

	status)
		pid=
		run=
		if [ -e $PID_FILE ]; then
			pid=`cat $PID_FILE`
			run=`ps -p $pid`
		fi
		if [ -n "$run" ]; then
			echo "SolarNode is running (PID $pid)"
		else
			echo SolarNode does not appear to be running.
		fi
		;;
	
	stop)
		pid=
		run=
		if [ -e $PID_FILE ]; then
			pid=`cat $PID_FILE`
			run=`ps -p $pid`
		fi
		if [ -n "$run" ]; then
			echo "Stopping SolarNode $pid"
			kill $pid
		else
			echo SolarNode does not appear to be running.
		fi
		;;

	*)
		# Print help
		echo "Usage: $0 {start|stop|status}" 1>&2
		exit 1
		;;
esac

exit 0

