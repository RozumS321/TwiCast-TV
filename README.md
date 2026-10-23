# TwiCast TV

A light, fast Twitch client for Android TV, built for the remote and for low-end TV boxes.

[Русская версия](README.ru.md)

> TwiCast TV is an independent project. It is not affiliated with, endorsed by or sponsored by Twitch Interactive, Inc.
> Twitch is a trademark of Twitch Interactive, Inc.

## Features

- **Home**: live channels you follow, recommendations from your categories, popular streams in your language.
- **Categories** with filters, and the live streams of each category by language.
- **Search** for channels and categories with an on-screen keyboard (LAT / РУС / УКР layouts) and voice input.
- **Following**: live and offline channels on one screen.
- **Channel page**: the live stream, past broadcasts and popular clips.
- **Player**: the TwiCast player (ExoPlayer, starts fast and stays light on weak boxes) or the official Twitch embed
  as a fallback. Quality menu, read-only chat next to the video, a plate during Twitch ad breaks. Playback stops when
  the app goes to the background and resumes when you come back.
- **Sign in from your phone** with the Twitch device code flow: a QR code and a short code, no typing passwords with
  the remote.
- **Settings**: player, default quality, chat with the stream, light / dark / system theme, interface language
  (English, Українська, Русский).
- **Self-update** from GitHub Releases, with a "What's new" list covering every skipped version.

## Install

Download the latest APK from the [releases](https://github.com/RozumS321/TwiCast-TV-releases/releases) and install it on
the TV, for example:

```bash
adb install twicast-tv-<version>.apk
```

Android TV 8.0 (API 26) or newer. Installed this way, the app then updates itself.

## Build

You need JDK 21 and the Android SDK (compile SDK 37).

1. Register an application in the [Twitch developer console](https://dev.twitch.tv/console/apps): client type
   **Public** (the app keeps no secret). The OAuth redirect URL is not used by the device code flow; any valid URL works.
2. Copy `local.properties.example` to `local.properties` and fill in `sdk.dir` and `twitch.clientId`.
3. Build and install:

```bash
./gradlew assembleDebug
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Only `user:read:follows` is requested at sign-in.

## Tech

Kotlin, Compose for TV, MVVM with a single `StateFlow` UI state per screen, Hilt, Retrofit with kotlinx.serialization,
Coil 3, Media3 ExoPlayer, DataStore, Navigation Compose with type-safe routes.

Performance rules (focus and sidebar animations only in the draw phase, keys in every lazy list, image requests sized
to the card) and the code conventions are in [docs/twicast_tv.md](docs/twicast_tv.md); colors, type and sizes in
[docs/DESIGN.md](docs/DESIGN.md).

```
app/src/main/java/app/twicast/tv/
├─ core/        design system, DI, network, device profile, locale
├─ data/        models, Twitch API, repositories, updates
├─ feature/     home, categories, search, following, profile, channel, player, auth, settings, update
└─ navigation/  app shell with the sidebar
```

## License

[Apache License 2.0](LICENSE).
