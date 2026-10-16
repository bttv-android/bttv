# Twitch 31.5.2

Since Twitch moved the stream page and the chat to React Native, the Java chat classes the patches in `patches/` target are still part of the app, but not used anymore. The scripts in this directory build the mod for Twitch 31.5.2 in a different way: a few calls to `bttv.api.Hooks` are inserted below React Native, where chat messages, images and requests pass through Java.

| Hook | What the mod does there |
| --- | --- |
| `TwitchApplication.onCreate` | gets the application context, loads the global emotes |
| incoming websocket text | adds BTTV, FFZ and 7TV emotes to the `emotes` tag of chat messages, with the id `BTTV-<id>` |
| image source | replaces the urls of emotes with a `BTTV-<id>` id |
| http request | answers the `EmoteCard` request for our emotes, adds our emotes to the `ChatEmoteSets` answer (emote menu) |
| text layout | wording of the emote card, wider slot for wide emotes |
| image view layout | makes the image view of a wide emote as wide as its slot |
| chat input (`com.twitchrn.emoteinput`) | url, width and animation of our emotes in the input box, images are read from the chat's disk cache (Fresco) |

Not ported yet: settings, badges and everything else the mod did before. All providers are always enabled.

## Build

The resources are not rebuilt and the split apks are not merged, apktool's output for this version crashes. Only the code of the base apk is changed, then the base apk and the original splits are signed with the same key.

1. Get the Twitch 31.5.2 bundle (e.g. the `.apkm` from apkmirror) and put it in the root of the repo as `twitch.zip`.
2. Start the builder with the repo mounted and copy the scripts:
   ```
   docker run -d --name bttv --entrypoint sleep -v "$(pwd)":/repo ghcr.io/bttv-android/builder infinity
   docker exec bttv bash -c 'cp /repo/v31/*.sh /usr/build/'
   ```
3. Once per bundle: `docker exec bttv bash /usr/build/setup.sh`
4. After every change: `docker exec bttv bash -c 'cd /usr/build && bash apply_hooks.sh && bash build_mod.sh'`
5. Install the base apk and the splits that match your device:
   ```
   adb install-multiple -r dist/splits/base.apk dist/splits/split_config.arm64_v8a.apk dist/splits/split_config.xxhdpi.apk
   ```

The mod is its own app, it can be installed next to the official one. By default it is called `BTTV` with the package `tv.twitch.bttvandroid.app`, like the releases of this project. A build signed with another key can not replace an installed one, so use another package during development:

```
docker exec -e BTTV_PACKAGE=tv.twitch.bttvandroid.dev -e BTTV_NAME=BTTV2 bttv bash -c 'cd /usr/build && bash build_mod.sh'
```

`RenamePackage.java` changes the package in the compiled manifests and resource tables of the base apk and the splits. Parts of the app that use the package name of the official app directly (and not the one of the installed app) can break.

## New Twitch version

The classes with short names in `apply_hooks.sh` are obfuscated and get new names with every release. The comments in the script say what each of them is, most can be found from classes of React Native that keep their names (`WebSocketModule`, `ReactImageManager`, `NetworkingModule`, `SurfaceMountingManager`).

Before building on a hook, check that it is called (e.g. with a build that only logs), code that is part of the app is not necessarily used.
