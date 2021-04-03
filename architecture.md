# Architecture

This file should help new contributors getting started.

## Build process

First let's understand the build process.
In order to build a the bttv-android mod we need three things:

1. The official twitch application
> Note: Before v10.3.1 it was possible to get a single `.apk` of the Twitch app through evozi. This is not possible anymore, instead evozi will hand out a zip archive which includes the App as multiple split apks. The toolchain now includes a step that merges these together to a single apk again. Just don't get confused when this document refers to a `twitch.apk`.
2. New java classes and methods we can call
3. Additional changes within existing classes in the apk (monkey patches)

[![Build process diagram][build-img]][build-img]

First we extract the `.apk` file and convert the `.dex` files in it into human readable (and editable) `.smali` code using `apktool`. Then we compile our own java classes and copy them into the bundle (they get converted to `.smali` as well). These classes by themselves are just dead code as they are never called. So the last step is to make changes to some existing `smali` files. These changes are stored in the form of a git patch file called `monke.patch`. We can use apktool again to build a new apk file (which gets signed afterwards using `uber apk signer`).

## source/ and monke.patch

The two parts of this mod are stored in the `monke.patch` file and the sources directory. Let's 
touch on them for a litle bit:

[![diagram][inter-img]][inter-img]

## source

[![diagram][source-img]][source-img]

In order to compile `javac` needs the call signature of every method we call in our code (`source/bttv`). So if we want to call methods which we have not written, like `Log.d()` in `android.util` we need to mock them. As the compiled references have to be the same as in the android application the source directory is spammed with packages that really only contain mocks.



## monke.patch

[![diagram][monke-img]][monke-img]

As we can't publish the whole application we simply track all our changes using `git`. This is done by creating a new tag right after the dissemblance of the base apk file. Every modification can then be tracked using `git diff` the result of which is stored in the `monke.patch` file. `git apply` takes this file and appies the changes to a given commit. This way, given the same base code produced by apktool, the mod is reproducable.

`base + monke.patch (+ sources) = mod`

If anything is left unclear feel free to [create a new discussion][new-disc] here on github.

[new-disc]: https://github.com/bttv-android/bttv/discussions/new
[build-img]: ./.github/docs/build.png?raw=true
[source-img]: ./.github/docs/source.png?raw=true
[inter-img]: ./.github/docs/interaction.png?raw=true
[monke-img]: ./.github/docs/monke.png?raw=true


