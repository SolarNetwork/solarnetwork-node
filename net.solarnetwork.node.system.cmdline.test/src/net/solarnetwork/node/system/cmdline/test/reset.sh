#!/usr/bin/env sh
#
# Test implementation of reset system service testing

APP_ONLY=""

while getopts ":a" opt; do
	case $opt in
		a) APP_ONLY="1";;
		*)
			echo "Unknown option '$OPTARG'." 1>&2
			exit 1
	esac
done

echo "Reset${APP_ONLY}"
