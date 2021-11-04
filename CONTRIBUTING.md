# CONTRIBUTING

# Issues

Issues are used for tracking new features, bugs or other tasks. If you have a question don't hesitate and open a [new discussion][new-discussion]!
Please make sure that your issue contains all nesessary information to avoid waiting times. All communication, including issues, should be written in understandable english.

## Scope of the Project

Before you submit a new issue please make sure it is in scope of this project:
- bugs in bttv-android
- features that exist in BTTV
- features that enhance quality of life (e.g. improving integration with existing Twitch behavior)

### Not in scope:
- bugs not caused by the mod
- features that are unrelated to BTTV (e.g. a new theme)

This way the mod does not become bloated and is thus more stable and maintainable.


# PRs

Most PRs have an associated issue, if you PR is one of those make sure to reference the issue. Your PR will be reviewed and commented please prepare yourself for that, we do not want to end up in stale PRs. It's ok (and encouraged) to open an PR even when you are not finished. Please mark those PRs as drafts. If you have any questions feel free to tag @bttv-android/developers and somebody will help you out :)

Before you undraft a PR follow this checklist:
- [ ] I tested my change
- [ ] I use the "bttv_" prefix for all resources I propose
- [ ] If my change is significant enough, I added it to the CHANGELOG.md under master


# Setup

> Read the build it yourself part (the hard way) in the README aswell!

Do the following once:

1. Initialize the workspace as described in the README
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
├─ build - re-assembles the 'disass' dir
├─ decompile - you need JADX to use this
├─ install - install the result of build on a device (using adb)
├─ patches - contains monkey patches
├─ genmonke - script that generates the patches based on disass (more below)
├─ disass/ - renamed from disass
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

During initialization the disass dir is initialized with an empty git repo.
Right after the disassemblement the first commit is made and tagged "base".
This is used to generate the patch file.
`patches` is thus the result of a diff between master and base.

#### Only source changes

You need to nothing else, you can commit your code and start a pull request!

#### Monkey patch changes

Run `./genmonke <dir>` before you make a commit.

## Best practices for contributers

> Please also read the [architecture.md](./architecture.md) file!

Everytime you get a new version of the code (e.g. using git pull or git checkout) remove `extraced` and run `./initworkspace` again.



[new-discussion]: https://github.com/bttv-android/bttv/discussions/new
