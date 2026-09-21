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
- At the Stage 1 boundary, container verification invoked `./disassemble`
  directly because `initworkspace` would still have attempted the old 19.0.1
  patches. Stage 2 retires those active feature patches before its build check.

## Stage 2 foundation port

Stage 2 ports only the common bootstrap and resource substrate. The active
`patches` tree now contains the eight Twitch 31.2.0 foundation patches; the
74 Twitch 19.0.1 feature patches were removed from the active tree and remain
available in Git history for later staged ports.

### Manifest mapping

- The application package remains the Twitch 31.2.0 value
  `tv.twitch.android.app`. The manifest patch adds BTTV version
  `0.43.2-beta` / version code `01208000338` from `bttv.manifest.json` without
  reviving the obsolete 19.0.1 side-by-side package rename.
- Package-derived permissions and provider authorities remain on
  `tv.twitch.android.app`, matching the root package. Component class names and
  the `amzn` callback host likewise remain unchanged.
- The 31.2.0 manifest adds required split metadata and several providers that
  did not exist in 19.0.1; the new package-derived authorities were included.
- Updater-only permission, activity, provider, and job-service declarations were
  deliberately not carried forward in this stage.

### Application/bootstrap mapping

- `TwitchApplication` moved from `smali_classes5` to the primary `smali` tree
  and is now heavily obfuscated. Its `b()` call performs injection and invokes
  `Application.onCreate()`. The BTTV context hook is placed immediately after
  that call and invokes `bttv.Data.setContext(Context)`.
- `Data.setContext` now stores the application context only. Badge loading and
  updater scheduling were removed from bootstrap so Stage 2 does not activate
  those feature groups; later feature stages must add their own initialization
  when ported.
- The old `ApplicationDelegate$Companion` package-name override has no direct
  31.2.0 mapping: that class is gone, Firebase startup moved to
  `TwitchFirebaseProvider` / the obfuscated Firebase bootstrap, and the
  equivalent path no longer reads the application package name. The obsolete
  patch was removed rather than attached to an unrelated package-name read.

### Resource mapping

- `bttv_mega` and `bttv_sonic` are appended after the 31.2.0 color table. The
  Twitch splash theme uses `bttv_mega`.
- The adaptive launcher resource moved from
  `res/mipmap-anydpi-v26/ic_launcher.xml` to
  `res/mipmap-anydpi/ic_launcher.xml`; the BTTV background override follows it.
- Existing `buildsource` behavior continues to merge BTTV strings, localized
  strings, XML, layouts, drawables, launcher bitmaps, native libraries, and
  contributor metadata. This is resource plumbing only; no feature hook is
  active in Stage 2.
- Public resource IDs were regenerated above the 31.2.0 maxima:
  IDs `0x7f0b17d8`-`0x7f0b17f2`, layouts `0x7f0e055e`-`0x7f0e0561`, colors
  `0x7f060737`-`0x7f060738`, strings `0x7f1518e4`-`0x7f15191b`, and drawable
  `0x7f080a89`. The four standalone IDs remain declared in `ids.xml`.

### Verification and later-stage notes

- Each foundation patch applies cleanly to the cached Twitch 31.2.0 base.
  Modified manifest/resource XML parses successfully, and the new public IDs
  do not collide with the 31.2.0 table.
- Local Gradle verification is unavailable on the Windows host because Java is
  not installed or configured; Docker is also unavailable in this shell. A
  single Stage 2 Docker build should be used for final APK/resource-linking
  verification.
- Stage 3 must port feature patches from Git history to 31.2.0 semantics rather
  than restoring the removed files wholesale. It should also exercise login to
  confirm that the removed legacy package-name override is no longer needed.

### Stage 2 Docker manifest correction

- The first full Stage 2 Docker run reached patching but failed on
  `AndroidManifest.xml.patch`. The cached decoded targets had been materialized
  on Windows with CRLF endings, while the committed patches correctly use LF.
  Docker's Linux Git configuration does not perform Windows line-ending
  conversion, so `git apply --index` treated the carriage returns as content
  and rejected the first manifest hunk. Reproducing the check with
  `core.autocrlf=false` fails at line 1; the same content with normalized target
  endings applies cleanly.
- `AndroidManifest.xml.patch` was regenerated from the current post-processed
  Twitch 31.2.0 manifest and preserves the source manifest's missing final
  newline. The patch contains LF only and no carriage-return bytes.
- The patch runner now derives each active patch's target path and strips CR
  only from those decoded target files before creating the Git index. This
  avoids altering unrelated decoded content and makes LF-only patches portable
  when the cache crosses Windows and Linux. Compose mounts the corrected local
  runner into the builder container.

### Stage 2 Apktool package correction

- The next Docker run applied every foundation patch, then Apktool reported a
  missing root `package` attribute. The post-patch root had placed
  `android:versionCode`, `android:versionName`, and the package on one line.
  Before Apktool runs, `build` uses the legacy greedy expression
  `android:versionCode=".*"`; on that one-line root it consumed the remaining
  attributes through the last quote, including `package`.
- The manifest patch now expands only the root tag across separate LF lines,
  adds the two BTTV version attributes, and preserves
  `package="tv.twitch.android.app"`. All Twitch permissions, providers,
  authorities, components, and callback hosts remain consistent with that
  official package; the prior 19.0.1 package/authority rename is not ported.
- The actual post-patch manifest and a simulation of the build-time version
  substitutions both parse as XML and retain a root `manifest` element with
  `package="tv.twitch.android.app"`.

### Stage 2 resource-table correction

