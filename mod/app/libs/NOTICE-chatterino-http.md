# Private Chatterino HTTP runtime

Narrow Java-only transport. No public Kotlin compiler/reflection compatibility is
promised. Twitch and the application's other networking implementations are not
replaced. The isolated artifact is not a Chatterino client or artwork bundle.

Runtime: Square OkHttp 4.12.0, Square Okio JVM 3.6.0, JetBrains Kotlin
stdlib/stdlib-jdk7/stdlib-jdk8/stdlib-common 1.9.10, JetBrains annotations 13.0.
These components are Apache-2.0 licensed. Original component manifests, notices,
Kotlin modules/builtins/common metadata and OkHttp's public suffix database and
associated NOTICE are retained at provider-private resource paths. Exact paths,
input hashes and resource hashes are in the adjacent provenance JSON.
The three original Kotlin JPMS descriptors are retained byte-identically as
`META-INF/bttv-badgehttp-components/<component>/versions/9/module-info.class.metadata`.
They are provenance resources, not Android executable classes. The provenance
records original component/path, output path and both byte hashes.

Reproduction: retrieve the exact Maven Central artifacts named by
`tools/chatterino-http-inputs.json` (including the build-only ASM 7.0 and official
Kotlin metadata 2.2.21 tools). Compile `tools/prepare_chatterino_http.java` with
those build tools on the classpath using JDK 11. Run `prepare_chatterino_http`
with a NEW output path followed by the seven runtime inputs in manifest order.
Repeat independently, then compare bytes. The tool verifies all input/tool hashes,
the fixed model schema through pinned tool bytes, path/duplicate/size constraints,
1422 classes, 1347 strict metadata round trips, 6 module round trips, no stale
physical metadata class references, and the exact expected final SHA-256.
Unrecognized versions or output bytes fail closed; no shrinking is performed.

Metadata transformation uses reflection only over the hash-pinned official build
tool model. No reflection-based metadata manipulation is shipped in the app.
The Kotlin 2.2.21 build tool/runtime MUST NOT be included as an app dependency.

API availability, service usage and Chatterino artwork/distribution terms remain
a final-release review item. No endorsement or public distribution is implied.
