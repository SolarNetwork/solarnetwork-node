#!/usr/bin/env bash
#
# Linux helper script for SolarNode OS statistic gathering.

VERBOSE=""

do_help () {
	echo "Usage: $0 <action>" 1>&2
	echo 1>&2
	echo "<action> is one of: cpu-use, fs-use, mem-use, net-traffic, sys-load, sys-up" 1>&2
}

while getopts ":v" opt; do
	case $opt in
		v) VERBOSE="1";;

		\?) do_help; exit 1;;
	esac
done

shift $(($OPTIND - 1))

ACTION="$1"

if [ $# -lt 1 ]; then
	echo "Must provide action, use -? for help."  1>&2
	exit 1
fi

# print out average CPU utilization for current hour reported by sar, e.g.
#
#  0.42,0.06,99.51
#  0.40,0.07,99.52
#  0.48,0.06,99.45
#
do_cpu_use_inst () {
	echo 'user,system,idle'
	# take 3 vmstat samples, but skip the first to ignore cost of launching this command with awk
	vmstat -n 1 3 |awk 'BEGIN { c=0; u=0; s=0; i=0 } NR > 3 { c++; u+=$13; s+=$14; i+=$15 } END { print u/c","s/c","i/c }'
}

# print out file system use, e.g.
#
#   /run,403224,12256,4
#   /,19880876,12386876,66
#
do_fs_use_inst () {
	echo 'mount,size-kb,used-kb,used-percent'
	BLOCKSIZE=1024 df |awk 'NR > 1 { sub("%","",$5); print $6","$2","$3","$5 }'
}

# print out memory use, e.g.
#
#   total-kb,avail-kb
#   1000184,756380
#
do_mem_use_inst () {
	echo "total-kb,avail-kb"
	free |awk '/^Mem/ { print $2","$7 }'
}

# print out network device accumulated traffic, e.g.
#
#   wlan0 373064 110099 5086 549
#   lo 3393 3393 29 29
#   eth0 1770391 1287753 7329 6542
#   usb0 0 0 0 0
#
do_net_traffic_acc () {
	echo 'device,bytes-in,bytes-out,packets-in,packets-out'
	awk '/:/ { n = sub(":","",$1); print($n","$2","$10","$3","$11) }' /proc/net/dev
}

# print out system load information, e.g.
#
#   1.52,1.67,1.65
#
do_sys_load_inst () {
	echo '1min,5min,15min'
	awk '{ print $1","$2","$3 }' /proc/loadavg
}

# print out system uptime seconds, e.g.
#
# 123122.21
#
do_sys_uptime_acc () {
	echo 'up-sec'
	cut -f1 -d' ' /proc/uptime
}

case $ACTION in
	cpu-use) do_cpu_use_inst;;

	fs-use) do_fs_use_inst "$@";;

	mem-use) do_mem_use_inst;;

	net-traffic) do_net_traffic_acc;;

	sys-load) do_sys_load_inst;;

	sys-up) do_sys_uptime_acc;;

	*)
		echo "Action '${ACTION}' not supported." 1>&2
		echo 1>&2
		do_help
		exit 1
		;;
esac
