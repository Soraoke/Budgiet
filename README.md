# Budgiet
A budget tracking app with the frontend written in Kotlin for Android or Swift for iOS and
the backend written in Rust.

# Features

TODO:

# App Demo

TODO:

# Developer Setup

Install [Rust](https://rust-lang.org/tools/install/) and add it to your `$PATH` (see instructions in install script).
Ensure it is installed by reopening the terminal and running `cargo --help`.

## Android

 * Install [**Android Studio**](https://developer.android.com/studio).
 * Clone the repository: `git clone git@github.com:Soraoke/Budgiet.git`
 * Set environment variables:
   * `ANDROID_HOME="$HOME/Android"`
 * Open the `Budgiet/android` directory with **Android Studio**.
  
   > After opening the project, you might not be able to build the app.
   > This is because gradle needs manual setup (not really).
   > On Android Studio, go to `Settings > Build, Execution, Deployment > Build Tools > Gradle`.
   > Set the `Gradle user home` option to `<your-user-home>/.gradle`,
   > and `Gradle JDK` to `GRADLE_LOCAL_JAVA_HOME` (or `<your-android-studio-install-dir>/jbr`).

 * Click on `Sync Project with Gradel Files`
   (the elephant icon with a down-left arrow on the right side of the top bar).

> Note: On MacOS, Android Studio must be opened through the terminal with `open -a "/Applications/Android Studio.app"` because otherwise it will not use the user's `$PATH` variable (and thus it can't run cargo).
>
> Alternatively, you can follow [this blog post](https://www.bounga.org/tips/2020/04/07/instructs-mac-os-gui-apps-about-path-environment-variable/) to open it normally through the GUI.

## IOS

TODO:

# Contributing to Budgiet

See [CONTRIBUTING.md](CONTRIBUTING.md)
