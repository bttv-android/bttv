# Build validation and release preparation

This document describes maintainer gates, not permission to publish a release.
The application remains `tv.twitch.bttvandroid.app`, version `0.43.2-beta`,
versionCode `1208000338`, minSdk 29 and targetSdk 36. No version bump is implied.

## Reproducible inputs and isolated builds

Follow the [Docker or manual instructions](README.md#build-it-yourself).
`tools/prepare_twitch_input.py` checks the five root-level APK payloads against
the pinned hashes in `tools/twitch-31.2.0-inputs.json`. It does not download them
or accept arbitrary APKMirror variants just because their versionName matches.
Its optional `--armv7-bundle` selects only the pinned ARMv7 APK from a second
bundle while preserving all four pinned primary APKs. It never substitutes the
secondary base or density/ARM64 components.
Keep Twitch inputs, decoded trees, APK exports, credentials and caches out of Git.

`tools/build_production.py` copies tracked production inputs, plus its explicit
infrastructure inputs, into a new workspace. It excludes temporary notification,
purchase and other untracked diagnostics. Windows shell line endings are normalized
only in that staged copy. The normal `initworkspace` flow runs there, with a
fresh disassembly and writable Gradle project. The checkout is not a build target.

Docker Compose selects `linux/amd64` for the Linux SDK/build-companion executables
and builds the local Java 11 toolchain instead of assuming a previously
published upstream image contains this port. It installs SDK Build Tools 30.0.3
and android-30, Apktool 2.9.3, APKEditor 1.4.9, Uber APK Signer 1.2.1, Baksmali
2.4.0 and build-companion 5.2.0. The JAR/executable hashes are checked in Dockerfile.
Gradle 7.2 and AGP 4.1.3 remain unchanged. The mod's compilation SDK is distinct
from the patched application's minSdk/targetSdk values.

Manual Linux/WSL builds require SDK and build-tool installation paths without
spaces or other whitespace because the inherited scripts do not support them.
Choose these paths before configuring the tools; the standard Docker paths are unchanged.

Fresh toolchain/dependency downloads require their official repositories to be
reachable. Existing dependency caches may be reused; decoded/patched caches must
not substitute for a fresh build. Export destinations must be new. Never overwrite
an accepted APK/archive while validating infrastructure.

## Local checks and artifact verification

```bash
python3 tests/stage9_infrastructure.py
for script in initworkspace disassemble buildsource build patch tools/docker_build.sh; do bash -n "$script"; done
git diff --check
```

The infrastructure suite checks input validation, original README assets/credits,
notice coverage, patch syntax, build isolation and release metadata failure cases.
It is not a replacement for the feature JVM suites or physical-device validation.

The build wrapper verifies the completed APK before copying it to the requested
output directory. `tools/verify_release_apk.py` uses SDK `aapt`, `apksigner` and
`zipalign`, plus independent ZIP CRC/duplicate, DEX checksum/pool and native ELF/
alignment checks. It requires seven contiguous DEX files, the dual-ARM 39+39
native sets, unchanged original Twitch native payloads, the permanent package/
version/SDKs, nine distinct providers and bundled notice resources. It rejects
temporary notification instrumentation. Smali assembly is not an ART runtime test.

## Signing continuity — decision required before public distribution

The accepted experimental APKs use the publicly bundled signing-tool debug identity:

`1E08A903AEF9C3A721510B64EC764D01D3D094EB954161B62544EA8F187B5953`

That key is not a secure public-release publisher identity. A mathematically valid
signature does not establish exclusive key custody. Local builds without `KS`
explicitly remain test builds. The artifact verifier permits that certificate only
with `--allow-test-key`; release metadata generation refuses it.

Maintainers must select and securely provision an approved private signing identity
or the existing official release identity. This repository does not contain or
create a private release key. `KS` is a readable external keystore path; `KSPASS`
is supplied privately through the environment. The existing signer invocation uses
alias `bttv-android`. `EXPECTED_SIGNER_SHA256` pins the intended candidate certificate.
Do not place secrets in Git, README commands, logs or public artifacts.

Android upgrades and the in-app updater require signing compatibility. The current
updater additionally requires exact installed/downloaded certificate equality.
A new private certificate cannot transparently update existing debug-signed test
installations. Decide and document a user-controlled transition separately; do not
relax verification, rotate keys silently, or promise data-preserving overlays.

## CI boundaries and maintainer provisioning

PR checks receive no release secrets and do not publish anything. The Docker
workflow builds/checks tooling only. The optional signed-candidate job is restricted
to a manual dispatch on the original repository's `master` branch. It never creates
a tag or GitHub release, and has only `contents: read` permission.

The job references the existing `KEYSTORE` and `KSPASS` secrets. Maintainers must
separately configure `TWITCH_31_INPUT_URL` (a permitted HTTPS source for the exact
approved input) and `RELEASE_SIGNER_SHA256` (the approved certificate fingerprint).
No values/secrets are created by this change. Missing configuration fails closed.
No Twitch input is committed or hosted by this repository as part of these changes.
Ensure any runner retrieval/artifact distribution is permitted before dispatching.

The workflow creates an ephemeral runner keystore, mounts it read-only into the
builder, verifies the candidate signer and exports only the APK/verification JSON.
It removes the runner key in an always-run cleanup step. It does not publish a
Docker image, expose signing to PR code, or upload source caches/signing inputs.
Maintainers should also apply their normal branch/environment protection policy.

## Future updater-compatible release metadata

Automatic and manual checking use the same fixed endpoint:

`https://api.github.com/repos/bttv-android/bttv/releases/latest`

The updater excludes drafts and GitHub `prerelease: true` releases. A `-beta`
version suffix alone does not determine GitHub's prerelease flag. Metadata checks
never install an APK; downloading and installation require user confirmation.

Before a future release, obtain explicit approval for a versionName/tag and a
strictly increasing versionCode. The present name/tag already refers to an old
upstream release and the current code must not be reused as a newer update. Do
not infer approval for `0.44` or choose a new code automatically.

After an approved APK has been uploaded to an authorized draft release, save the
actual release API response as `release.json`. Only then is the final asset ID
known. The local-only metadata tool independently re-verifies the actual APK:

```bash
python3 tools/release_metadata.py --help
```

Supply `--apk`, `--input`, `--expected-signer`, `--release-json`,
`--previous-code` and a new `--output` path. The previous code is the highest
supported installed code, not an invented smaller value. The tool emits one visible
`bttv-update` fenced JSON block and performs no network or publishing action.
It refuses test signing, reused codes, mismatched tag/version, ambiguous assets,
incorrect size/hash and off-origin asset URLs. If GitHub does not provide an asset
digest, independently confirm that the uploaded bytes match the verified local
APK before publication. Preserve ordinary release notes
outside that single block; the uploader must not substitute placeholder asset IDs.

The block includes schemaVersion, standalone, packageName, versionName,
versionCode, releaseTag, signerSha256, minSdk, supportedAbis, twitchVersion,
assetId, assetName, sizeBytes and sha256. The existing app verifies the metadata
and downloaded APK independently. Run the actual updater parser tests and review
the final release JSON before an independently authorized publication. A candidate
artifact or source merge is not proof that a future update installs successfully.

## Notices and remaining release gates

Preserve [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), the two existing library
NOTICE files, provenance records and both bundled license resources. Verify the
actual exported APK contains them and retains the PSL NOTICE/data. The preferred
PSL source URL/hash and relevant component-specific notices are supplied there.

Notice implementation does not itself settle proprietary Twitch binary
redistribution, provider artwork/service/trademark terms, or all jurisdictional
legal questions. Review those with maintainers before public distribution.
Complete final device coverage (including supported ARMv7/API29 targets and an
authorized same-signer newer-release update), and make signing/versioning decisions
before publication. Historical test observations must not be promoted to universal
device acceptance.

Correction of identities on existing port commits is separately deferred. Do not
rewrite upstream contributors/history as part of release preparation. Any later
authorized rewrite needs a recoverable backup and tree/message/date preservation.
