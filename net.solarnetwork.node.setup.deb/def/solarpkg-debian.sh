#!/usr/bin/env sh
#
# Debian helper script for managing Debian packages.

VERBOSE=""

do_help () {
	echo "Usage: $0 [-v] <action>" 1>&2
	echo 1>&2
	echo "<action> is one of: install, remove, set-conf" 1>&2
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

shift 1

export DEBIAN_FRONTEND=noninteractive

# install a package, and return a list of files installed
pkg_install () {
	local pkg="$1"
	if [ -z "$pkg" ]; then
		echo "Must provide path to package to install."  1>&2
		exit 1
	fi
	
	# assume dpkg -c output lines look like:
	#
	# drwxr-xr-x 0/0               0 2019-05-20 18:33 ./usr/share/solarnode/bin/
	# -rwxr-xr-x 0/0            2167 2019-05-20 18:29 ./usr/share/solarnode/bin/solarstat.sh
	#
	# We thus extract the 6th field, omitting paths that end in '/' and stripping the leading '.'
	
	sudo dpkg -i --force-confdef --force-confold "$pkg" >/dev/null \
		&& dpkg -c "$pkg" |awk '$6 !~ "/$" {print substr($6,2)}'
}

pkg_remove () {	
	local pkg="$1"
	if [ -z "$pkg" ]; then
		echo "Must provide name of package to remove."  1>&2
		exit 1
	fi
	if dpkg -s "$pkg" >/dev/null 2>&1; then
		sudo apt-get -qy remove --purge >/dev/null "$pkg"
	fi
}


case $ACTION in
	install) pkg_install "$@";;
	
	remove) pkg_remove "$@";;

	*)
		echo "Action '${ACTION}' not supported." 1>&2
		echo 1>&2
		do_help
		exit 1
		;;
esac
