[![license: MIT][license-badge]][license-file]
[![bttv version][bttv-version]][latest-release]
![build on][base-version]
![GitHub release (latest by date)][downloads-latest]
![GitHub all releases][downloads-total]
[![Discord][discord-badge]][discord-invite]

# bttv-android


A mod of the Twitch Android Mobile<sup><a href="#no-tv">1</a></sup> App adding BetterTTV, FrankerFaceZ and 7TV emotes

> This project is **not affiliated** to Twitch Interactive Inc, NightBot LLC, Dan Salvato LLC or the SevenTV project!
> THE SOFTWARE IS PROVIDED "AS IS", **WITHOUT WARRANTY OF ANY KIND**!

 [![Hero image][hero-img]][hero-img]

## We need you
**You can help this project by contributing translations on [crowdin][crowdin]**.

## Features
 - BTTV, FFZ and 7TV Emotes
 - Auto-Claim Bonus Channel Points
 - Auto Updater
 - Sleep Timer
 - Highlight Keywords
 - Split Chat
 - Anon Chat

## Known issues
 - Amazon Video Watch Parties require the Twitch App to be installed on the device and the optional step in the install instructions below
 - Animated Emotes occasionally glitch on some devices
 - 7TV's zero width emotes are not supported right now
 - Users have [reported that notifications are not working for them](https://github.com/bttv-android/bttv/issues/492). To fix this go to  "App-Info -> Open Supported Apps" and select "Only in this application".

 - <span id="no-tv">bttv-android will not work on Android TVs as it is a mod of the Twitch mobile App and not of the Twitch Andorid TV App. You can install it, just do not expect a good experience</span>

# Install

1. Skim through this [guide on how to enable third party app istallations][enable-guide]
2. Go to the [latest release][latest-release] page and download the `.apk` file of the mod. It periodically checks for new releases so you only have to download it once from there.
   [![How to download][howtodl]][latest-release]
3. (Optional) Prevent Twitch from opening when you interact with twitch.tv links: Long-press on the Twitch App -> "App Info" -> "Advanced" -> "Open by default" -> "Open supported links" -> "Ask every time" (might vary depending on OEM)

# Build it yourself

> This patch is meant to be applied to version **13.4.1** of the official twitch app
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
      -v path/to/patches:/usr/build/patches \
      -v /path/to/mod:/usr/build/mod \
      -v /path/to/.all-contributorsrc:/usr/build/.all-contributorsrc \
      -v /path/to/bttv.manifest.json:/usr/build/bttv.manifest.json \
      ghcr.io/bttv-android/builder
      ```
   > If you are in the same directory as the apk file you can copy and paste: 
      ```
      docker run --rm -ti \
         -v $(pwd)/twitch.apk:/usr/build/twitch.apk \
         -v $(pwd)/.bttv-cache:/usr/build/.bttv-cache \
         -v $(pwd)/dist:/usr/build/dist \
         -v $(pwd)/patches:/usr/build/patches \
         -v $(pwd)/mod:/usr/build/mod \
         -v $(pwd)/.all-contributorsrc:/usr/build/.all-contributorsrc \
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
| jq                                                                   | _Has to be in PATH_      |                                                |
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
[base-version]: https://img.shields.io/badge/build%20on-v13.4.1-blueviolet?style=flat-square
[downloads-latest]: https://img.shields.io/github/downloads/bttv-android/bttv/latest/total?color=yellow&style=flat-square
[downloads-total]: https://img.shields.io/github/downloads/bttv-android/bttv/total?color=yellowgreen&label=downloads%20total&style=flat-square
[latest-release]: https://github.com/bttv-android/bttv/releases/latest
[crowdin]: https://crowdin.com/project/bttv-android
[discord-badge]: https://img.shields.io/discord/856518013292249089?color=blue&label=discord&logo=discord&logoColor=white&style=flat-square
[discord-invite]: https://discord.gg/7jgDGdXXqN
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
    <td align="center"><a href="https://bmn.dev/"><img src="https://avatars.githubusercontent.com/u/22842759?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Max Baumann</b></sub></a><br /><a href="#maintenance-FoseFx" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/LEMIBANDDEXARI"><img src="https://avatars.githubusercontent.com/u/70129787?v=4?s=100" width="100px;" alt=""/><br /><sub><b>LEMIBANDDEXARI</b></sub></a><br /><a href="#translation-LEMIBANDDEXARI" title="Translation">🌍</a> <a href="#ideas-LEMIBANDDEXARI" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/bttv-android/bttv/issues?q=author%3ALEMIBANDDEXARI" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/StephanBruh"><img src="https://avatars.githubusercontent.com/u/19285400?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Stephan</b></sub></a><br /><a href="#ideas-StephanBruh" title="Ideas, Planning, & Feedback">🤔</a> <a href="#translation-StephanBruh" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/zeykafx"><img src="https://avatars.githubusercontent.com/u/37271973?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Corentin Detry</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Azeykafx" title="Bug reports">🐛</a> <a href="#translation-zeykafx" title="Translation">🌍</a></td>
    <td align="center"><a href="http://dioneb.me"><img src="https://avatars.githubusercontent.com/u/15141090?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Dione Batista</b></sub></a><br /><a href="#translation-Santagain" title="Translation">🌍</a> <a href="https://github.com/bttv-android/bttv/issues?q=author%3ASantagain" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/Spkz"><img src="https://avatars.githubusercontent.com/u/19599808?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Erős Dániel</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ASpkz" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/Xslash58"><img src="https://avatars.githubusercontent.com/u/75801324?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Xslash</b></sub></a><br /><a href="#translation-Xslash58" title="Translation">🌍</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/GogaFroga"><img src="https://avatars.githubusercontent.com/u/80777820?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Artur</b></sub></a><br /><a href="#translation-GogaFroga" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/YOEL-44"><img src="https://avatars.githubusercontent.com/u/7842997?v=4?s=100" width="100px;" alt=""/><br /><sub><b>YOEL_44</b></sub></a><br /><a href="#translation-YOEL-44" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/albertopasqualetto"><img src="https://avatars.githubusercontent.com/u/39854348?v=4?s=100" width="100px;" alt=""/><br /><sub><b>albertopasqualetto</b></sub></a><br /><a href="#translation-albertopasqualetto" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/kendricktsoi"><img src="https://avatars.githubusercontent.com/u/53035976?v=4?s=100" width="100px;" alt=""/><br /><sub><b>kendricktsoi</b></sub></a><br /><a href="#translation-kendricktsoi" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/ashamefuldog"><img src="https://avatars.githubusercontent.com/u/52229049?v=4?s=100" width="100px;" alt=""/><br /><sub><b>ashamefuldog</b></sub></a><br /><a href="#ideas-ashamefuldog" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/Zeuxis29"><img src="https://avatars.githubusercontent.com/u/84096340?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Zeuxis29</b></sub></a><br /><a href="#translation-Zeuxis29" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/xplod62"><img src="https://avatars.githubusercontent.com/u/87228564?v=4?s=100" width="100px;" alt=""/><br /><sub><b>xplod62</b></sub></a><br /><a href="#ideas-xplod62" title="Ideas, Planning, & Feedback">🤔</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/colahobi"><img src="https://avatars.githubusercontent.com/u/73035288?v=4?s=100" width="100px;" alt=""/><br /><sub><b>colahobi</b></sub></a><br /><a href="#ideas-colahobi" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/Epicnicity322"><img src="https://avatars.githubusercontent.com/u/13574419?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Christiano Rangel</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3AEpicnicity322" title="Bug reports">🐛</a> <a href="#ideas-Epicnicity322" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/GauravM512"><img src="https://avatars.githubusercontent.com/u/37218716?v=4?s=100" width="100px;" alt=""/><br /><sub><b>GauravM512</b></sub></a><br /><a href="#ideas-GauravM512" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/bttv-android/bttv/issues?q=author%3AGauravM512" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/nelswad90"><img src="https://avatars.githubusercontent.com/u/2206347?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Dennis Magee</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Anelswad90" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/Senpai206"><img src="https://avatars.githubusercontent.com/u/68463773?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Senpai206</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ASenpai206" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/ReggX"><img src="https://avatars.githubusercontent.com/u/1953510?v=4?s=100" width="100px;" alt=""/><br /><sub><b>ReggX</b></sub></a><br /><a href="#ideas-ReggX" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/ZerolCamb"><img src="https://avatars.githubusercontent.com/u/64336142?v=4?s=100" width="100px;" alt=""/><br /><sub><b>ZerolCamb</b></sub></a><br /><a href="#translation-ZerolCamb" title="Translation">🌍</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/metezd"><img src="https://avatars.githubusercontent.com/u/37701679?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Mete</b></sub></a><br /><a href="#translation-metezd" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/splizh"><img src="https://avatars.githubusercontent.com/u/85130943?v=4?s=100" width="100px;" alt=""/><br /><sub><b>splizh</b></sub></a><br /><a href="#translation-splizh" title="Translation">🌍</a></td>
    <td align="center"><a href="https://bandism.net/"><img src="https://avatars.githubusercontent.com/u/22633385?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Ikko Ashimine</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/commits?author=eltociear" title="Documentation">📖</a></td>
    <td align="center"><a href="http://e-hentai.ml"><img src="https://avatars.githubusercontent.com/u/50764666?v=4?s=100" width="100px;" alt=""/><br /><sub><b>KeepSOBP</b></sub></a><br /><a href="#translation-KeepSOBP" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/jhurag"><img src="https://avatars.githubusercontent.com/u/25354634?v=4?s=100" width="100px;" alt=""/><br /><sub><b>jhurag</b></sub></a><br /><a href="#translation-jhurag" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/Avernys"><img src="https://avatars.githubusercontent.com/u/75722852?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Avernys</b></sub></a><br /><a href="#translation-Avernys" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/Jfamig"><img src="https://avatars.githubusercontent.com/u/21336678?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Jfamig</b></sub></a><br /><a href="#ideas-Jfamig" title="Ideas, Planning, & Feedback">🤔</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/NumbPat"><img src="https://avatars.githubusercontent.com/u/88035271?v=4?s=100" width="100px;" alt=""/><br /><sub><b>NumbPat</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ANumbPat" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/JRoy"><img src="https://avatars.githubusercontent.com/u/10731363?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Josh Roy</b></sub></a><br /><a href="#ideas-JRoy" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/Lather0519"><img src="https://avatars.githubusercontent.com/u/32637838?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Lather0519</b></sub></a><br /><a href="#translation-Lather0519" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/zezofelipe"><img src="https://avatars.githubusercontent.com/u/37094298?v=4?s=100" width="100px;" alt=""/><br /><sub><b>zezofelipe</b></sub></a><br /><a href="#translation-zezofelipe" title="Translation">🌍</a> <a href="#ideas-zezofelipe" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/ARTH-V-R"><img src="https://avatars.githubusercontent.com/u/101481528?v=4?s=100" width="100px;" alt=""/><br /><sub><b>ARTH-V-R</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3AARTH-V-R" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/linkmath"><img src="https://avatars.githubusercontent.com/u/43995639?v=4?s=100" width="100px;" alt=""/><br /><sub><b>linkmath</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Alinkmath" title="Bug reports">🐛</a></td>
    <td align="center"><a href="http://thedearbear.com"><img src="https://avatars.githubusercontent.com/u/37769766?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Oleg</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ATheDearbear" title="Bug reports">🐛</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/3bood-k"><img src="https://avatars.githubusercontent.com/u/74608947?v=4?s=100" width="100px;" alt=""/><br /><sub><b>3bood_k</b></sub></a><br /><a href="#translation-3bood-k" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/moonMan8"><img src="https://avatars.githubusercontent.com/u/107777735?v=4?s=100" width="100px;" alt=""/><br /><sub><b>moonMan8</b></sub></a><br /><a href="#ideas-moonMan8" title="Ideas, Planning, & Feedback">🤔</a></td>
    <td align="center"><a href="https://github.com/mirronake"><img src="https://avatars.githubusercontent.com/u/96698262?v=4?s=100" width="100px;" alt=""/><br /><sub><b>mirronake</b></sub></a><br /><a href="#translation-mirronake" title="Translation">🌍</a></td>
    <td align="center"><a href="https://github.com/mariusandersen"><img src="https://avatars.githubusercontent.com/u/3061094?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Marius Andersen</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Amariusandersen" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/mklwong"><img src="https://avatars.githubusercontent.com/u/8085890?v=4?s=100" width="100px;" alt=""/><br /><sub><b>mklwong</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Amklwong" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/owwouwu"><img src="https://avatars.githubusercontent.com/u/112671011?v=4?s=100" width="100px;" alt=""/><br /><sub><b>owwouwu</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Aowwouwu" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/Wissididom"><img src="https://avatars.githubusercontent.com/u/30803034?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Wissididom</b></sub></a><br /><a href="#translation-Wissididom" title="Translation">🌍</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
