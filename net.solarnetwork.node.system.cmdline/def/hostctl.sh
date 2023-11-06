#!/bin/sh
#
# Add/remove aliases from /etc/hosts. Add this script  to the sudoers config
# for the solarnode user, then it can manage hostname aliases. Does not allow
# changing 'localhost'.

HOSTS_FILE=/etc/hosts

do_help () {
	echo "Usage: $0 {list|add|remove} [hostname] [ip]" 1>&2
}

do_list () {
	cat /etc/hosts
}

do_add () {
	local name="$1"
	local ip="$2"
	if [ -z "$ip" ]; then
		echo 'Hostname and ip arguments required.' 1>&2
		do_help
		exit 1
	elif [ "$name" = "localhost" ]; then
		echo 'localhost may not be changed.' 1>&2
		exit 2
	fi
	local ip_match=$(grep -w "$ip" "$HOSTS_FILE")
	local name_match=$(grep -E -m 1 "[[:space:]]$name([[:space:]]|$)" "$HOSTS_FILE")
	if [ -z "$ip_match" ]; then
		if [ -n "$name_match" ]; then
			do_remove "$name"
		fi
		echo "$ip $name" >> "$HOSTS_FILE"
	else
		# ip found; check if hostname present
		local name_match=$(echo "$ip_match" |grep -w "$name")
		if [ -z "$name_match" ]; then
			sed -i -e "/$ip/s/$/ $name/" "$HOSTS_FILE"
		fi
	fi
}

do_remove() {
	local name="$1"
	if [ "$name" = "localhost" ]; then
		echo 'localhost may not be changed.' 1>&2
		exit 2
	fi
	# loop to remove all matches of hostname
	grep -E "[[:space:]]$name([[:space:]]|$)" "$HOSTS_FILE" |while read -r line; do
		# TODO: this removes entire line, even if another hostname using this IP;
		#       might be nice to remove just hostname, but then need to remove entire
		#       line if only IP remains
		sed -i -e "/$line/d" "$HOSTS_FILE"
	done
}

# Verify command arguments
ACTION="$1"

if [ -z "$ACTION" ]; then
	do_help
	exit 1
fi

shift

# Parse command line parameters.
case $ACTION in
	l|list) do_list;;

	a|add) do_add "$@";;

	r|remove) do_remove "$@";;

	*)
		do_help
		exit 1
		;;
esac

exit 0

