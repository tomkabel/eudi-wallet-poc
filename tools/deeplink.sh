#!/bin/sh
# Open an OpenID4VP deeplink on the attached device:
#   tools/deeplink.sh 'haip://?client_id=xxx&request_uri=yyyy'
[ -n "$1" ] || { echo "usage: $0 <haip://... deeplink>" >&2; exit 1; }
# adb re-parses through the device shell, so '&' must be escaped.
adb shell am start -W -a android.intent.action.VIEW -d "$(printf '%s' "$1" | sed 's/&/\\&/g')"
