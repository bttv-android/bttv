# bttv-android [![license: MIT][license-badge]][license-file] [![bttv version][bttv-version]][latest-release] ![build on][base-version] [![Crowdin](https://badges.crowdin.net/bttv-android/localized.svg)](https://crowdin.com/project/bttv-android)

A mod of the Twitch Android Mobile App adding BetterTTV, FrankerFaceZ and 7TV emotes

> This project is **not affiliated** to Twitch Interactive Inc, NightBot LLC, Dan Salvato LLC or the SevenTV project!
> THE SOFTWARE IS PROVIDED "AS IS", **WITHOUT WARRANTY OF ANY KIND**!

 [![Hero image][hero-img]][hero-img]

## We need you
**You can help this project by contributing translations on [crowdin][crowdin]**.

# Install

1. Scim through this [guide on how to enable third party app istallations][enable-guide]
2. Go to the [latest release][latest-release] page and download the `.apk` file of the mod. It periodically checks for new releases so you only have to download it once from there.
   [![How to download][howtodl]][latest-release]

# Build it yourself

> This patch is meant to be applied to version **11.0.0** of the official twitch app
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
| [build-companion][build-companion]                                   | BUILD_COMPANION          | /opt/build-companion                           |

### How to:

> If you get stuck at any point, just remove the `disass` dir and try again

0. Download and install prerequisites
1. Get the Twitch App's apk files (e.g from [here][evozi]), drop them zipped in this directory and call it "twitch.zip"
   > Please make sure you get it from a non-shady source!
   > If you have adb installed follow [this guide][adb-apk] (Method 3)
2. The `./initworkspace` script will disassemble the apks, build the sources, apply monkey patches and build the new apk for you
3. Transfer to device and [install apk](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android)
   > (Adb installed and device connected? Try the : `./install` script)

This repo is mirrored on [Gitlab][mirror-gitlab].

Made with ♥️ by [@FoseFx](https://github.com/FoseFx) and contributors. MIT License.

[license-badge]: https://img.shields.io/github/license/bttv-android/bttv?style=flat-square
[license-file]: ./LICENSE
[bttv-version]: https://img.shields.io/github/v/release/bttv-android/bttv?style=flat-square
[base-version]: https://img.shields.io/badge/build%20on-v11.0.0-blueviolet?style=flat-square
[latest-release]: https://github.com/bttv-android/bttv/releases/latest
[crowdin]: https://crowdin.com/project/bttv-android
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
[build-companion]: https://github.com/bttv-android/build-companion/releases/latest
[mirror-gitlab]: https://gitlab.com/fosefx/bttv


## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://www.fosefx.com/"><img src="https://avatars.githubusercontent.com/u/22842759?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Max Baumann</b></sub></a><br /><a href="#maintenance-FoseFx" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!