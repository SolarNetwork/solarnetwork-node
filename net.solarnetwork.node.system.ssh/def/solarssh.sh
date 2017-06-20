#!/usr/bin/env bash
#
# SSH systemd-managed reverse tunnel helper script
#
# This script is designed to work with the solarssh@.service template service unit.
# The instance name of the service is generated from the parameters passed to this
# script (user, host, port, and remote port) so that any number of unique connection
# profiles can be managed.
#
# Typical use looks like this to create the SSH connection:
#
# solarssh.sh -u solar -h sshserver.example.com -r 18932 start
#
# Some assumptions are made for this to work:
#
#  *  sudo is available
#  *  `sudo systemctl start solarssh@${instance}.service` can be run without
#     requesting a password
#  *  `sudo systemctl stop solarssh@${instance}.service` can be run without
#     requesting a password
#  *  SSH has been configured to use password-less login, e.g. through keys
#     and/or certificates

TEST=""
SUSER="solar"
HOST="data.solarnetwork.net"
PORT="22"
RPORT="17777"
VERBOSE=""
ENV_DIR="/run/solar/tmp"
SERVICE_NAME="solarssh"
SERVICE_TEMPLATE="${SERVICE_NAME}@.service"

do_help () {
	echo "Usage: $0 -u <user> -h <host> -p <port> -r <reverse port> <action>" 1>&2
	echo 1>&2
	echo "<action> is one of list, status, start, stop" 1>&2
}

while getopts ":h:p:r:tu:v" opt; do
	case $opt in
		h)
			HOST="${OPTARG}"
			;;

		p)
			PORT="${OPTARG}"
			;;

		r)
			RPORT="${OPTARG}"
			;;

		t)
			TEST="1"
			;;

		u)
			SUSER="${OPTARG}"
			;;

		v)
			VERBOSE="1"
			;;

		\?)
			do_help
			exit 1
	esac
done

shift $(($OPTIND - 1))

ACTION="$1"

if [ $# -lt 1 ]; then
	echo "Must provide action (list, status, start, stop), use -? for help."
	exit 1
fi

if [ -z "${HOST}" -a "${ACTION}" != "list" ]; then
	echo "Destination host not provided (-h), use -? for help."
	exit 1
fi

if [ -z "${PORT}" -a "${ACTION}" != "list" ]; then
	echo "Destination port not provided (-p), use -? for help."
	exit 1
fi

if [ -z "${RPORT}" -a "${ACTION}" != "list" ]; then
	echo "Reverse port not provided (-r), use -? for help."
	exit 1
fi

if [ -z "${SUSER}" -a "${ACTION}" != "list" ]; then
	echo "User not provided (-u), use -? for help."
	exit 1
fi

connDescription="${SUSER}@${HOST}:${PORT} <-- ${RPORT}"
serviceInstanceKey="${SUSER},${HOST},${PORT},${RPORT}"
serviceInstanceName=`systemd-escape --template=${SERVICE_TEMPLATE} ${serviceInstanceKey}`
envFile="${ENV_DIR}/${serviceInstanceKey}.env"

write_env() {
	echo "SSHREMOTE_USER=${SUSER}" >${envFile}
	echo "SSHREMOTE_HOST=${HOST}" >>${envFile}
	echo "SSHREMOTE_PORT=${PORT}" >>${envFile}
	echo "SSHREMOTE_RPORT=${RPORT}" >>${envFile}
	if [ -n "${VERBOSE}" ]; then
		echo "SSH environment ${envFile} generated..."
	fi
}

del_env() {
	rm -f ${envFile}
}

do_start() {
	if systemctl -q is-active ${serviceInstanceName}; then
		echo "SSH connection ${connDescription} is already active." 1>&2
		exit 2
	fi
	write_env
	if [ -n "${VERBOSE}" ]; then
		echo "Starting SSH connection ${connDescription}..."
	fi
	if ! sudo systemctl start ${serviceInstanceName}; then
		echo "Error starting ${connDescription}" 1>&2
		exit 1
	fi
}

do_stop() {
	del_env
	if [ -n "${VERBOSE}" ]; then
		echo "Stopping SSH connection ${connDescription}..."
	fi
	if ! sudo systemctl stop ${serviceInstanceName}; then
		echo "Error stopping ${connDescription}" 1>&2
		exit 1
	fi
}

do_status() {
	service=$1
	if [ -n "${VERBOSE}" ]; then
		systemctl status ${service}
	elif systemctl -q is-active ${service}; then
		echo active
	else
		systemdStatus="$?"
		regex="status=([[:digit:]]+)"
		statusLine=`systemctl status ${service} |grep 'Main PID'`
		statusVal=""
		if [[ $statusLine =~ $regex ]]; then
			statusVal="${BASH_REMATCH[1]}"
		fi
		regex="code=([[:alnum:]]+)"
		codeVal=""
		if [[ $statusLine =~ $regex ]]; then
			codeVal="${BASH_REMATCH[1]}"
		fi
		if [ -n "${statusVal}" ]; then
			echo "error,code=$codeVal,status=$statusVal"
		else
			echo "error,code="`systemctl is-active ${service}`
		fi
	fi
}

do_list() {
	regex="^${SERVICE_NAME}@(.*).service"
	systemctl list-units "${SERVICE_NAME}@*.service" -l |grep ${SERVICE_NAME} |while read -r line ; do
		if [[ $line =~ $regex ]]; then
			service="${BASH_REMATCH[1]}"
			statusVal=`do_status "${SERVICE_NAME}@${service}.service"`
			if [ $statusVal == "active" ]; then
				statusVal=""
			else
				statusVal=",${statusVal}"
			fi
			echo `systemd-escape -u ${service}`${statusVal}
		fi
	done
}


case $ACTION in
	list)
		do_list
		;;

	start)
		do_start
		;;

	status)
		do_status ${serviceInstanceName}
		;;

	stop)
		do_stop
		;;

	*)
		echo "Action '${ACTION}' not supported." 1>&2
		echo 1>&2
		do_help
		exit 1
		;;
esac
