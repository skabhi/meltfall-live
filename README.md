# Meltfall Live

Meltfall Live is mainly an Android live wallpaper app. It renders colorful
melting-face emojis raining down the screen with depth, scale, speed, and
brightness variation.

The repository also keeps the earlier Windows/PowerShell versions as secondary
desktop artifacts, but current development is focused on Android.

## Author

Abhishek Kumar Singh

## Current Version

The current project and Android app version is tracked in [VERSION](VERSION).
The Android Gradle build reads this file and derives the APK `versionName` and
`versionCode` from it, so future version bumps should update `VERSION` first.

## Project Layout

- `MeltingFaceRainAndroid/` - primary Android live wallpaper project.
- `config/defaults.properties` - editable Android release defaults.
- `dist/android/MeltfallLiveAndroid-debug.apk` - latest debug APK artifact.
- `assets/` - shared source images and funky PNG variants.
- `windows/powershell/` - Windows WPF PowerShell scripts.
- `windows/exe/` - Windows executable sources, icons, and built `.exe` files.
- `archive/` and `experiments/` - older prototypes and experimental assets.

## License

This repository is public for viewing only. No permission is granted to use,
copy, modify, distribute, or create derivative works from this code or assets.
See [LICENSE](LICENSE).

## Set Up On A New Computer

1. Install Git.

2. Clone the repository:

```powershell
git clone git@github.com:skabhi/meltfall-live.git
cd meltfall-live
```

If SSH is not set up on the new computer yet, add an SSH key to GitHub first,
or clone with HTTPS:

```powershell
git clone https://github.com/skabhi/meltfall-live.git
cd meltfall-live
```

3. Install Android Studio, or install a local command-line Android toolchain:

- JDK 17
- Android SDK with platform SDK 35
- Android build tools
- Gradle 8.10.2, or use Android Studio's bundled Gradle support

4. If using command-line builds, set these environment variables for the current
PowerShell session. Adjust paths to match your computer:

```powershell
$env:JAVA_HOME="C:\Path\To\jdk-17"
$env:ANDROID_HOME="C:\Path\To\Android\sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
```

## Build Android APK

From the repository root:

```powershell
.\android-build-deps\gradle-8.10.2\bin\gradle.bat -p .\MeltingFaceRainAndroid assembleDebug
```

On a fresh computer where `android-build-deps/` is not present, use an installed
Gradle instead:

```powershell
gradle -p .\MeltingFaceRainAndroid assembleDebug
```

The generated APK is:

```text
MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

To refresh the debug APK artifact:

```powershell
Copy-Item -LiteralPath .\MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk -Destination .\dist\android\MeltfallLiveAndroid-debug.apk -Force
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

The debug app also has a developer-only helper that copies the current phone
settings in `defaults.properties` format. Paste those values into this file,
commit the change, then build the release AAB.

## Install On Android Phone

1. Enable Developer options on the phone.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Approve the USB debugging prompt on the phone.
5. Verify ADB sees the phone:

```powershell
adb devices
```

6. Install the debug APK:

```powershell
adb install -r .\dist\android\MeltfallLiveAndroid-debug.apk
```

If ADB is not on `PATH`, use the full path to `adb.exe`, for example:

```powershell
& "C:\Path\To\Android\sdk\platform-tools\adb.exe" install -r .\dist\android\MeltfallLiveAndroid-debug.apk
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

This project is configured locally on the original development computer to use
a dedicated SSH key for pushes. A new computer needs its own GitHub
authentication setup.
