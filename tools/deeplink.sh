#!/bin/sh
# Open an OpenID4VP deeplink on the attached device:
#   tools/deeplink.sh 'haip://?client_id=xxx&request_uri=yyyy'
[ -n "$1" ] || { echo "usage: $0 <haip://... deeplink>" >&2; exit 1; }
# adb re-parses the command through the DEVICE shell, so an unquoted value
# would let &, ;, $(), backticks etc. execute there. Single-quoting the value
# on the device command line makes every byte literal — which is also why an
# embedded single quote cannot be transported safely and is refused instead.
case "$1" in
*\'*)
	echo "refusing single quote in the deeplink (cannot be quoted for the device shell)" >&2
	exit 1
	;;
esac
adb shell am start -W -a android.intent.action.VIEW -d "'$(printf '%s' "$1")'"
