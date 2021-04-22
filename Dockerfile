FROM debian:buster

RUN apt update
RUN apt install openjdk-11-jdk-headless git wget zip --yes

# build tools

WORKDIR /opt

RUN wget https://dl.google.com/android/repository/build-tools_r30.0.1-linux.zip

RUN unzip build-tools_r30.0.1-linux.zip
RUN rm build-tools_r30.0.1-linux.zip
ENV BUILDTOOLS_PATH=/opt/android-11

# apktool


RUN wget https://github.com/iBotPeaches/Apktool/releases/download/v2.5.0/apktool_2.5.0.jar

ENV APKTOOL_PATH /opt/apktool_2.5.0.jar

# uber apk signer

RUN wget https://github.com/patrickfav/uber-apk-signer/releases/download/v1.2.1/uber-apk-signer-1.2.1.jar

ENV UBER_APK_SIGNER_PATH /opt/uber-apk-signer-1.2.1.jar

# baksmali

RUN wget https://bitbucket.org/JesusFreke/smali/downloads/baksmali-2.4.0.jar

ENV BAKSMALI_PATH /opt/baksmali-2.4.0.jar

# public-fixer

RUN wget https://github.com/bttv-android/public-fixer/releases/download/v3.0.0/public-fixer
RUN shasum public-fixer
RUN chmod +x public-fixer
ENV PUBLIC_FIXER /opt/public-fixer

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
