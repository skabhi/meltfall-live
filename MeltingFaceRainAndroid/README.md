# Funky Face Rain Android

Native Android version of the WPF funky melting-face rain animation.

## Build

Open this folder in Android Studio:

```text
MeltingFaceRainAndroid
```

Then let Android Studio sync Gradle and press Run.

## Controls

- Tap the screen to quit.
- The app starts fullscreen.
- A small FPS/help text appears for the first 3 seconds.

## Notes

The animation is implemented as a custom Android `View` using hardware acceleration and `Choreographer` for display-synced frames. Emoji variants use pre-tinted PNG assets for smoother rendering.
