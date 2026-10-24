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

### Emotes

- BTTV, FFZ and 7TV global and channel emotes
- Animated GIF and WebP emotes
- Third-party emotes in the native emote picker
- Emote autocomplete
- Frequently Used emotes
- Emote details

### Chat

- Split Chat
- Show Deleted Messages
- Highlight Keywords
- Chat Blacklist
- Chat Timestamps
- BTTV, FFZ and Chatterino Badges
- Anonymous Chat

### Other features

- Auto-Claim Bonus Channel Points
- Sleep Timer
- Hide Stories
- BTTV Settings and Stream Quick Settings
- Manual and Automatic Update Checking
- Optional Browser Links for Bits, Subscriptions and Gifts

## Known issues

- Anonymous Chat is read-only. Enabling or disabling it requires restarting the app.
- 7TV zero-width emotes are not supported.
- Personal emotes are not currently supported.
- Push notifications may not work.
- <span id="no-tv">bttv-android is not designed for Android TV, as it modifies the Twitch mobile app rather than the Twitch Android TV app.</span>

# Install

1. Skim through this [guide on how to enable third-party app installations][enable-guide].
2. Go to the [latest release][latest-release] page and download the mod APK.
   BTTV can periodically check for new releases. Automatic update checking can be
   enabled or disabled in BTTV Settings. Downloading and installing updates
   requires confirmation.

   [![How to download][howtodl]][latest-release]

3. (Optional) Prevent Twitch from opening when you interact with twitch.tv links:
   Long-press on the Twitch App -> "App Info" -> "Advanced" -> "Open by default"
   -> "Open supported links" -> "Ask every time" (might vary depending on OEM).

# Build it yourself

> This patch is meant to be applied to version **31.2.0 (3102006)** of the official Twitch Android app.
> Make sure you use the correct version before creating an issue!

The resulting app requires Android 10 or later and supports ARM64 and ARMv7.

## Easy way: Docker

### Prerequisites

- Git and Docker with Docker Compose (Linux containers).
- The builder uses `linux/amd64`; other host architectures need Docker's x86-64 emulation.
- Enough disk space for the Android tools, Gradle dependencies and decoded app.
- The supported Twitch 31.2.0 APK components obtained from a trusted source,
  such as [Evozi][evozi] or [APKMirror][apkmirror].

### How to

1. Clone this repository and open a terminal in it.
2. Prepare `twitch.zip` with these five **root-level** APK entries:

   ```text
   base.apk
   split_config.arm64_v8a.apk
   split_config.armeabi_v7a.apk
   split_config.xxhdpi.apk
   split_config.xxxhdpi.apk
   ```

   APKMirror's `.apkm` is a ZIP container, but renaming an arbitrary bundle is
   insufficient: it may contain different variants or lack required splits.
   The validator pins the exact supported APK payloads in
   [tools/twitch-31.2.0-inputs.json](tools/twitch-31.2.0-inputs.json).
   With Python 3 installed, prepare a new input from a matching five-APK bundle:

   ```bash
   python3 tools/prepare_twitch_input.py twitch-31.2.0.apkm twitch.zip
   ```

   If the proven base/ARM64/density bundle contains only four APKs, the supported
   preparation can add **only** the pinned ARMv7 split from a second bundle:

   ```bash
   python3 tools/prepare_twitch_input.py proven-arm64.apkm twitch.zip --armv7-bundle armv7-source.apkm
   ```

   The second bundle's base, ARM64 and density APKs are never substituted.
   Every selected APK must match its recorded hash.

   Or validate an existing input without replacing it:

   ```bash
   python3 tools/prepare_twitch_input.py twitch.zip
   ```

   On Windows, use `python` if that is your Python 3 command. If the validator
   rejects a variant or a missing split, do not mix unverified APKs to bypass it.
   Neither this repository nor the builder downloads Twitch for you.

3. Build the local toolchain and run the isolated builder:

   ```bash
   docker compose build
   docker compose run --rm build
   ```

   The source is mounted read-only. Compilation and decoding use a fresh Linux
   workspace; only a separate Gradle dependency cache is reused. The signed,
   verified result is `dist/docker/build/twitch.apk`, with `verification.json`
   and `build.log` beside it. Existing outputs are never overwritten. For a
   second build, select a new output directory with `BTTV_OUTPUT_DIR` (for
   example, `./dist/docker-next`) in your shell before running Compose.

4. Transfer the APK to your device and [install it](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android).

