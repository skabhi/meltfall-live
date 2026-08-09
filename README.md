# Meltfall Live

Meltfall Live is mainly an Android live wallpaper app. It renders colorful
melting-face emojis raining down the screen with depth, scale, speed, and
brightness variation.

The repository also keeps earlier Windows/PowerShell versions as secondary
desktop artifacts, but current development is focused on Android.

## Author

Abhishek Kumar Singh

## Table Of Contents

- [License Notice](#license-notice)
- [Current Android Build](#current-android-build)
- [Project Layout](#project-layout)
- [Set Up On A New Windows Computer](#set-up-on-a-new-windows-computer)
- [Build Android APK](#build-android-apk)
- [Install On Android Phone](#install-on-android-phone)
- [Tune Release Defaults](#tune-release-defaults)
- [Reproducibility Rules](#reproducibility-rules)
- [Troubleshooting](#troubleshooting)
- [Development Workflow](#development-workflow)

## License Notice

This repository is public for viewing only. No permission is granted to use,
copy, modify, distribute, or create derivative works from this code or assets.
See [LICENSE](LICENSE).

That creates an intentional tension with the build instructions below: the
instructions document how the owner can reproduce the build and how reviewers
can inspect it, but the license does not currently grant third parties
permission to clone, copy, build, redistribute, or modify the app. The owner
should decide later whether to keep this view-only license or switch to a
license that permits outside use/contribution.

## Current Android Build

- Package: `com.anderson.singh.play.meltfalllive`
- Version source: [VERSION](VERSION)
- Current version: `1.1.0`
- Current `versionCode`: `10100`
- Android Gradle Plugin: `8.7.3`
- Gradle: `8.10.2`, pinned by the committed Gradle Wrapper
- JDK: `17`
- Compile SDK: `35`
- Android Build Tools: `35.0.0`

The Android Gradle build reads [VERSION](VERSION) and derives `versionName` and
`versionCode` from it.

## Project Layout

- `settings.gradle` and `build.gradle` - root Gradle project opened by Android Studio.
- `MeltingFaceRainAndroid/` - primary Android app module folder.
- `gradlew` and `gradlew.bat` - committed Gradle Wrapper entry points.
- `gradle/wrapper/` - Gradle Wrapper JAR and pinned distribution properties.
- `config/defaults.properties` - editable Android release defaults.
- `dist/android/MeltfallLiveAndroid-debug.apk` - optional checked-in debug APK artifact.
- `assets/` - shared source images and funky PNG variants.
- `windows/powershell/` - Windows WPF PowerShell scripts.
- `windows/exe/` - Windows executable sources, icons, and built `.exe` files.
- `archive/` and `experiments/` - older prototypes and experimental assets.

## Set Up On A New Windows Computer

Use **one** of these setup paths. Android Studio is easier. Command-line-only is
more explicit and closer to CI.

### Clone The Repository

Working directory for these commands: any folder where you keep projects, for
example `C:\Users\You\Documents`.

```powershell
git clone https://github.com/skabhi/meltfall-live.git
cd meltfall-live
```

After this, the repository root is the folder that contains `README.md`,
`settings.gradle`, `gradlew.bat`, and `MeltingFaceRainAndroid`.

### Path A: Android Studio Setup

1. Install Android Studio from the official Android Developers page:
   <https://developer.android.com/studio>

2. Open Android Studio.

3. Install or select **JDK 17**:
   - In recent Android Studio versions, the bundled JDK may be usable.
   - If selecting a JDK manually, choose a JDK 17 install.
   - Downloaded JDK archives commonly extract to a versioned folder such as
     `C:\Android\jdk-17.0.x`; use the folder that directly contains
     `bin\java.exe`.
   - Keep the project on JDK 17 until the project deliberately upgrades.

4. Install Android SDK components:
   - Open **Tools > SDK Manager**.
   - In **SDK Platforms**, install **Android 15 / API 35**.
   - In **SDK Tools**, enable **Show Package Details** and install:
     - **Android SDK Build-Tools 35.0.0**
     - **Android SDK Platform-Tools**
     - **Android SDK Command-line Tools latest**

5. Open the repository root in Android Studio:

```text
meltfall-live
```

6. Let Android Studio sync Gradle. Android Studio should use the committed
   wrapper at `gradlew` / `gradlew.bat`.

7. Build the debug APK from Android Studio:
   - Select **Build > Make Project**, or
   - Open the Gradle tool window and run `:app:assembleDebug`.

8. The generated APK is:

```text
MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

### Path B: Command-Line-Only Setup

Use this path if you do not want Android Studio.

Official downloads:

- JDK 17: <https://learn.microsoft.com/en-us/java/openjdk/download>
- Android SDK Command-line Tools: <https://developer.android.com/studio#command-tools>

Example install locations used below:

```text
C:\Android\jdk-17.0.x
C:\Android\sdk
```

Your `JAVA_HOME` folder must be the extracted JDK directory that directly
contains `bin\java.exe`. It does not have to be named exactly `jdk-17`.

The Android command-line tools ZIP must be extracted into this exact layout:

```text
C:\Android\sdk\cmdline-tools\latest\bin\sdkmanager.bat
```

If the ZIP extracts to a folder named `cmdline-tools`, create `latest` yourself
and move the extracted contents under it.

Environment variable assignments can run from any PowerShell directory.

```powershell
$env:JAVA_HOME="C:\Android\jdk-17.0.x"
$env:ANDROID_HOME="C:\Android\sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

After `PATH` is configured, `java`, `sdkmanager`, and `adb` verification can
run from any PowerShell directory. The Gradle Wrapper command must run from the
repository root.

```powershell
java -version
sdkmanager --version
.\gradlew.bat --version
```

Install SDK packages:

```powershell
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

You must personally review and accept the Android SDK license prompts shown by
`sdkmanager --licenses`. Do not automate acceptance unless you understand and
accept those license terms.

Verify ADB after installing Platform Tools:

```powershell
adb version
```

## Build Android APK

Primary command. Working directory: repository root.

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is:

```text
MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

Optional: refresh the checked-in debug artifact after a successful build.
Working directory: repository root.

```powershell
Copy-Item -LiteralPath .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk -Destination .\dist\android\MeltfallLiveAndroid-debug.apk -Force
```

## Install On Android Phone

1. Enable Developer options on the phone.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Approve the USB debugging prompt on the phone.

Working directory: repository root.

```powershell
adb devices
adb install -r .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

If `adb` is not on `PATH`, use the full path:

```powershell
& "C:\Android\sdk\platform-tools\adb.exe" devices
& "C:\Android\sdk\platform-tools\adb.exe" install -r .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

## Tune Release Defaults

Release defaults are stored in [config/defaults.properties](config/defaults.properties).
Open it in Notepad before building a release.

Working directory: repository root.

```powershell
notepad .\config\defaults.properties
```

Valid values are documented inside the file. In short:

- `drop_speed`: `0..200`, where `100` is normal speed and `0` stops falling.
- `emoji_count`: `0` or greater. There is no app-enforced maximum.
- `emoji_size`: `60..170`, where `100` is normal size.
- `fps_limit`: `0`, `30`, `45`, or `60`, where `0` means unlimited.
- `show_fps`: `true` or `false`; release builds hide the FPS counter anyway.

The debug app also has a developer-only helper that copies the current phone
settings in `defaults.properties` format. Paste those values into this file,
commit the change, then build the release AAB.

## Reproducibility Rules

- Use the committed Gradle Wrapper: `.\gradlew.bat`.
- Do not replace it with the latest globally installed Gradle.
- The wrapper pins Gradle `8.10.2` even if newer Gradle versions exist.
- JDK 17 and SDK 35 are pinned requirements until deliberately upgraded.
- Android Gradle Plugin `8.7.3` is declared in the root `build.gradle`.
- Android Build Tools `35.0.0` is explicitly selected in
  `MeltingFaceRainAndroid\app\build.gradle`.
- The wrapper distribution checksum is stored in
  `gradle\wrapper\gradle-wrapper.properties`.

The private/local `android-build-deps/` folder is ignored by Git and is not part
of a fresh clone. It may exist on the original development machine, but normal
users should follow the Android Studio or command-line setup above.

## Troubleshooting

### `java` command not found

Install JDK 17 and set `JAVA_HOME`. Working directory does not matter.

```powershell
$env:JAVA_HOME="C:\Android\jdk-17.0.x"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

### `gradle` command not found

Do not use a global `gradle` command. From the repository root, use:

```powershell
.\gradlew.bat --version
```

### `JAVA_HOME` is incorrect

`JAVA_HOME` must point to the JDK folder, not the `bin` folder.

Correct:

```text
C:\Android\jdk-17.0.x
```

Incorrect:

```text
C:\Android\jdk-17.0.x\bin
```

### SDK location not found

Set `ANDROID_HOME` and `ANDROID_SDK_ROOT` for the current PowerShell session.
Working directory: repository root.

```powershell
$env:ANDROID_HOME="C:\Android\sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat assembleDebug
```

### SDK licenses not accepted

Run:

```powershell
sdkmanager --licenses
```

Review and accept the licenses you agree to, then build again.

### Compile SDK 35 missing

Install the required SDK packages:

```powershell
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### `adb` not found

Add Platform Tools to `PATH` or call `adb.exe` by full path:

```powershell
$env:Path="$env:ANDROID_HOME\platform-tools;$env:Path"
adb version
```

### Device shows as unauthorized

Unlock the phone, approve the USB debugging prompt, then run:

```powershell
adb kill-server
adb start-server
adb devices
```

### Gradle or Android Gradle Plugin incompatibility

Use the wrapper and JDK 17:

```powershell
.\gradlew.bat --version
java -version
```

Do not upgrade Gradle, AGP, JDK, or SDK levels unless doing a deliberate project
upgrade.

### Paths Containing Spaces

PowerShell handles this repository path if commands are run from the repository
root. For explicit paths with spaces, quote them:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
& ".\gradlew.bat" assembleDebug
```

### Clean Rebuild

Working directory: repository root.

```powershell
.\gradlew.bat clean assembleDebug
```

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

The Windows scripts remain usable directly. Working directory: repository root.

```powershell
powershell.exe -STA -ExecutionPolicy Bypass -File .\windows\powershell\melting_face_wpf.ps1 -ImagePath .\assets\melting_face_transparent.png
powershell.exe -STA -ExecutionPolicy Bypass -File .\windows\powershell\melting_face_wpf_funky.ps1 -ImagePath .\assets\melting_face_transparent.png
```

The Windows executables were built from the C# sources using the .NET Framework
C# compiler and WPF assemblies available on Windows.

## Development Workflow

Working directory: repository root.

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

This project is configured locally on the original development computer to use
a dedicated SSH key for pushes. A new computer needs its own GitHub
authentication setup.
