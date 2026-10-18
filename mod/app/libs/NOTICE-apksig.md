# Pinned Android APK verifier

`apksig-sdk30.0.3.jar` contains only the `com/android/apksig/` library classes
from the existing official Android SDK Build Tools 30.0.3 `lib/apksigner.jar`.
No CLI, external downloader, private key or signing configuration is included.
All library classes are retained rather than risk an incomplete hand-pruned verifier.

Origin: official Android SDK Build Tools 30.0.3 (Linux), already provisioned in
the project's `bttv-sdk-recovery-30` SDK volume. Upstream AOSP:
https://android.googlesource.com/platform/tools/apksig/

Original SDK JAR SHA-256:
`4295EF085C0DE5CBD5D8E4C73126E948E20D3126DB26A4F60125864C15F8B8CB`

Vendored library JAR SHA-256:
`ADDF02D5B6867CCD87FF00A1FD2AF4F65C0D4F7EB5EA6189EA6E144ADD52CE2C`

Size: 443,724 bytes. D8 min-API21 preflight: 1,847 method references before
overlap with the existing app (26,083 spare conservative DEX7 references).
It is necessary here to verify signature bytes instead of misrepresenting
PackageManager's archive metadata as cryptographic verification. Only the
verifier API is invoked. Unsupported/failed signatures fail closed; Android's
installer remains the final platform/rotation/policy authority. No large
networking, UI or new signature implementation is added.

Copyright (C) The Android Open Source Project. Licensed under Apache License 2.0;
see `../src/main/res/raw/bttv_apksig_license.txt` (also packaged with the APK).
No upstream library code is modified.
