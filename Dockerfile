FROM debian:buster

RUN apt update
RUN apt install openjdk-11-jdk-headless git wget zip --yes

# build tools

WORKDIR /opt

RUN wget https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip

RUN unzip commandlinetools-linux-6858069_latest.zip
RUN rm commandlinetools-linux-6858069_latest.zip

ENV ANDROID_SDK_HOME /opt/android-sdk-linux
ENV ANDROID_SDK_ROOT /opt/android-sdk-linux
ENV ANDROID_HOME /opt/android-sdk-linux
ENV ANDROID_SDK /opt/android-sdk-linux

RUN echo "y\n" | cmdline-tools/bin/sdkmanager --sdk_root=/opt/android-sdk-linux "build-tools;30.0.0"

# apktool


RUN wget https://github.com/iBotPeaches/Apktool/releases/download/v2.5.0/apktool_2.5.0.jar

ENV APKTOOL_PATH /opt/apktool_2.5.0.jar

# uber apk signer

RUN wget https://github.com/patrickfav/uber-apk-signer/releases/download/v1.2.1/uber-apk-signer-1.2.1.jar

ENV UBER_APK_SIGNER_PATH /opt/uber-apk-signer-1.2.1.jar

# baksmali

RUN wget https://bitbucket.org/JesusFreke/smali/downloads/baksmali-2.4.0.jar

ENV BAKSMALI_PATH /opt/baksmali-2.4.0.jar

# build-companion

RUN wget https://github.com/bttv-android/build-companion/releases/download/v5.0.0/build-companion
RUN shasum build-companion
RUN chmod +x build-companion
ENV BUILD_COMPANION /opt/build-companion

# Cleanup
RUN apt remove wget --yes
RUN apt autoremove --yes
RUN apt clean --yes

# Git

RUN git config --global user.email "you@example.com"
RUN git config --global user.name "Your Name"

# Repo

WORKDIR /usr/build

ENV DOCKER true
RUN mkdir dist

COPY disassemble disassemble
COPY build build
COPY buildsource buildsource
COPY patch patch
COPY initworkspace initworkspace

ENTRYPOINT [ "/bin/bash", "initworkspace" ]
