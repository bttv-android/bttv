FROM eclipse-temurin:11-jdk-jammy

RUN apt-get update && apt-get install --no-install-recommends -y \
    bash ca-certificates curl git jq python3 rsync unzip zip && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /opt
ENV ANDROID_SDK_ROOT=/opt/android-sdk-linux \
    ANDROID_HOME=/opt/android-sdk-linux \
    ANDROID_SDK=/opt/android-sdk-linux

# This command-line-tools revision runs on the Java 11 build toolchain.
RUN curl --fail --location --retry 3 -o commandlinetools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip && \
    unzip -q commandlinetools.zip && rm commandlinetools.zip && \
    yes | /opt/cmdline-tools/bin/sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
    "build-tools;30.0.3" "platforms;android-30"

RUN curl --fail --location --retry 3 -o apktool_2.9.3.jar \
    https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar && \
    echo '7956eb04194300ce0d0a84ad18771eebc94b89fb8d1ddcce8ea4c056818646f4  apktool_2.9.3.jar' | sha256sum -c - && \
    curl --fail --location --retry 3 -o APKEditor-1.4.9.jar \
    https://github.com/REAndroid/APKEditor/releases/download/V1.4.9/APKEditor-1.4.9.jar && \
    echo 'a9cd40df818845456be6d696de6110c89edf4b0a0580cb83438ed6b25a366e67  APKEditor-1.4.9.jar' | sha256sum -c - && \
    curl --fail --location --retry 3 -o uber-apk-signer-1.2.1.jar \
    https://github.com/patrickfav/uber-apk-signer/releases/download/v1.2.1/uber-apk-signer-1.2.1.jar && \
    echo '47ae984df200afbae292d6f950e517afb445d3c902a1edc7717a82c513c5e772  uber-apk-signer-1.2.1.jar' | sha256sum -c - && \
    curl --fail --location --retry 3 -o baksmali-2.4.0.jar \
    https://bitbucket.org/JesusFreke/smali/downloads/baksmali-2.4.0.jar && \
    echo 'a558e863e81eec5a2d749d64d8f2ce4457dd735adb685982669ed6ecee90aad3  baksmali-2.4.0.jar' | sha256sum -c - && \
    curl --fail --location --retry 3 -o build-companion \
    https://github.com/bttv-android/build-companion/releases/download/v5.2.0/build-companion && \
    echo 'd899bcdf51d3a597e3bde41fff77acd54798cb5848ccffd19287a0c7d9f00e06  build-companion' | sha256sum -c - && \
    chmod +x build-companion

ENV APKTOOL_PATH=/opt/apktool_2.9.3.jar \
    APKEDITOR_PATH=/opt/APKEditor-1.4.9.jar \
    UBER_APK_SIGNER_PATH=/opt/uber-apk-signer-1.2.1.jar \
    BAKSMALI_PATH=/opt/baksmali-2.4.0.jar \
    BUILD_COMPANION=/opt/build-companion \
    GRADLE_USER_HOME=/gradle-cache

COPY tools/docker_build.sh /opt/bttv/docker_build.sh
RUN sed -i 's/\r$//' /opt/bttv/docker_build.sh
WORKDIR /source
ENTRYPOINT ["/bin/bash", "/opt/bttv/docker_build.sh"]
