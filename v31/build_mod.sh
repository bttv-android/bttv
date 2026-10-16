#!/bin/bash
# Builds the mod's Java, merges it into the disassembled Twitch code and produces signed split apks
# in /repo/dist/splits (run inside the builder container, after setup.sh and apply_hooks.sh).
#
# The app gets its own package name and name, so it can be installed next to the official app:
#   BTTV_PACKAGE (default tv.twitch.bttvandroid.app), BTTV_NAME (default BTTV)
# The background of the launcher icon gets the color of the mod.
set -e
cd /usr/build

TWITCH_PACKAGE=tv.twitch.android.app
[ -z "$BTTV_PACKAGE" ] && BTTV_PACKAGE=tv.twitch.bttvandroid.app
[ -z "$BTTV_NAME" ] && BTTV_NAME=BTTV
# bttv_mega, the launcher icon needs a color resource of the app with this value
ICON_COLOR=ff00fafa
AAPT2=$(ls $ANDROID_SDK_ROOT/build-tools/*/aapt2 | head -1)
ZIPALIGN=$(ls $ANDROID_SDK_ROOT/build-tools/*/zipalign | head -1)
echo "BTTV_PACKAGE: $BTTV_PACKAGE"
echo "BTTV_NAME: $BTTV_NAME"

echo "== sync mod sources"
rm -rf mod/app/src mod/twitch/src mod/consumer/src
cp -r /repo/mod/app/src mod/app/src
cp -r /repo/mod/twitch/src mod/twitch/src
cp -r /repo/mod/consumer/src mod/consumer/src

echo "== gradle"
(cd mod && bash gradlew assemble --console=plain -q)

echo "== dex -> smali"
rm -rf modsmali && mkdir modsmali
unzip -q -o mod/consumer/build/outputs/apk/release/consumer-release-unsigned.apk classes.dex -d modsmali
java -jar $BAKSMALI_PATH d -o modsmali/out modsmali/classes.dex
find modsmali/out/bttv -type f -exec sed -i -e 's/BTTVDefaultConstructorMarker/DefaultConstructorMarker/g' {} \;

# only the mod's own package, as its own dex file
rm -rf work/smali_classes7
mkdir -p work/smali_classes7
cp -r modsmali/out/bttv work/smali_classes7/bttv

echo "== rename package"
rm -rf out && mkdir -p out/s out/tool
javac -d out/tool /repo/v31/RenamePackage.java
ICON=res/mipmap-anydpi-v26/ic_launcher.xml
(cd work && git checkout -q base -- AndroidManifest.xml resources.arsc $ICON)
ICON_COLOR_ID=$($AAPT2 dump resources split/base.apk | grep -B1 -m1 -E "^ +\(\) #$ICON_COLOR\$" | grep -oE "0x7f[0-9a-f]+" | head -1)
[ -z "$ICON_COLOR_ID" ] && echo "no color resource with the value $ICON_COLOR" && exit 1
java -cp out/tool RenamePackage icon work/$ICON ${ICON_COLOR_ID#0x}
java -cp out/tool RenamePackage manifest work/AndroidManifest.xml $TWITCH_PACKAGE $BTTV_PACKAGE "$BTTV_NAME"
java -cp out/tool RenamePackage arsc work/resources.arsc $TWITCH_PACKAGE $BTTV_PACKAGE

echo "== build apk"
java -Xmx5g -jar $APKTOOL_PATH b work -o out/base.apk
java -jar $UBER_APK_SIGNER_PATH -a out/base.apk > out/sign.log
# the splits need the same package name and have to be signed with the same key as the base apk
for f in split/split_config.*.apk; do
  java -cp out/tool RenamePackage apk $f out/renamed.apk $TWITCH_PACKAGE $BTTV_PACKAGE > /dev/null
  # native libraries are loaded from the apk, they have to stay aligned to pages
  $ZIPALIGN -p -f 4 out/renamed.apk out/s/$(basename $f)
done
rm out/renamed.apk
java -jar $UBER_APK_SIGNER_PATH -a out/s --skipZipAlign --allowResign >> out/sign.log
grep -E "Successfully processed" out/sign.log

rm -rf /repo/dist/splits && mkdir -p /repo/dist/splits
cp out/base-aligned-debugSigned.apk /repo/dist/splits/base.apk
for f in out/s/*-debugSigned.apk; do cp $f /repo/dist/splits/$(basename $f | sed "s/-debugSigned//"); done
ls -la /repo/dist/splits
