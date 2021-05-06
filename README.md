# bttv-android [![license: MIT][license-badge]][license-file] [![bttv version][bttv-version]][latest-release] ![build on][base-version]

A mod of the Twitch Android Mobile App adding BetterTTV and FrankerFaceZ emotes

> This project is **not affiliated** to Twitch Interactive Inc, NightBot LLC or Dan Salvato LLC!
> THE SOFTWARE IS PROVIDED "AS IS", **WITHOUT WARRANTY OF ANY KIND**!

 [![Hero image][hero-img]][hero-img]

# Install

1. Scim through this [guide on how to enable third party app istallations][enable-guide]
2. Go to the [latest release][latest-release] page and download the `.apk` file of the mod. It periodically checks for new releases so you only have to download it once from there.
   [![How to download][howtodl]][latest-release]

# Build it yourself

> This patch is meant to be applied to version **10.7.0** of the official twitch app
> Make sure you use this version before you create an issue!

## Easy way: Docker

### Prerequisites:

Docker

### How to:

1. Clone this repo
2. Get the Twitch App's apk files (e.g from [here][evozi], or [here][apkmirror])
   > Please make sure you get it from a non-shady source!
  
   > If you have downloaded a single `.apk` file, name it `twitch.apk`
   
   > If you have downloaded a bundle of `.apk` files put them in a `.zip` archive and name it `twitch.zip`
   
   > Note: Apkmirror publishes bundles as `.apkm` files, those are just `zip` files, so rename them to `twitch.zip`

3. Run the builder: 
   ```
   docker run --rm -ti \
      -v path/to/twitch.apk:/usr/build/twitch.apk \
      -v /path/to/dist:/usr/build/dist \
      -v path/to/monke.patch:/usr/build/monke.patch \
      -v /path/to/mod:/usr/build/mod \
      ghcr.io/bttv-android/builder
      ```
   > If you are in the same directory as the apk file you can copy and paste: 
      ```
      docker run --rm -ti \
         -v $(pwd)/twitch.apk:/usr/build/twitch.apk \
         -v $(pwd)/dist:/usr/build/dist \
         -v $(pwd)/monke.patch:/usr/build/monke.patch \
         -v $(pwd)/mod:/usr/build/mod \
         ghcr.io/bttv-android/builder
      ```
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
| [Android SDK][sdk] or install using Android Studio | BUILDTOOLS_PATH | ~/Android/Sdk            |
| [ApkTool][apktool]                                                   | APKTOOL_PATH             | /opt/apktool/apktool.jar                       |
| [Uber APK Signer][uber]                                              | UBER_APK_SIGNER_PATH     | /opt/uber-apk-signer/uber-apk-signer-1.2.1.jar |
| [Baksmali][baksmali]                                                 | BAKSMALI_PATH            | /opt/baksmali/baksmali-2.4.0.jar               |
| [public-fixer][public-fixer]                                         | PUBLIC_FIXER             | /opt/public-fixer                              |

### How to:

> If you get stuck at any point, just remove the `disass` dir and try again

0. Download and install prerequisites
1. Get the Twitch App's apk files (e.g from [here][evozi]), drop them zipped in this directory and call it "twitch.zip"
   > Please make sure you get it from a non-shady source!
   > If you have adb installed follow [this guide][adb-apk] (Method 3)
2. The `./initworkspace` script will disassemble the apks, build the sources, apply monkey patches and build the new apk for you
3. Transfer to device and [install apk](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android)
   > (Adb installed and device connected? Try the : `./install` script)

# For contributers

> Read the patch it yourself part (the hard way) above aswell!

Do the following once:

1. Intialize the workspace as described above
2. Rename `disass` to `extracted`

3. Now you can build the java source code in (`source`) using `./buildsource extracted`.
   It will automaticly dex the class files and baksmali them.
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
├─ mod/        - java sources for patches
│  ├─ app/     - sources for the mod
|  ├─ twitch/  - stubs so we can call twitch's classes

```

### genmonke

During initialization the disass / extraced dir is initialized with an empty git repo.
Right after the disassemblement the first commit is made and tagged "base".
This is used to generate the patch file.
`monke.patch` is thus the result of a diff between master and base.

#### Only source changes

You need to nothing else, you can commit your code and start a pull request!

#### Monkey patch changes

Run `./genmonke <dir>` before you make a commit.

## Best practices for contributers

> Please also read the [architecture.md](./architecture.md) file!

Everytime you get a new version of the code (e.g. using git pull or git checkout) remove `extraced` and run `./initworkspace` again.

[license-badge]: https://img.shields.io/github/license/bttv-android/bttv?style=flat-square
[license-file]: ./LICENSE
[bttv-version]: https://img.shields.io/badge/current%20version-v0.4.0-blue?style=flat-square
[base-version]: https://img.shields.io/badge/build%20on-v10.7.0-blueviolet?style=flat-square
[latest-release]: https://github.com/bttv-android/bttv/releases/latest
[enable-guide]: https://www.howtogeek.com/696504/how-to-install-third-party-app-stores-on-android/
[howtodl]: ./.github/dltut.webp?raw=true
[hero-img]: ./.github/bttvog.jpg?raw=true
[baksmali]: https://github.com/JesusFreke/smali
[uber]: https://github.com/patrickfav/uber-apk-signer/releases/latest
[apktool]: https://ibotpeaches.github.io/Apktool/
[sdk]: https://developer.android.com/studio/#downloads
[evozi]: https://apps.evozi.com/apk-downloader/?id=tv.twitch.android.app
[apkmirror]: https://www.apkmirror.com/apk/twitch-interactive-inc/twitch/
[adb-apk]: https://beebom.com/how-extract-apk-android-app/
[public-fixer]: https://github.com/bttv-android/public-fixer/releases/latest
