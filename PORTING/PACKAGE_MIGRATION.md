# Twitch 31 package identity and production builds

The installed package is `tv.twitch.bttvandroid.app`, versionCode `1208000338`.
Internal Twitch/BTTV class names and the stock authentication/User-Agent code
remain unchanged. The manifest patch changes the installed package, nine unique
provider authorities, the app-owned signature permission definition/use, and
the Amazon `amzn` callback host.

Stage 5M-P1 was accepted after all 20 user-reported device tests passed,
including side-by-side installation, login/callback, playback, chat, existing
BTTV features, restart persistence and channel links. Accepted APK:
`dist/archive/BTTV_Stage5M_PackageMigration_Test.apk`, SHA-256
`7D7FB313AD986F424E91FB7B9B8A0345CC7A0F0ECA7D7244C6BE4336AB9A5027`.
All seven DEX files are byte-identical to accepted Stage 5L. Notifications
remain WIP; this migration includes no Firebase or notification repair.

## Select production inputs

The normal patch driver discovers every patch directory. Do not point a
production build at the primary checkout's full `patches` or `mod` directories
while unfinished notification diagnostics are present.

From the repository root, with Python 3 and Git available:

```powershell
python tools/stage_production_input.py .tmp/production-input
```

Use a new destination for each build. The helper copies the current contents
of Git-tracked files (including intentional tracked edits), records HEAD and
per-file SHA-256 in `selection.json`, and excludes only:

- `patches/notifications_diag/`
- `patches/notifications_reason/`
- `mod/app/src/main/java/bttv/notifications/`

All permanent BTTV patches and features remain included. New production files
must be tracked before staging. Existing diagnostic files/reports stay in the
primary checkout. Staging does not copy ignored APKs, caches or build outputs.

## SDK and clean Docker build

The verified builder image is `bttv-builder-splitfix:1.4.9` (APKEditor 1.4.9,
Apktool 2.9.3). Its base SDK contains Build Tools 30.0.0 only; Gradle requires
the exact official `build-tools;30.0.3` and `platforms;android-30` packages.
The existing recovered Docker volume is `bttv-sdk-recovery-30`. On another
machine, populate that volume using the image's Google SDK manager:

```powershell
docker volume create bttv-sdk-recovery-30
docker run --rm --entrypoint /bin/bash `
  --mount type=volume,source=bttv-sdk-recovery-30,target=/sdk-recovery `
  bttv-builder-splitfix:1.4.9 -c 'yes | /opt/cmdline-tools/bin/sdkmanager --sdk_root=/sdk-recovery "build-tools;30.0.3" "platforms;android-30"'
```

Review/accept the SDK license as appropriate. Do not substitute Build Tools
versions or modify application Gradle files to hide a missing dependency.

The build input is the approved dual-ARM `twitch.zip` (not the separately
existing root `twitch.apk`), SHA-256
`63698016AD703C09C724CC0815658CC1EE4724C7018DE14557AEB10C00413889`.
After staging, run the existing normal pipeline in a fresh container:

```powershell
$repo = (Get-Location).Path
$inputPath = (Resolve-Path .tmp/production-input).Path
$outputPath = (New-Item -ItemType Directory .tmp/production-output -ErrorAction Stop).FullName
$containerBuild = @'
set -euo pipefail
cd /usr/build
ln -s /sdk-recovery/build-tools/30.0.3 /opt/android-sdk-linux/build-tools/30.0.3
ln -s /sdk-recovery/platforms /opt/android-sdk-linux/platforms
grep -q '^Pkg.Revision=30.0.3' /opt/android-sdk-linux/build-tools/30.0.3/source.properties
test -s /opt/android-sdk-linux/platforms/android-30/android.jar
test ! -e disass
test ! -e .bttv-cache
test ! -e twitch.apk
test ! -e dist/twitch.apk
test ! -d patches/notifications_diag
test ! -d patches/notifications_reason
test ! -d mod/app/src/main/java/bttv/notifications
for script in initworkspace disassemble buildsource patch build; do
  sed 's/\r$//' "/input/$script" > "$script"
  chmod +x "$script"
done
bash initworkspace </dev/null 2>&1 | tee dist/build.log
test -s dist/twitch.apk
sha256sum dist/twitch.apk
'@
$containerBuild = $containerBuild.Replace("`r", "")
docker run --rm --entrypoint /bin/bash --env DOCKER=true `
  --mount "type=bind,source=$inputPath,target=/input,readonly" `
  --mount "type=bind,source=$inputPath\mod,target=/usr/build/mod" `
  --mount "type=bind,source=$inputPath\patches,target=/usr/build/patches,readonly" `
  --mount "type=bind,source=$inputPath\.all-contributorsrc,target=/usr/build/.all-contributorsrc,readonly" `
  --mount "type=bind,source=$inputPath\bttv.manifest.json,target=/usr/build/bttv.manifest.json,readonly" `
  --mount "type=bind,source=$repo\twitch.zip,target=/usr/build/twitch.zip,readonly" `
  --mount "type=bind,source=$outputPath,target=/usr/build/dist" `
  --mount type=volume,source=bttv-gradle-cache,target=/root/.gradle `
  --mount type=volume,source=bttv-sdk-recovery-30,target=/sdk-recovery,readonly `
  bttv-builder-splitfix:1.4.9 -c $containerBuild
if ($LASTEXITCODE -ne 0) { throw 'Production build failed; inspect build.log' }
```

The mod mount must be read/write for Gradle. Script LF normalization happens
only inside the temporary container. No decoded cache is mounted. `DOCKER=true`
enables the existing exporter to the bind-mounted Windows output directory;
the container can then be removed safely.

Before publishing copies, verify the exported file's package/version, nine
authorities, custom permission, seven DEX/reference limits, absence of
`BTTV31PUSH`, both 39-library ARM inventories, STORED/page alignment, ZIP
integrity, zipalign and v3 signature. Expected signing certificate SHA-256:
`1E08A903AEF9C3A721510B64EC764D01D3D094EB954161B62544EA8F187B5953`.
Run migration tests and applicable Stage 5E–5L regressions against the original
Twitch 31 decode. Preserve accepted archives. Save a new validated stage archive
without overwriting an existing archive, update `dist/twitch.apk`, and verify
both copies' SHA-256. APKs, SDK caches and diagnostic sources are not checkpoint
inputs.
