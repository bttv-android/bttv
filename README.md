# BTTV-android

## Patch it yourself

Prerequisites: Linux, Git, Apktool and Uber APK Signer.

> If you get stuck at any point, just remove the `disass` dir and try again

0. Download [Apktool](https://ibotpeaches.github.io/Apktool/) and [Uber APK Signer](https://github.com/patrickfav/uber-apk-signer/releases/latest)
1. Download the Twitch App's apk file, drop it in this directory and call it "twitch.apk"
2. Download the [correct BTTV .patch file](https://github.com/bttv-android/patchfiles)
3. Disassemble it by running `APKTOOL_PATH=/path/to/apktool.jar ./disassemble`
4. Apply patch by running `./patch <patchfile>.patch`
5. Re-assemble patch `APKTOOL_PATH=/path/to/apktool.jar UBER_APK_SIGNER_PATH=/path/to/uber-apk-signer-x.y.z.jar ./build disass`
6. Transfer to device and [install apk](https://www.wikihow.com/Install-APK-Files-from-a-PC-on-Android)

> (Adb installed and device connected? Try the : `./install` script)

## For contributers

> Read the patch it yourself part above aswell!

You will also need the android sdk and [baksmali](https://github.com/JesusFreke/smali)

Do the following once:

1. Download and install everything like stated above
2. Do the apply patch (assemble not needed yet)
3. Rename `disass` to `extracted`

4. Now you can build the java source code in (`source`) using `./buildsource`.
   This will result in the creation of a directory called `sourceout`.
   It will also automaticly dex the class files and baksmali them.
   I recommend to scim through the script so you can set your environment variables.
5. Build your new changes using the `./build extracted` script.
6. Open an emulator and run `./install`

> Please only modify or add java files in the bttv package! Others won't be compiled anyway.
> Also keep in mind, that the patch includes some monkey patching.
> I think I cant publicly share the smali files for legal reasons.
> Give me some time to come up with a solution.