- The subsequent Docker run applied all foundation patches and reached Apktool
  resource linking, where AAPT2 rejected Material resources that reference
  `state_dragged`, `state_swiped`, `state_with_icon`, and `widthChange`.
- This is not a Stage 2 patch regression or a split-merging loss. The same XML
  references exist in the pristine 31.2.0 base, none of the four declarations
  exists in its decoded `attrs.xml` or `public.xml`, and none is supplied by the
  ABI or density splits. The Stage 2 patches did not remove or rename them.
- Twitch 31.2.0's compiled binary XML retains the original attribute IDs even
  though its optimized resource table omits the declarations. Apktool 2.9.3
  decodes those references to names but does not reconstruct the stripped
  declarations, so an unmodified decode/rebuild fails for the same reason.
- The resource patch set now restores the real Material declarations and their
  exact IDs recovered from the pristine compiled XML resource maps:
  `state_dragged` (`0x7f040661`, boolean), `state_swiped` (`0x7f040666`,
  boolean), `state_with_icon` (`0x7f040667`, boolean), and `widthChange`
  (`0x7f0407af`, dimension or fraction). A scan of every binary XML resource in
  the pristine base found no other referenced app attributes missing from the
  decoded public table. No Twitch Material resource was removed and no ID was
  invented.

## Stage 3 settings and settings navigation

### Twitch 19.0.1 behavior

- `MainSettingsPresenter.updateSettingModels()` appended a `SubMenuModel`
  labelled `BTTV` to Twitch's main settings list and targeted the custom
  `SettingsDestination.BTTV` enum constant.
- `MainSettingsPresenter$navController$1` recognized that destination and
  constructed `bttv.settings.SettingsFragment`.
- `SettingsFragment` and its custom Dagger binding supplied
  `SettingsPresenter`, a `BaseSettingsPresenter` subclass that translated the
  BTTV `Settings` enum into Twitch `ToggleMenuModel`, `DropDownMenuModel`, and
  `SubMenuModel` rows.
- Four additional injected `SettingsDestination` values dispatched highlight,
  blacklist, credits, and updater actions. The separate
  `SettingsPreferencesController.SettingsPreference.BTTVEmotesEnabled` enum
  value supported the old toggle pipeline.

### Twitch 31.2.0 mapping and architecture change

- The old named `SettingsDestination` class is gone. Its current destination
  role is the obfuscated `b6z` enum, whose named values still include
  `Settings`, `Preferences`, `Credits`, and other destinations.
- `SettingsActivity` remains the host and continues to switch `b6z`
  destinations into fragments. Its current main destination selects between
  legacy `MainSettingsFragment`/`ffo` and, by default, the Compose-based
  `MainSettingsFragmentV2`.
- `MainSettingsFragmentV2.onCreateView()` creates a `ComposeView`; its injected
  `hfo` router handles current settings destinations. There is no safe direct
  equivalent of the old `MainSettingsPresenter.updateSettingModels()` list
  insertion or its fragment-factory lambda.
- Stage 3 therefore uses `SettingsActivity.onCreateOptionsMenu(Menu)` as the
  stable current hook. It adds one visible `BTTV` toolbar action without
  changing Twitch destinations, Compose state, Hilt bindings, or existing menu
  actions. The action opens a manifest-registered, non-exported
  `bttv.settings.SettingsActivity` by explicit intent.
- The obsolete BTTV `BaseSettingsFragment`/`BaseSettingsPresenter` integration,
  its Dagger binding helpers, and its Twitch-menu-model adapters were removed.
  BTTV link rows now use the internal `SettingsAction` enum rather than
  pretending to be Twitch destinations.

### Implemented settings surface

- `bttv.settings.SettingsActivity` builds its rows from the existing BTTV
  `Settings` and `UserPreferences` data model. It uses the existing `BTTV`
  `SharedPreferences` file and preserves all existing preference keys and
  defaults.
- The screen includes boolean rows for BTTV/FFZ/7TV emotes, automatic channel
  points, deleted messages, split chat, sleep timer, and anonymous chat; the
  GIF render-mode list; highlight and blacklist editors; and credits.
- Highlight and blacklist links reuse their existing dialogs and string-set
  storage. Credits reuses the existing contributor dialog. Updater controls
  and developer-tools controls are deliberately hidden because those runtime
  feature groups are outside Stage 3.
- Existing localized BTTV strings and the Stage 2 resource-merge pipeline are
  reused. The highlight/list/credits layouts and their fixed public IDs remain
  unchanged; `bttv_blacklist_dialog` continues to be copied by `buildsource`
  and assigned by AAPT as in the successful Stage 2 build. No new public
  resource ID or arbitrary menu-item ID was introduced. Runtime colors are
  resolved from Twitch 31.2.0's `background_base`, `background_body`,
  `text_base`, and `text_alt` resources with safe fallbacks.

### Later-stage boundary and verification

- The visible feature controls persist their values, but their behavioral
  hooks are not part of Stage 3. In particular, emote rendering, GIF behavior,
  channel-points redemption, deleted-message display, split chat, sleep timer,
  anonymous chat, highlighting, and blacklist filtering remain unported until
  their respective later stages. The settings screen states this boundary.
- Credits and highlight/blacklist list editing are functional settings-surface
  behavior. The updater is neither invoked nor registered by Stage 3.
- Both Stage 3 patches apply cleanly to the cached 31.2.0 base. The manifest
  activity uses the existing `Theme.TwitchApp`; all referenced Twitch colors
  and the theme exist in the 31.2.0 public resource table.
- The Windows host still has no Java runtime, so the new Java source cannot be
  compiled locally. A single direct builder run is required for final D8,
  Apktool, zipalign, and signing verification; do not use the Compose service.
