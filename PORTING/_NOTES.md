# Twitch 31.2.0 porting notes

## Stage 1 input

- Source: `C:\BTTV\inputs\twitch-31.2.0.apkm`
- Working copy: `C:\BTTV\bttv\twitch.zip`
- SHA-256 (both files): `7F03175FDFBCD597940381F8F0A88F6377E9F6BD984CDA76F17D93813D0DB02C`
- APKMirror format version: 5
- Package: `tv.twitch.android.app`
- Version name: `31.2.0`
- Version code: `3102006`
- Variant: `arm64-v8a`, 480-640 dpi, Android 10+ (`min_api` 29)

The package and version values above come from the APKMirror `info.json` included
in the bundle. A manifest-level `aapt` check is still part of the Docker
disassembly verification because Docker was not available on this task's shell.

## Bundle structure

APK payloads:

| File | Size (bytes) | Purpose |
| --- | ---: | --- |
| `base.apk` | 106,316,228 | Base application; contains six DEX files and the non-density-specific resources |
| `split_config.arm64_v8a.apk` | 54,271,134 | ARM64 configuration; contains 38 `lib/arm64-v8a` native-library entries |
| `split_config.xxhdpi.apk` | 5,751,446 | xxhdpi (480 dpi) resource configuration; contains 326 resource entries |
| `split_config.xxxhdpi.apk` | 6,505,193 | xxxhdpi (640 dpi) resource configuration; contains 326 resource entries |

All four APKs are required inputs when producing the project's fused APK for
this APKMirror variant. The two density splits cover the variant's advertised
480-640 dpi range. There are no separate language configuration APKs in this
bundle; `info.json` lists the packaged languages.

Other archive entries:

- `info.json`
- `icon.png`
- `APKM_installer.url`
- `META-INF/MANIFEST.MF`
- `META-INF/APKMIRRO.SF`
- `META-INF/APKMIRRO.RSA`

## Existing input and disassembly behavior

- `initworkspace` calls `disassemble` before compiling BTTV source, applying
  patches, or rebuilding the APK.
- `disassemble` accepts either `twitch.zip` or `twitch.apk`. For a ZIP it
  extracts the archive into `.tmp`; only top-level `*.apk` files are verified
  and decoded, so APKMirror metadata files are harmless and are discarded with
  `.tmp` afterward.
- It recognizes `base.apk` (as used by this bundle) and the older
  `tv.twitch.android.app.apk` base name.
- Each split is decoded separately. Split outputs are copied into the decoded
  base without overwriting base files, then `build-companion fix` and
  `build-companion yml` reconcile split resource metadata and `apktool.yml`.
- The cache key is derived from the sorted SHA-256 hashes of the extracted APK
  files, not from archive metadata. This makes the 31.2.0 input reproducible and
  invalidates the prior-version disassembly cache.
- A single `twitch.apk` is copied to `.tmp/tv.twitch.android.app.apk`; APK
  bundles are not converted to that filename before decoding.

## Stage 1 tooling changes

- No disassembly-script change was necessary: the existing `twitch.zip`,
  `base.apk`, and split-merging paths already match this APKMirror bundle.
- `twitch.zip` was created as an ignored working copy of the original APKM and
  verified byte-for-byte by SHA-256. The original input was not modified.
- The existing Compose file mounts `twitch.apk`, so the Stage 1 verification
  command uses a direct read-only `twitch.zip` bind mount instead.
- No BTTV source, feature patch, manifest, or generated disassembly was changed.

## Stage 2 handoff

- Start from `base.apk` plus all three configuration splits listed above.
- Do not treat the two density APKs as alternate source downloads; both belong
  to this APKMirror 480-640 dpi variant and are inputs to the current fusing
  pipeline.
- The 31.2.0 base has six DEX files. The existing tooling later creates
  `smali_classes9` for BTTV output; compatibility with that old layout belongs
  to the feature-port/build stage, not Stage 1.
- The first container verification should invoke `./disassemble` directly. Do
  not run `initworkspace`, because it would compile BTTV source and attempt the
  old 19.0.1 feature patches.
