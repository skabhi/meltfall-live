# Meltfall Live

Meltfall Live is mainly an Android live wallpaper app. It renders colorful
melting-face emojis raining down the screen with depth, scale, speed, and
brightness variation.

The repository also keeps earlier Windows/PowerShell versions as secondary
desktop artifacts, but current development is focused on Android.

## Contents

- [License Notice](#license-notice)
- [Current Android Build](#current-android-build)
- [Project Layout](#project-layout)
- [Set Up On A New Windows Computer](#set-up-on-a-new-windows-computer)
- [Build Android APK](#build-android-apk)
- [Install On Android Phone](#install-on-android-phone)
- [Tune Release Defaults](#tune-release-defaults)
- [Troubleshooting](#troubleshooting)
- [Reproducibility](#reproducibility)
- [Development Workflow](#development-workflow)

## Author

Abhishek Kumar Singh

## License Notice

This repository is public for viewing only. No permission is granted to use,
copy, modify, distribute, or create derivative works from this code or assets.
See [LICENSE](LICENSE).

Important owner decision: these setup instructions explain how to clone and
build the project, but the current license does not grant public permission to
copy or create derivative works. Keep the license as-is unless the repository
owner deliberately chooses a different legal model.

## Current Android Build

- Android app path: `MeltingFaceRainAndroid`
- Package: `com.anderson.singh.play.meltfalllive`
- Version: read from [VERSION](VERSION)
- Current version: `1.1.0`
- Current versionCode: `10100`
- JDK: `17`
- Android Gradle Plugin: `8.7.3`
- Gradle Wrapper: `8.10.2`
- compile SDK: `35`
- target SDK: `35`
- min SDK: `23`
- Build Tools: `35.0.0`
- Debug APK path from repository root:
  `MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk`

## Project Layout

- `MeltingFaceRainAndroid/` - primary Android live wallpaper project.
- `MeltingFaceRainAndroid/gradlew.bat` - pinned Gradle Wrapper for Windows.
- `MeltingFaceRainAndroid/gradlew` - pinned Gradle Wrapper for macOS/Linux/CI.
- `config/defaults.properties` - editable Android release defaults.
- `dist/android/MeltfallLiveAndroid-debug.apk` - optional copied debug APK artifact.
- `assets/` - shared source images and funky PNG variants.
- `windows/powershell/` - Windows WPF PowerShell scripts.
- `windows/exe/` - Windows executable sources, icons, and built `.exe` files.
- `archive/` and `experiments/` - older prototypes and experimental assets.
- `android-build-deps/` - optional private local tooling folder on the original
  development computer. It is ignored and is not part of a fresh clone.

## Set Up On A New Windows Computer

Clone the repository first. Run these commands from any PowerShell directory
where you keep projects:

```powershell
git clone https://github.com/skabhi/meltfall-live.git
cd meltfall-live
```

If SSH is already configured for GitHub, this also works:

```powershell
git clone git@github.com:skabhi/meltfall-live.git
cd meltfall-live
```

Use one of the two setup paths below.

### Path A: Android Studio Setup

1. Install Android Studio from the official Android Developers page:
   https://developer.android.com/studio
2. During setup, install or select JDK 17. Android Studio may provide a bundled
   JDK; verify it is JDK 17 for this project.
3. Open Android Studio, then open this project directory:
   `meltfall-live\MeltingFaceRainAndroid`
4. In Android Studio SDK Manager, install:
   - Android SDK Platform 35
   - Android SDK Build-Tools 35.0.0
   - Android SDK Platform-Tools
5. Let Android Studio sync the Gradle project. It should use the committed
   wrapper in `MeltingFaceRainAndroid`.
6. Build the debug APK from Android Studio, or use the PowerShell build command
   in [Build Android APK](#build-android-apk).

### Path B: Command-Line-Only Setup

Official downloads:

- JDK 17: https://learn.microsoft.com/en-us/java/openjdk/download
- Android SDK command-line tools: https://developer.android.com/studio#command-tools

Suggested folders:

```text
C:\Android\jdk-17.0.x
C:\Android\sdk
```

JDK archives commonly extract to a versioned folder. `JAVA_HOME` must point to
the folder that directly contains `bin\java.exe`, for example:

```text
C:\Android\jdk-17.0.19
```

For Android command-line tools, the final layout must be:

```text
C:\Android\sdk\cmdline-tools\latest\bin\sdkmanager.bat
```

If the zip extracts to a folder named `cmdline-tools`, create
`C:\Android\sdk\cmdline-tools\latest` and move the extracted contents into
`latest`.

Set environment variables. These commands can run from any PowerShell
directory:

```powershell
$env:JAVA_HOME="C:\Android\jdk-17.0.x"
$env:ANDROID_HOME="C:\Android\sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

Review and accept Android SDK licenses yourself:

```powershell
sdkmanager --licenses
```

Install the exact SDK packages used by this project:

```powershell
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Verify the command-line tools. These commands can run from any PowerShell
directory once `PATH` is configured:

```powershell
java -version
sdkmanager --list_installed
adb version
```

Verify the Gradle Wrapper from the Android project directory:

```powershell
cd C:\Path\To\meltfall-live\MeltingFaceRainAndroid
.\gradlew.bat --version
```

The wrapper downloads Gradle 8.10.2 automatically on first use and verifies the
distribution checksum recorded in `gradle\wrapper\gradle-wrapper.properties`.

## Build Android APK

From the Android project directory:

```powershell
cd C:\Path\To\meltfall-live\MeltingFaceRainAndroid
.\gradlew.bat assembleDebug
```

From the repository root:

```powershell
.\MeltingFaceRainAndroid\gradlew.bat -p .\MeltingFaceRainAndroid assembleDebug
```

The generated APK is:

```text
MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

Clean rebuild from the repository root:

```powershell
.\MeltingFaceRainAndroid\gradlew.bat -p .\MeltingFaceRainAndroid clean assembleDebug
```

To refresh the optional debug APK artifact:

```powershell
Copy-Item -LiteralPath .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk -Destination .\dist\android\MeltfallLiveAndroid-debug.apk -Force
```

## Install On Android Phone

1. Enable Developer options on the phone.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Approve the USB debugging prompt on the phone.
5. Verify ADB sees the phone. This can run from any PowerShell directory:

```powershell
adb devices
```

Install the freshly built APK from the repository root:

```powershell
adb install -r .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

If ADB is not on `PATH`, use the full path to `adb.exe`:

```powershell
& "C:\Android\sdk\platform-tools\adb.exe" install -r .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

Install on a specific connected device:

```powershell
adb devices
adb -s DEVICE_ID install -r .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

## Tune Release Defaults

Release defaults are stored in [config/defaults.properties](config/defaults.properties).
Open it in Notepad before building a release:

```powershell
notepad .\config\defaults.properties
```

Valid values are documented inside the file. In short:

- `drop_speed`: `0..200`, where `100` is normal speed and `0` stops falling.
- `emoji_count`: `0` or greater. There is no app-enforced maximum.
- `emoji_size`: `60..170`, where `100` is normal size.
- `fps_limit`: `0`, `30`, `45`, or `60`, where `0` means unlimited.
- `show_fps`: `true` or `false`; release builds hide the FPS counter anyway.

## Troubleshooting

- `java` not found: install JDK 17 and put `%JAVA_HOME%\bin` on `PATH`.
- `JAVA_HOME` incorrect: it must point to the folder that directly contains
  `bin\java.exe`, not the parent folder.
- `gradle` not found: use the wrapper command, `.\gradlew.bat`; no global
  Gradle install is required.
- SDK location not found: set `ANDROID_HOME` and `ANDROID_SDK_ROOT` to the SDK
  folder, for example `C:\Android\sdk`.
- SDK licenses not accepted: run `sdkmanager --licenses` and personally review
  and accept the prompts.
- compile SDK 35 missing: run
  `sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"`.
- `adb` not found: install Platform Tools and add
  `%ANDROID_HOME%\platform-tools` to `PATH`.
- Device unauthorized: unlock the phone, approve the USB debugging prompt, then
  rerun `adb devices`.
- Gradle/AGP incompatibility: keep JDK 17, AGP 8.7.3, Gradle Wrapper 8.10.2,
  SDK 35, and Build Tools 35.0.0 together until deliberately upgraded.
- Paths containing spaces: wrap paths in quotes, or use PowerShell variables.
- Clean rebuild from the repository root:
  `.\MeltingFaceRainAndroid\gradlew.bat -p .\MeltingFaceRainAndroid clean assembleDebug`

## Reproducibility

The committed Gradle Wrapper pins Gradle 8.10.2 even if newer Gradle versions
exist. Do not replace it with the latest system Gradle unless the project is
deliberately upgraded.

Pinned requirements:

- JDK 17
- Android Gradle Plugin 8.7.3
- Gradle Wrapper 8.10.2
- Android SDK Platform 35
- Android Build Tools 35.0.0

The Android project directory is `MeltingFaceRainAndroid`; Android Studio,
PowerShell builds, and CI should all use that same project directory.

## Use The Android App

1. Open **Meltfall Live** on the phone.
2. Tap **View wallpaper**.
3. Use the bottom sheet in the preview screen to tune wallpaper-specific
settings such as speed, emoji count, and size.
4. Tap **Use this wallpaper** to open Android's own live wallpaper preview.
5. From Android's preview screen, choose whether to apply it to the home screen,
lock screen, or both.

The gear icon on the main screen opens app-wide settings. Debug builds include
developer-only options such as the rendered FPS counter and FPS limit.

## Windows Desktop Artifacts

The Windows scripts remain usable directly:

```powershell
powershell.exe -STA -ExecutionPolicy Bypass -File .\windows\powershell\melting_face_wpf.ps1 -ImagePath .\assets\melting_face_transparent.png
powershell.exe -STA -ExecutionPolicy Bypass -File .\windows\powershell\melting_face_wpf_funky.ps1 -ImagePath .\assets\melting_face_transparent.png
```

The Windows executables were built from the C# sources using the .NET Framework
C# compiler and WPF assemblies available on Windows.

## Development Workflow

After each completed change:

```powershell
git status
git add .
git commit -m "Describe the change"
git push
```

For a version bump, update [VERSION](VERSION), rebuild the Android APK, refresh
`dist/android/MeltfallLiveAndroid-debug.apk`, then commit and push the version
change. Also tag the matching version:

```powershell
git tag v1.2.0
git push origin v1.2.0
```
