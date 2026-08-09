# Meltfall Live Android

This is the primary Android app module for Meltfall Live. It is a native live
wallpaper using a custom rendering view, hardware acceleration,
`Choreographer`, and bitmap matrix transforms.

The canonical setup, build, install, troubleshooting, and release-default
instructions are in the repository root [README.md](../README.md).

Quick build from the repository root:

```powershell
.\gradlew.bat -p .\MeltingFaceRainAndroid assembleDebug
```

Generated APK:

```text
MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

Important pinned requirements:

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Android Gradle Plugin 8.7.3
- Gradle 8.10.2 through the committed root Gradle Wrapper

Release defaults are read from:

```text
config\defaults.properties
```
