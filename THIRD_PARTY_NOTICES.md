# Third-party notices

The original BTTV Android source retains its [MIT license](LICENSE). Incorporated
third-party components retain their own licenses; neither that license nor this
document licenses Twitch's proprietary application code or remote emote artwork.

The complete shared Apache License 2.0 text and AOSP attribution are retained in
[bttv_apksig_license.txt](mod/app/src/main/res/raw/bttv_apksig_license.txt).
Component-specific notices and additional license texts are bundled in
[bttv_third_party_notices.txt](mod/app/src/main/res/raw/bttv_third_party_notices.txt).
Both resources are included in built APKs. Preserve them when redistributing.

## Incorporated components

| Component | Version | License / source and attribution |
| --- | --- | --- |
| AOSP APK signature verifier | SDK Build Tools 30.0.3 subset | Apache-2.0; [existing provenance and notice](mod/app/libs/NOTICE-apksig.md), [AOSP source](https://android.googlesource.com/platform/tools/apksig/) |
| Square OkHttp | 4.12.0 | Apache-2.0; [pinned source](https://github.com/square/okhttp/tree/4984568367caaf359b82c452bd28b5e192824d1c), [private runtime notice](mod/app/libs/NOTICE-chatterino-http.md) |
| Square Okio JVM | 3.6.0 | Apache-2.0; [source](https://github.com/square/okio/tree/parent-3.6.0) |
| JetBrains Kotlin stdlib, jdk7, jdk8 and common runtime | 1.9.10 | Apache-2.0; [source](https://github.com/JetBrains/kotlin/tree/v1.9.10) |
| JetBrains annotations | 13.0 | Apache-2.0; [artifact metadata](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.pom) |
| Public Suffix List data in OkHttp | Snapshot specified below | MPL-2.0; notice and preferred source below |
| Glide, gifdecoder, disklrucache and annotations | 4.12.0 | Composite BSD-style, MIT and Apache-2.0 terms; [complete upstream license](https://github.com/bumptech/glide/blob/v4.12.0/LICENSE), reproduced in the bundled notice |
| GlideWebpDecoder | 2.0.4.12.0 | Apache-2.0 wrapper, Copyright 2017-2018 zjupure; [pinned source](https://github.com/zjupure/GlideWebpDecoder/tree/989e6a4ab7db933c7685d40ee1047fcddeb4c4e5) |
| libwebp in GlideWebpDecoder | Provider snapshot identifies 1.1.0 | Google BSD-style terms and additional patent grant; [COPYING](https://github.com/webmproject/libwebp/blob/v1.1.0/COPYING), [PATENTS](https://github.com/webmproject/libwebp/blob/v1.1.0/PATENTS) reproduced in the bundled notice |
| AndroidSVG | 1.4 | Apache-2.0, Copyright 2013-2018 Cave Rock Software Ltd; [pinned license](https://github.com/BigBadaboom/androidsvg/blob/9490d751d91033051efc6a4a9835ceff6082e9ff/LICENSE) |
| AndroidX libraries and Material Components | Resolved consumer inventory below | Apache-2.0; Android Open Source Project / respective contributors; [AndroidX license](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [Material license](https://github.com/material-components/material-components-android/blob/1.4.0/LICENSE) |

Glide's composite notice retains Google 2014, Jake Wharton 2012, Android Open
Source Project 2011, Xcellent Creations 2013, and the additional encoder notices,
including Anthony Dekker 1994. Retaining the whole upstream text does not claim
that every optional encoder implementation is executed in this application.

## Private runtime modifications and provenance

The private Chatterino badge HTTP runtime relocates library namespaces and Kotlin
metadata under `com.bttv.badgehttp`. Three inert JPMS descriptors are retained as
non-executable provenance resources. Input component manifests and the PSL
NOTICE/data are retained. These are not original BTTV library implementations.
No Chatterino desktop-client source or badge artwork is included in that JAR.

Exact versions and input/output SHA-256 values:
[chatterino-http-inputs.json](tools/chatterino-http-inputs.json) and
[adjacent provenance JSON](mod/app/libs/chatterino-http-isolated-4.12.0.provenance.json).
Transformation source: [prepare_chatterino_http.java](tools/prepare_chatterino_http.java).
The AOSP verifier is an unmodified class subset; its separate notice records the
official SDK origin. Glide/decoder/AndroidSVG artifacts are used without a local
library-source fork. The packaged decoder binaries match the declared AAR; a
reproducible native compilation from that source snapshot is not asserted.

## Public Suffix List

The retained upstream resource notice says:

> Note that publicsuffixes.gz is compiled from The Public Suffix List:
> https://publicsuffix.org/list/public_suffix_list.dat
>
> It is subject to the terms of the Mozilla Public License, v. 2.0:
> https://mozilla.org/MPL/2.0/

The exact corresponding preferred source is
[public_suffix_list.dat at OkHttp commit 4984568367caaf359b82c452bd28b5e192824d1c](https://github.com/square/okhttp/blob/4984568367caaf359b82c452bd28b5e192824d1c/okhttp/src/test/resources/okhttp3/internal/publicsuffix/public_suffix_list.dat).
SHA-256: `E8B273972EB5A70E888BD3E7D7C5B9B04E600A59D69DEF9136A74D70AE6FCDD3`.
Its rules reproduce the bundled compiled data. The resource remains at
`com/bttv/badgehttp/okhttp3/internal/publicsuffix/publicsuffixes.gz` with its NOTICE.
The MPL applies to this covered data, not automatically to the entire app or JAR.
See [MPL 2.0](https://www.mozilla.org/en-US/MPL/2.0/) for its terms.

## Build tools and external content

ASM and Kotlin metadata tooling used to prepare the private runtime are build-only
components, recorded separately in its input manifest. They are not additional
APK runtime dependencies. APKEditor, Apktool, Baksmali, Gradle, Android SDK tools
and the signing tool are independently supplied development tools; this file
does not relicense them or imply their source is embedded in the application.

Provider API access, remote emote/badge artwork, provider trademarks and modified
Twitch binary redistribution require their own applicable rights/terms review.
Public endpoints do not grant blanket artwork redistribution permission. A
research acknowledgment does not replace these notices or grant code permissions.

## Resolved AndroidX and Material inventory

The consumer `releaseRuntimeClasspath` resolves AppCompat to 1.3.0 and Material
to 1.4.0 (the app/stub modules request older versions). The following Apache-2.0
AndroidX artifacts have class definitions present in the injected DEX. Versions
come from actual Gradle resolution and cached artifact/POM inspection, not merely
the direct declarations. Some native Twitch definitions may also exist; this
inventory does not claim every artifact is newly copied wholesale.

| AndroidX artifact | Resolved version |
| --- | --- |
| `androidx.activity:activity` | 1.2.3 |
| `androidx.annotation:annotation` | 1.2.0 |
| `androidx.annotation:annotation-experimental` | 1.0.0 |
| `androidx.appcompat:appcompat` | 1.3.0 |
| `androidx.appcompat:appcompat-resources` | 1.3.0 |
| `androidx.arch.core:core-common` | 2.1.0 |
| `androidx.arch.core:core-runtime` | 2.1.0 |
| `androidx.cardview:cardview` | 1.0.0 |
| `androidx.collection:collection` | 1.1.0 |
| `androidx.constraintlayout:constraintlayout` | 2.0.1 |
| `androidx.constraintlayout:constraintlayout-solver` | 2.0.1 |
| `androidx.coordinatorlayout:coordinatorlayout` | 1.1.0 |
| `androidx.core:core` | 1.5.0 |
| `androidx.cursoradapter:cursoradapter` | 1.0.0 |
| `androidx.customview:customview` | 1.0.0 |
| `androidx.documentfile:documentfile` | 1.0.0 |
| `androidx.drawerlayout:drawerlayout` | 1.0.0 |
| `androidx.dynamicanimation:dynamicanimation` | 1.0.0 |
| `androidx.exifinterface:exifinterface` | 1.2.0 |
| `androidx.fragment:fragment` | 1.3.4 |
| `androidx.interpolator:interpolator` | 1.0.0 |
| `androidx.legacy:legacy-support-core-utils` | 1.0.0 |
| `androidx.lifecycle:lifecycle-common` | 2.3.1 |
| `androidx.lifecycle:lifecycle-livedata` | 2.0.0 |
| `androidx.lifecycle:lifecycle-livedata-core` | 2.3.1 |
| `androidx.lifecycle:lifecycle-runtime` | 2.3.1 |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.3.1 |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.3.1 |
| `androidx.loader:loader` | 1.0.0 |
| `androidx.localbroadcastmanager:localbroadcastmanager` | 1.0.0 |
| `androidx.print:print` | 1.0.0 |
| `androidx.recyclerview:recyclerview` | 1.1.0 |
| `androidx.savedstate:savedstate` | 1.1.0 |
| `androidx.tracing:tracing` | 1.0.0 |
| `androidx.transition:transition` | 1.2.0 |
| `androidx.vectordrawable:vectordrawable` | 1.1.0 |
| `androidx.vectordrawable:vectordrawable-animated` | 1.1.0 |
| `androidx.versionedparcelable:versionedparcelable` | 1.1.1 |
| `androidx.viewpager2:viewpager2` | 1.0.0 |
| `androidx.viewpager:viewpager` | 1.0.0 |

Material Components `com.google.android.material:material:1.4.0` is Apache-2.0.
Applicable license copies and component notices accompany the source and APK as
described above; the inspected artifacts do not supply additional standalone
NOTICE files. No copyright years are inferred from POM inception years.

The legacy Twitch compile-stub graph also resolves Dagger 2.35.1, javax.inject 1,
OkHttp 4.0.1/Okio 2.2.2, Kotlin 1.3.40, RxAndroid 2.1.1/RxJava 2.2.6,
reactive-streams 1.0.2, Markwon 2.0.2 and commonmark 0.12.1. Their original-package
class definitions are not injected into DEX7 by the production namespace-copy
step. They must not be confused with the separately shipped relocated HTTP
runtime above. This is not an audit of every library already inside Twitch.
If future packaging starts copying those other namespaces, reconcile their
specific licenses/notices before distributing them (including commonmark's
retained license and reactive-streams' CC0 terms).
