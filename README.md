# BTTV-android [![license: MIT][license-badge]][license-file] [![bttv version][bttv-version]][latest-release] ![build on][base-version]

A mod of the Twitch Android Mobile App adding BetterTTV and FrankerFaceZ emotes

> This project is **not affiliated** to Twitch Interactive Inc, NightBot LLC or Dan Salvato LLC!
> THE SOFTWARE IS PROVIDED "AS IS", **WITHOUT WARRANTY OF ANY KIND**!

# Install

1. Scim through this [guide on how to enable third party app istallations][enable-guide]
2. Go to the [latest release][latest-release] page and download the `.apk` file of the mod. It periodically checks for new releases so you only have to download it once from there.
   [![How to download][howtodl]][latest-release]

# Build it yourself

> This patch is meant to be applied to version **10.1.0** of the official twitch app
> Make soure you use this version before you create an issue!

## Easy way: Docker

### Prerequisites:

Docker

### How to:

1. Pull the builder `docker pull ghcr.io/bttv-android/builder`
2. Get the Twitch App's apk file (e.g from [here][evozi]), drop it in this directory and call it "twitch.apk"
   > Please make sure you get it from a non-shady source!
3. Run the builder: `docker run --rm -ti -v path/to/twitch.apk:/usr/build/twitch.apk -v /path/to/dist:/usr/build/dist ghcr.io/bttv-android/builder`
   > If you are in the same directory as the apk file you can copy and paste: `docker run --rm -ti -v $(pwd)/twitch.apk:/usr/build/twitch.apk -v $(pwd)/dist:/usr/build/dist ghcr.io/bttv-android/builder`
4. The `dist` directory will contain the patched apk file!
5. Transfer to device and [install apk](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android)
6. Remove the builder again: `docker rmi ghcr.io/bttv-android/builder`

## Hard way

### Prerequisites:

| Tool                                                                 | Env variable             | Default                                        |
| -------------------------------------------------------------------- | ------------------------ | ---------------------------------------------- |
| Bash                                                                 | _Has to be in /bin_      |                                                |
| Git                                                                  | _Has to be in PATH_      |                                                |
| Java JDK                                                             | JAVA_PATH and JAVAC_PATH | java and javac (from PATH)                     |
| [Android SKD Buildtools][buildtools] or install using Android Studio | BUILDTOOLS_PATH          | ~/Android/Sdk/build-tools/30.0.3               |
| [ApkTool][apktool]                                                   | APKTOOL_PATH             | /opt/apktool/apktool.jar                       |
| [Uber APK Signer][uber]                                              | UBER_APK_SIGNER_PATH     | /opt/uber-apk-signer/uber-apk-signer-1.2.1.jar |
| [Baksmali][baksmali]                                                 | BAKSMALI_PATH            | /opt/baksmali/baksmali-2.4.0.jar               |

### How to:

> If you get stuck at any point, just remove the `disass` dir and try again

0. Download and install prerequisites
1. Get the Twitch App's apk file (e.g from [here][evozi]), drop it in this directory and call it "twitch.apk"
   > Please make sure you get it from a non-shady source!
   > If you have adb installed follow [this guide][adb-apk] (Method 3)
2. The `./initworkspace` script will disassemble the apk, build the sources, apply monkey patches and build the new apk for you
3. Transfer to device and [install apk](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android)
   > (Adb installed and device connected? Try the : `./install` script)

# For contributers

> Read the patch it yourself part (the hard way) above aswell!

Do the following once:

1. Intialize the workspace as described above
2. Rename `disass` to `extracted`

3. Now you can build the java source code in (`source`) using `./buildsource extracted`.
   This will result in the creation of a directory called `sourceout`.
   It will also automaticly dex the class files and baksmali them.
   I recommend to scim through the script so you can set your environment variables.
4. Build your new changes using the `./build extracted` script.
5. Open an emulator and run `./install`

> Please only modify or add java files in the bttv package! Others won't be compiled anyway.
> In case you need to apply a monkeypatch (i.e. edit smali files (not in the bttv package) in `extracted` directly) read the [instructions below](#genmonke-and-branches-in-extraced).
> **Never check in the `extracted` or `disass` directories for legal reasons**

## Overview

```
bttv/
├─ initworkspace - used to set up everything
├─ buildsource - used to build the java sources
├─ build - re-assembles the 'extracted' dir
├─ decompile - you need JADX to use this
├─ install - install the result of build on a device (using adb)
├─ monke.patch - contains monkey patches
├─ genmonke - script that generates the monke.patch based on extracted (more below)
├─ extracted/ - renamed from disass
│  ├─ AndroidManifest.xml
│  ├─ res/
│  ├─ smali_classes0-10/ - contains disassembled app + bttv code
│  ├─ dist/
│  │  ├─ twitch.apk - the final app
├─ source/ - java sources for patches
│  ├─ bttv/ - the only relevant directory, rest are stubs

```

### genmonke and branches in extraced

During initialization the disass / extraced dir is initialized with an empty git repo.
Right after the disassemblement the first commit is made and tagged "base".
This is used to generate the patch file.
In order to distinguish between patches made by compiling `source` and monkey patches made directly in `extraced`
a branch named `javaonly` was created. Right after the first compilation this branch recieves a commit with only these changes.
monke.patch is thus the result of a diff between master and javaonly.

#### Only source changes

You need to nothing else, you can commit your code and start a pull request!

#### Only monkey patch changes

(i.e. changes to resources)

Just run the `./genmonkeyonly` script and commit it. Your PR should only contain changes in `monke.patch`.

#### Both source and monkey patch changes

Run `./genmonkefull` script and commit it. Your PR will contain both changes to `source/` and `monke.patch`.
If, for whatever reason, the script fails¹ you might need to `cd` into `extracted` run `git checkout master` and `git stash pop` to get your changes back.

¹: If the script stops, because it detected no changes, you do not need to do that.

# Best practices for contributers

Everytime you get a new version of the code (e.g. using git pull or git checkout) remove `extraced` and run `./initworkspace` again.

[license-badge]: https://img.shields.io/github/license/bttv-android/bttv?style=flat-square
[license-file]: ./LICENSE
[bttv-version]: https://img.shields.io/badge/current%20version-v0.0.8-blue?style=flat-square
[base-version]: https://img.shields.io/badge/build%20on-v10.1.0-blueviolet?style=flat-square
[latest-release]: https://github.com/bttv-android/bttv/releases/latest
[enable-guide]: https://www.howtogeek.com/696504/how-to-install-third-party-app-stores-on-android/
[howtodl]: ./.github/dltut.webp?raw=true
[baksmali]: https://github.com/JesusFreke/smali
[uber]: https://github.com/patrickfav/uber-apk-signer/releases/latest
[apktool]: https://ibotpeaches.github.io/Apktool/
[buildtools]: https://androidsdkmanager.azurewebsites.net/Buildtools
[evozi]: https://apps.evozi.com/apk-downloader/?id=tv.twitch.android.app
[adb-apk]: https://beebom.com/how-extract-apk-android-app/
