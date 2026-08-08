# Meltfall Live Android

This is the primary app in the Meltfall Live repository. It is a native Android
live wallpaper using a custom rendering view, hardware acceleration,
`Choreographer`, and bitmap matrix transforms.

## Requirements

- JDK 17
- Android SDK with compile SDK 35
- Android Gradle Plugin 8.7.3
- Gradle 8.10.2, or Android Studio with compatible Gradle support

## Version

The Android app version is read from the repository root `VERSION` file. Update
that file before building when preparing a new app version.

## Build

From the repository root:

```powershell
$env:JAVA_HOME="C:\Path\To\jdk-17"
$env:ANDROID_HOME="C:\Path\To\Android\sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
gradle -p .\MeltingFaceRainAndroid assembleDebug
```

If the local `android-build-deps/` folder exists:

```powershell
.\android-build-deps\gradle-8.10.2\bin\gradle.bat -p .\MeltingFaceRainAndroid assembleDebug
```

The APK is generated at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install

Enable USB debugging on the phone, connect it, approve the prompt, then run:

```powershell
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

From the repository root, install the copied root APK with:

```powershell
adb install -r .\MeltfallLiveAndroid-debug.apk
```

## Use

1. Open **Meltfall Live**.
2. Tap **View wallpaper**.
3. Drag the bottom sheet up to tune wallpaper-specific settings.
4. Tap **Use this wallpaper**.
5. Use Android's wallpaper preview to apply the live wallpaper.

## Notes

- The main screen gear opens app-wide settings.
- The preview bottom sheet contains settings for this specific wallpaper.
- Debug builds can show a rendered FPS counter from app-wide settings.
- Emoji variants use pre-tinted PNG assets for smoother rendering.
