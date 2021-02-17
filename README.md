# BTTV-android ![GitHub](https://img.shields.io/github/license/bttv-android/bttv?style=flat-square)

A mod of the Twitch Android Mobile App adding BetterTTV and FrankerFaceZ emotes

> This project is **not affiliated** to Twitch Interactive Inc, NightBot LLC or Dan Salvato LLC!
> THE SOFTWARE IS PROVIDED "AS IS", **WITHOUT WARRANTY OF ANY KIND**!

## Build it yourself

> This patch is meant to be applied to version **10.1.0** of the official twitch app
> Make soure you use this version before you create an issue!

## Prerequisites:

| Tool                                                                 | Env variable             | Default                                        |
| -------------------------------------------------------------------- | ------------------------ | ---------------------------------------------- |
| Bash                                                                 | _Has to be in /bin_      |                                                |
| Git                                                                  | _Has to be in PATH_      |                                                |
| Java JDK                                                             | JAVA_PATH and JAVAC_PATH | java and javac (from PATH)                     |
| [Android SKD Buildtools][buildtools] or install using Android Studio | BUILDTOOLS_PATH          | ~/Android/Sdk/build-tools/30.0.3               |
| [ApkTool][apktool]                                                   | APKTOOL_PATH             | /opt/apktool/apktool.jar                       |
| [Uber APK Signer][uber]                                              | UBER_APK_SIGNER_PATH     | /opt/uber-apk-signer/uber-apk-signer-1.2.1.jar |
| [Baksmali][baksmali]                                                 | BAKSMALI_PATH            | /opt/baksmali/baksmali-2.4.0.jar               |

> If you get stuck at any point, just remove the `disass` dir and try again

0. Download and install prerequisites
1. Get the Twitch App's apk file (e.g from [here][evozi]), drop it in this directory and call it "twitch.apk"
   > Please make sure you get it from a non-shady source!
   > If you have adb installed follow [this guide][adb-apk] (Method 3)
2. The `./initworkspace` script will disassemble the apk, build the sources, apply monkey patches and build the new apk for you
3. Transfer to device and [install apk](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android)
   > (Adb installed and device connected? Try the : `./install` script)

## For contributers

> Read the patch it yourself part above aswell!

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

[baksmali]: https://github.com/JesusFreke/smali
[uber]: https://github.com/patrickfav/uber-apk-signer/releases/latest
[apktool]: https://ibotpeaches.github.io/Apktool/
[buildtools]: ttps://androidsdkmanager.azurewebsites.net/Buildtools
[evozi]: https://apps.evozi.com/apk-downloader/?id=tv.twitch.android.app
[adb-apk]: https://beebom.com/how-extract-apk-android-app/
