FROM androidsdk/android-30:latest

# build tools

ENV BUILDTOOLS_PATH=${ANDROID_SDK}/build-tools/30.0.2

# apktool

WORKDIR /opt

RUN wget https://github.com/iBotPeaches/Apktool/releases/download/v2.5.0/apktool_2.5.0.jar

ENV APKTOOL_PATH /opt/apktool_2.5.0.jar

# uber apk signer

RUN wget https://github.com/patrickfav/uber-apk-signer/releases/download/v1.2.1/uber-apk-signer-1.2.1.jar

ENV UBER_APK_SIGNER_PATH /opt/uber-apk-signer-1.2.1.jar

# baksmali

RUN wget https://bitbucket.org/JesusFreke/smali/downloads/baksmali-2.4.0.jar

ENV BAKSMALI_PATH /opt/baksmali-2.4.0.jar

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

COPY source/ source/
COPY monke.patch monke.patch

ENTRYPOINT [ "/bin/bash", "initworkspace" ]
