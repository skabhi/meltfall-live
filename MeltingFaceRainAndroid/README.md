# Meltfall Live Android

This is the primary Android live wallpaper project for Meltfall Live.

The canonical setup, build, install, troubleshooting, and reproducibility guide
is the repository root [README](../README.md). Keep this file short so it does
not contradict the root instructions.

## Quick Build

From this directory:

```powershell
.\gradlew.bat assembleDebug
```

From the repository root:

```powershell
.\MeltingFaceRainAndroid\gradlew.bat -p .\MeltingFaceRainAndroid assembleDebug
```

The APK is generated at:

```text
MeltingFaceRainAndroid\app\build\outputs\apk\debug\app-debug.apk
```

## Notes

- Android Studio should open this directory: `MeltingFaceRainAndroid`.
- The app version is read from the repository root `VERSION` file.
- New-install defaults are read at build time from
  `config/defaults.properties`.
