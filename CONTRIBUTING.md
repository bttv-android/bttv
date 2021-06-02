# CONTRIBUTING

> Read the build it yourself part (the hard way) in the README aswell!

Do the following once:

1. Intialize the workspace as described in the README
2. Rename `disass` to `extracted`
3. Now you can build the java source code in (`mod`) using `./buildsource extracted`.
   It will automaticly dex the class files and baksmali them.
   It is recommended to scim through the script so you can set your environment variables.
4. Build your new changes using the `./build extracted` script.
5. Open an emulator and run `./install`

> Please only modify or add java files in the bttv package! Others won't be compiled anyway.
> In case you need to apply a monkey-patch (i.e. edit smali files (not in the bttv package) in `extracted` directly) read the [instructions below](#genmonke).
> **Never check in the `extracted` or `disass` directories for legal reasons**

## Overview

```
bttv/
├─ initworkspace - used to set up everything
├─ buildsource - used to build the java sources
├─ build - re-assembles the 'extracted' dir
├─ decompile - you need JADX to use this
├─ install - install the result of build on a device (using adb)
├─ monke.patch - contains monkey patches
├─ genmonke - script that generates the monke.patch based on extracted (more below)
├─ extracted/ - renamed from disass
│  ├─ AndroidManifest.xml
│  ├─ res/
│  ├─ smali_classes0-10/ - contains disassembled app + bttv code
│  ├─ dist/
│  │  ├─ twitch.apk - the final app
├─ mod/        - java sources for patches
│  ├─ app/     - sources for the mod
|  ├─ twitch/  - stubs so we can call twitch's classes

```

### genmonke

During initialization the disass / extraced dir is initialized with an empty git repo.
Right after the disassemblement the first commit is made and tagged "base".
This is used to generate the patch file.
`monke.patch` is thus the result of a diff between master and base.

#### Only source changes

You need to nothing else, you can commit your code and start a pull request!

#### Monkey patch changes

Run `./genmonke <dir>` before you make a commit.

## Best practices for contributers

> Please also read the [architecture.md](./architecture.md) file!

Everytime you get a new version of the code (e.g. using git pull or git checkout) remove `extraced` and run `./initworkspace` again.
