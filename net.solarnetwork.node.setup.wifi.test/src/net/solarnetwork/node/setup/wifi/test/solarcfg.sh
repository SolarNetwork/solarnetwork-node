#!/usr/bin/env sh
#
# Test implementation of solarcfg for WiFi setup testing

if [ $# -lt 2 ]; then
	echo "Must provide service and action arguments."  1>&2
	exit 1
fi

SERVICE="$1"
ACTION="$2"
shift 2

do_configure () {
	local OPTIND opt country ssid password
	while getopts ":c:p:s:" opt; do
		case $opt in
			c) country="${OPTARG}";;
			p) password="${OPTARG}";;
			s) ssid="${OPTARG}";;
			*)
				echo "Unknown configure option '$OPTARG'." 1>&2
				configure_help
				exit 1
		esac
	done
	echo $country
	echo $ssid
	echo ${#password}
}

# settings: print country, ssid on lines to STDOUT
do_settings () {
	echo "NZ"
	echo "Test SSID"
}

# print out connection status, followed by non-link IP addresses
do_status () {
	echo "active"
	echo "127.0.1.1"
	echo "2406:e006:3093:b301:65b1:4726:2af:d721"
}

# restart WiFi
do_restart () {
	echo "Restarted WiFi."
}

case $ACTION in
	configure) do_configure "$@";;

	settings) do_settings "$0";;

	status) do_status "$@";;

	restart) do_restart "$@";;

	*)
		echo "Action '${ACTION}' not supported." 1>&2
		echo 1>&2
		do_help
		exit 1
esac
