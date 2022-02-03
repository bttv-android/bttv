# Architecture - Overview

This file should help new contributors getting started.

> Update: This was written before the `mod` directory was an Android Studio/Gradle project
> to every time you see the directory `source/bttv` mentioned, `mod/app` is ment.

## Build process

First let's understand the build process.
In order to build a the bttv-android mod we need three things:

1. The official twitch application
> Note: Before v10.3.1 it was possible to get a single `.apk` of the Twitch app through evozi. This is not possible anymore, instead evozi will hand out a zip archive which includes the App as multiple split apks. The toolchain now includes a step that merges these together to a single apk again. Just don't get confused when this document refers to a `twitch.apk`.
2. New java classes and methods we can call
3. Additional changes within existing classes in the apk (monkey patches)

[![Build process diagram][build-img]][build-img]

First we extract the `.apk` file and convert the `.dex` files in it into human readable (and editable) `.smali` code using `apktool`. Then we compile our own java classes and copy them into the bundle (they get converted to `.smali` as well). These classes by themselves are just dead code as they are never called. So the last step is to make changes to some existing `smali` files. These changes are stored in the form of git `.patch` files in the `patches` directory. We can use apktool again to build a new apk file (which gets signed afterwards using `uber apk signer`).

## mod/ and patches

The two parts of this mod are stored in the `patches` and the `mod` directory. Let's 
touch on them for a litle bit:

[![diagram][inter-img]][inter-img]

## mod

[![diagram][source-img]][source-img]

In order to compile `javac` needs the call signature of every method we call in our code (`mod/app`). So if we want to call methods which we have not written, like `Log.d()` in `android.util` we need to mock them. As the compiled references have to be the same as in the android application the mod directory is spammed with packages that really only contain mocks.



## patches

[![diagram][monke-img]][monke-img]

As we can't publish the whole application we simply track all our changes using `git`. This is done by creating a new tag right after the dissemblance of the base apk file. Every modification can then be tracked using `git diff` the result of which is stored in the `patches` directory. `git apply` takes these files and appies the changes to a given commit. This way, given the same base code produced by apktool, the mod is reproducable.

`base + patches (+ sources) = mod`

# Architecture - mod directory

The `mod` Android Studio Project contains three modules:

 - [`app`](#app) - core logic of the mod
 - [`twitch`](#twitch) - stubs for Twitch Classes
 - [`consumer`](#consumer) - needed to get some mangled methods back

## app

The app module contains most of the logic for the mod. For it to use it depends on the `twitch` module to use classes in the stock twitch app.
All calls in `patches` should go to methods in the `bttv.api` package. The methods in this package are mostly just wrappers around classes in the root `bttv` package. They exist for the purpose of error handling (they call their underlying method in a try/catch block) and somtimes Logging (they can log if a method is called and what it has returned). For Android Studio this will look like dead code.

## twitch

This module consists of stubs for classes that exist in the twitch app. The app module depends on it's APIs. This means the app module can call methods in this module but this module itself will not be compiled. This way we do not overwrite the existing classes when merging.

## consumer

For the Webp-Glide library we need to call certain methods and use classes that do not exist in the stock app. This module is an App which simply registers the WebP library. In the buildsource step we then copy the needed files from this module into the bundle.


If anything is left unclear feel free to [create a new discussion][new-disc] here on github.

[new-disc]: https://github.com/bttv-android/bttv/discussions/new
[build-img]: ./.github/docs/build.png?raw=true
[source-img]: ./.github/docs/source.png?raw=true
[inter-img]: ./.github/docs/interaction.png?raw=true
[monke-img]: ./.github/docs/monke.png?raw=true