## Hard way

### Prerequisites

Use x86-64 Linux or WSL with Bash, Git, Python 3, jq, rsync, zip/unzip and Java JDK 11.
The Gradle wrapper supplies Gradle 7.2; the project uses Android Gradle Plugin 4.1.3.
The inherited manual scripts require SDK and build-tool installation paths without
spaces or other whitespace. The standard Docker paths already meet this requirement.

| Tool | Environment variable | Required version / setup |
| --- | --- | --- |
| Java JDK | `JAVA_PATH`, `JAVAC_PATH` | Java 11; defaults to `java`/`javac` on PATH |
| [Android SDK][sdk] | `ANDROID_SDK_ROOT` | Build Tools **30.0.3** and `platforms;android-30` |
| [Apktool][apktool] | `APKTOOL_PATH` | 2.9.3 JAR |
| [APKEditor](https://github.com/REAndroid/APKEditor) | `APKEDITOR_PATH` | 1.4.9 JAR; split-aware merge before decoding |
| [Uber APK Signer][uber] | `UBER_APK_SIGNER_PATH` | 1.2.1 JAR |
| [Baksmali][baksmali] | `BAKSMALI_PATH` | 2.4.0 JAR |
| [build-companion][build-companion] | `BUILD_COMPANION` | 5.2.0 executable |

### How to

1. Install the prerequisites and accept the Android SDK licenses. With the SDK
   command-line tools installed, install the exact SDK packages:

   ```bash
   sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "build-tools;30.0.3" "platforms;android-30"
   ```

2. Prepare and validate `twitch.zip` as above. Export the JAR/executable paths
   listed in the table. The defaults used by the Dockerfile are under `/opt`;
   a manual installation should set paths to its own installed tools.

3. Run the same build pipeline in new, isolated directories:

   ```bash
   python3 tools/build_production.py --input twitch.zip --workspace .tmp/manual-build --output dist/manual
   ```

   This selects production inputs, runs `initworkspace` away from the checkout,
   merges the splits before one Apktool decode, injects BTTV, applies patches,
   rebuilds, signs, verifies, and exports `dist/manual/twitch.apk`.
   Gradle requires access to its declared repositories unless the exact
   dependencies and Gradle distribution are already cached.
   Use new workspace/output names for another run; do not run `initworkspace`
   directly in your development checkout.

4. Transfer and install the exported APK as above. The legacy `./install` script
   expects `disass/dist/twitch.apk`; it is not used by these isolated exports.

Local builds use the signer's test identity when no `KS` is supplied.
They cannot necessarily update an installation signed by a different key.
See [release and signing requirements](RELEASING.md) for maintainer builds.

This repo is mirrored on [Gitlab][mirror-gitlab].

Made with ♥️ by [@FoseFx](https://github.com/FoseFx) and contributors. MIT License.

[license-badge]: https://img.shields.io/github/license/bttv-android/bttv?style=flat-square
[license-file]: ./LICENSE
[bttv-version]: https://img.shields.io/github/v/release/bttv-android/bttv?style=flat-square
[base-version]: https://img.shields.io/badge/build%20on-v31.2.0-blueviolet?style=flat-square
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
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://bmn.dev/"><img src="https://avatars.githubusercontent.com/u/22842759?v=4?s=100" width="100px;" alt="Max Baumann"/><br /><sub><b>Max Baumann</b></sub></a><br /><a href="#maintenance-FoseFx" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/LEMIBANDDEXARI"><img src="https://avatars.githubusercontent.com/u/70129787?v=4?s=100" width="100px;" alt="LEMIBANDDEXARI"/><br /><sub><b>LEMIBANDDEXARI</b></sub></a><br /><a href="#translation-LEMIBANDDEXARI" title="Translation">🌍</a> <a href="#ideas-LEMIBANDDEXARI" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/bttv-android/bttv/issues?q=author%3ALEMIBANDDEXARI" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/StephanBruh"><img src="https://avatars.githubusercontent.com/u/19285400?v=4?s=100" width="100px;" alt="Stephan"/><br /><sub><b>Stephan</b></sub></a><br /><a href="#ideas-StephanBruh" title="Ideas, Planning, & Feedback">🤔</a> <a href="#translation-StephanBruh" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zeykafx"><img src="https://avatars.githubusercontent.com/u/37271973?v=4?s=100" width="100px;" alt="Corentin Detry"/><br /><sub><b>Corentin Detry</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Azeykafx" title="Bug reports">🐛</a> <a href="#translation-zeykafx" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://dioneb.me"><img src="https://avatars.githubusercontent.com/u/15141090?v=4?s=100" width="100px;" alt="Dione Batista"/><br /><sub><b>Dione Batista</b></sub></a><br /><a href="#translation-Santagain" title="Translation">🌍</a> <a href="https://github.com/bttv-android/bttv/issues?q=author%3ASantagain" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Spkz"><img src="https://avatars.githubusercontent.com/u/19599808?v=4?s=100" width="100px;" alt="Erős Dániel"/><br /><sub><b>Erős Dániel</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ASpkz" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Xslash58"><img src="https://avatars.githubusercontent.com/u/75801324?v=4?s=100" width="100px;" alt="Xslash"/><br /><sub><b>Xslash</b></sub></a><br /><a href="#translation-Xslash58" title="Translation">🌍</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/GogaFroga"><img src="https://avatars.githubusercontent.com/u/80777820?v=4?s=100" width="100px;" alt="Artur"/><br /><sub><b>Artur</b></sub></a><br /><a href="#translation-GogaFroga" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/YOEL-44"><img src="https://avatars.githubusercontent.com/u/7842997?v=4?s=100" width="100px;" alt="YOEL_44"/><br /><sub><b>YOEL_44</b></sub></a><br /><a href="#translation-YOEL-44" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/albertopasqualetto"><img src="https://avatars.githubusercontent.com/u/39854348?v=4?s=100" width="100px;" alt="albertopasqualetto"/><br /><sub><b>albertopasqualetto</b></sub></a><br /><a href="#translation-albertopasqualetto" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/kendricktsoi"><img src="https://avatars.githubusercontent.com/u/53035976?v=4?s=100" width="100px;" alt="kendricktsoi"/><br /><sub><b>kendricktsoi</b></sub></a><br /><a href="#translation-kendricktsoi" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ashamefuldog"><img src="https://avatars.githubusercontent.com/u/52229049?v=4?s=100" width="100px;" alt="ashamefuldog"/><br /><sub><b>ashamefuldog</b></sub></a><br /><a href="#ideas-ashamefuldog" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Zeuxis29"><img src="https://avatars.githubusercontent.com/u/84096340?v=4?s=100" width="100px;" alt="Zeuxis29"/><br /><sub><b>Zeuxis29</b></sub></a><br /><a href="#translation-Zeuxis29" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/xplod62"><img src="https://avatars.githubusercontent.com/u/87228564?v=4?s=100" width="100px;" alt="xplod62"/><br /><sub><b>xplod62</b></sub></a><br /><a href="#ideas-xplod62" title="Ideas, Planning, & Feedback">🤔</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/colahobi"><img src="https://avatars.githubusercontent.com/u/73035288?v=4?s=100" width="100px;" alt="colahobi"/><br /><sub><b>colahobi</b></sub></a><br /><a href="#ideas-colahobi" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Epicnicity322"><img src="https://avatars.githubusercontent.com/u/13574419?v=4?s=100" width="100px;" alt="Christiano Rangel"/><br /><sub><b>Christiano Rangel</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3AEpicnicity322" title="Bug reports">🐛</a> <a href="#ideas-Epicnicity322" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/GauravM512"><img src="https://avatars.githubusercontent.com/u/37218716?v=4?s=100" width="100px;" alt="GauravM512"/><br /><sub><b>GauravM512</b></sub></a><br /><a href="#ideas-GauravM512" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/bttv-android/bttv/issues?q=author%3AGauravM512" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/nelswad90"><img src="https://avatars.githubusercontent.com/u/2206347?v=4?s=100" width="100px;" alt="Dennis Magee"/><br /><sub><b>Dennis Magee</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Anelswad90" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Senpai206"><img src="https://avatars.githubusercontent.com/u/68463773?v=4?s=100" width="100px;" alt="Senpai206"/><br /><sub><b>Senpai206</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ASenpai206" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ReggX"><img src="https://avatars.githubusercontent.com/u/1953510?v=4?s=100" width="100px;" alt="ReggX"/><br /><sub><b>ReggX</b></sub></a><br /><a href="#ideas-ReggX" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ZerolCamb"><img src="https://avatars.githubusercontent.com/u/64336142?v=4?s=100" width="100px;" alt="ZerolCamb"/><br /><sub><b>ZerolCamb</b></sub></a><br /><a href="#translation-ZerolCamb" title="Translation">🌍</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/metezd"><img src="https://avatars.githubusercontent.com/u/37701679?v=4?s=100" width="100px;" alt="Mete"/><br /><sub><b>Mete</b></sub></a><br /><a href="#translation-metezd" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/splizh"><img src="https://avatars.githubusercontent.com/u/85130943?v=4?s=100" width="100px;" alt="splizh"/><br /><sub><b>splizh</b></sub></a><br /><a href="#translation-splizh" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://bandism.net/"><img src="https://avatars.githubusercontent.com/u/22633385?v=4?s=100" width="100px;" alt="Ikko Ashimine"/><br /><sub><b>Ikko Ashimine</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/commits?author=eltociear" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://e-hentai.ml"><img src="https://avatars.githubusercontent.com/u/50764666?v=4?s=100" width="100px;" alt="KeepSOBP"/><br /><sub><b>KeepSOBP</b></sub></a><br /><a href="#translation-KeepSOBP" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/jhurag"><img src="https://avatars.githubusercontent.com/u/25354634?v=4?s=100" width="100px;" alt="jhurag"/><br /><sub><b>jhurag</b></sub></a><br /><a href="#translation-jhurag" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Avernys"><img src="https://avatars.githubusercontent.com/u/75722852?v=4?s=100" width="100px;" alt="Avernys"/><br /><sub><b>Avernys</b></sub></a><br /><a href="#translation-Avernys" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Jfamig"><img src="https://avatars.githubusercontent.com/u/21336678?v=4?s=100" width="100px;" alt="Jfamig"/><br /><sub><b>Jfamig</b></sub></a><br /><a href="#ideas-Jfamig" title="Ideas, Planning, & Feedback">🤔</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/NumbPat"><img src="https://avatars.githubusercontent.com/u/88035271?v=4?s=100" width="100px;" alt="NumbPat"/><br /><sub><b>NumbPat</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ANumbPat" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/JRoy"><img src="https://avatars.githubusercontent.com/u/10731363?v=4?s=100" width="100px;" alt="Josh Roy"/><br /><sub><b>Josh Roy</b></sub></a><br /><a href="#ideas-JRoy" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Lather0519"><img src="https://avatars.githubusercontent.com/u/32637838?v=4?s=100" width="100px;" alt="Lather0519"/><br /><sub><b>Lather0519</b></sub></a><br /><a href="#translation-Lather0519" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zezofelipe"><img src="https://avatars.githubusercontent.com/u/37094298?v=4?s=100" width="100px;" alt="zezofelipe"/><br /><sub><b>zezofelipe</b></sub></a><br /><a href="#translation-zezofelipe" title="Translation">🌍</a> <a href="#ideas-zezofelipe" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ARTH-V-R"><img src="https://avatars.githubusercontent.com/u/101481528?v=4?s=100" width="100px;" alt="ARTH-V-R"/><br /><sub><b>ARTH-V-R</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3AARTH-V-R" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/linkmath"><img src="https://avatars.githubusercontent.com/u/43995639?v=4?s=100" width="100px;" alt="linkmath"/><br /><sub><b>linkmath</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Alinkmath" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://thedearbear.com"><img src="https://avatars.githubusercontent.com/u/37769766?v=4?s=100" width="100px;" alt="Oleg"/><br /><sub><b>Oleg</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ATheDearbear" title="Bug reports">🐛</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/3bood-k"><img src="https://avatars.githubusercontent.com/u/74608947?v=4?s=100" width="100px;" alt="3bood_k"/><br /><sub><b>3bood_k</b></sub></a><br /><a href="#translation-3bood-k" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/moonMan8"><img src="https://avatars.githubusercontent.com/u/107777735?v=4?s=100" width="100px;" alt="moonMan8"/><br /><sub><b>moonMan8</b></sub></a><br /><a href="#ideas-moonMan8" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/mariusandersen"><img src="https://avatars.githubusercontent.com/u/3061094?v=4?s=100" width="100px;" alt="Marius Andersen"/><br /><sub><b>Marius Andersen</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Amariusandersen" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/mklwong"><img src="https://avatars.githubusercontent.com/u/8085890?v=4?s=100" width="100px;" alt="mklwong"/><br /><sub><b>mklwong</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Amklwong" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/owwouwu"><img src="https://avatars.githubusercontent.com/u/112671011?v=4?s=100" width="100px;" alt="owwouwu"/><br /><sub><b>owwouwu</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Aowwouwu" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Wissididom"><img src="https://avatars.githubusercontent.com/u/30803034?v=4?s=100" width="100px;" alt="Wissididom"/><br /><sub><b>Wissididom</b></sub></a><br /><a href="#translation-Wissididom" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/theSaintKappa"><img src="https://avatars.githubusercontent.com/u/96151089?v=4?s=100" width="100px;" alt="SaintKappa"/><br /><sub><b>SaintKappa</b></sub></a><br /><a href="#translation-theSaintKappa" title="Translation">🌍</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/sassvagyok"><img src="https://avatars.githubusercontent.com/u/76638318?v=4?s=100" width="100px;" alt="sassvagyok"/><br /><sub><b>sassvagyok</b></sub></a><br /><a href="#translation-sassvagyok" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/wacra"><img src="https://avatars.githubusercontent.com/u/82950858?v=4?s=100" width="100px;" alt="Wacra"/><br /><sub><b>Wacra</b></sub></a><br /><a href="#ideas-wacra" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://steamcommunity.com/profiles/76561198158864042 "><img src="https://avatars.githubusercontent.com/u/36592509?v=4?s=100" width="100px;" alt="Blueberryy"/><br /><sub><b>Blueberryy</b></sub></a><br /><a href="#translation-Blueberryy" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Dlevda"><img src="https://avatars.githubusercontent.com/u/122129905?v=4?s=100" width="100px;" alt="Dlevda"/><br /><sub><b>Dlevda</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ADlevda" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/AFranzo"><img src="https://avatars.githubusercontent.com/u/17572007?v=4?s=100" width="100px;" alt="AFranzo"/><br /><sub><b>AFranzo</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/commits?author=AFranzo" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/framerunner"><img src="https://avatars.githubusercontent.com/u/78372766?v=4?s=100" width="100px;" alt="Samsa"/><br /><sub><b>Samsa</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Aframerunner" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Benderosy"><img src="https://avatars.githubusercontent.com/u/126038064?v=4?s=100" width="100px;" alt="Benderosy"/><br /><sub><b>Benderosy</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ABenderosy" title="Bug reports">🐛</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/LtHusky1218"><img src="https://avatars.githubusercontent.com/u/126059193?v=4?s=100" width="100px;" alt="LtHusky1218"/><br /><sub><b>LtHusky1218</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3ALtHusky1218" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/abraxas86"><img src="https://avatars.githubusercontent.com/u/8891358?v=4?s=100" width="100px;" alt="abraxas86"/><br /><sub><b>abraxas86</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Aabraxas86" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Vikings09876"><img src="https://avatars.githubusercontent.com/u/11668494?v=4?s=100" width="100px;" alt="Ataraxia"/><br /><sub><b>Ataraxia</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3AVikings09876" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zakary2841"><img src="https://avatars.githubusercontent.com/u/26402356?v=4?s=100" width="100px;" alt="zakary2841"/><br /><sub><b>zakary2841</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Azakary2841" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ecan18"><img src="https://avatars.githubusercontent.com/u/149601367?v=4?s=100" width="100px;" alt="ecan18"/><br /><sub><b>ecan18</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3Aecan18" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Alucard-xi"><img src="https://avatars.githubusercontent.com/u/98977509?v=4?s=100" width="100px;" alt="Alucard"/><br /><sub><b>Alucard</b></sub></a><br /><a href="#translation-Alucard-xi" title="Translation">🌍</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Zerwin"><img src="https://avatars.githubusercontent.com/u/46265592?v=4?s=100" width="100px;" alt="Zerwin"/><br /><sub><b>Zerwin</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/commits?author=Zerwin" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ChristineBean11"><img src="https://avatars.githubusercontent.com/u/191286941?v=4?s=100" width="100px;" alt="ChristineBean11"/><br /><sub><b>ChristineBean11</b></sub></a><br /><a href="https://github.com/bttv-android/bttv/issues?q=author%3AChristineBean11" title="Bug reports">🐛</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!

## Acknowledgments

[Hooman's Morphe Patches](https://github.com/arandomhooman/hoomans-morphe-patches)
was a useful reference for Twitch compatibility and networking research during
the Twitch 31.2.0 port.

## Third-party notices

This project incorporates third-party open-source components distributed under
their respective licenses.

See [Third-party Notices](THIRD_PARTY_NOTICES.md) for licensing information,
copyright notices and attributions.
