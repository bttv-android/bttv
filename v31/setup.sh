#!/bin/bash
# Prepares the workspace for Twitch 31.5.2 (run inside the builder container, once per Twitch bundle).
# Expects the repo at /repo with the bundle of split apks as /repo/twitch.zip.
set -e
cd /usr/build

[ -f /repo/twitch.zip ] || (echo "/repo/twitch.zip not found" && exit 1)

# the splits stay untouched, only the code of the base apk is changed
rm -rf split work
mkdir split
unzip -q -o /repo/twitch.zip "*.apk" -d split

# compare with https://twitch.tv/.well-known/assetlinks.json
TWITCH_SHA="1338F9B049893CC70B78432A177582F90BD4BC6296EA4ED35BCC7DF59687AC53"
java -jar $UBER_APK_SIGNER_PATH -y --verifySha256 $TWITCH_SHA -a split > verify.log || (cat verify.log && echo "ERROR: not signed by Twitch" && exit 1)

# -r: do not decode the resources, apktool can not rebuild them for this version
java -jar $APKTOOL_PATH d -r -f split/base.apk -o work

# reference for apply_hooks.sh
cd work
printf "build\ndist\nsmali_classes7\n" > .gitignore
git init -q
git add -A
git commit -q -m "base"
git tag base
cd ..

# built on the container's file system, the disk of a Mac is not case sensitive
rm -rf mod
cp -r /repo/mod mod
rm -rf mod/.gradle mod/build mod/*/build mod/local.properties

echo "done, next: apply_hooks.sh and build_mod.sh"
