# loopr-player-android

Fire TV / Android TV / Android tablet player for [Loopr](https://loopr.studio). Kotlin + Jetpack Compose. Talks to `api.loopr.studio`.

**Stack:** Kotlin 2.1 · Jetpack Compose · OkHttp · kotlinx.serialization · DataStore · Media3 ExoPlayer (queued for v0.2 playback). Targets Android 22+ (Fire TV Stick 2nd gen and newer).

## What v0.1 does

The whole app is one Activity that flips between two screens based on whether a device token has been persisted:

1. **Pairing screen** — on first boot, the app calls `POST /api/v1/devices/claim-code` to get a 6-character pairing code, displays it full-screen with the URL `loopr.studio`, and polls `GET /api/v1/devices/poll/{code}` every few seconds.
2. **Player screen (placeholder)** — once the dashboard claims the code and the poll returns the device token, the app persists the token to `DataStore` and switches to the player screen, which currently shows a "Connected — Waiting for content" placeholder.

ExoPlayer + WebView playback ships in v0.2 once the backend has playlist/media endpoints.

## Project layout

```
app/src/main/
├── AndroidManifest.xml          # LEANBACK_LAUNCHER for Fire TV; BootReceiver for auto-start
├── kotlin/co/loopr/player/
│   ├── LooprApp.kt              # Application — wires LooprApi + DeviceStore singletons
│   ├── MainActivity.kt          # Single Activity — immersive fullscreen, picks pairing/player by identity flow
│   ├── api/
│   │   ├── LooprApi.kt          # OkHttp + kotlinx.serialization client
│   │   └── Models.kt            # ClaimCodeRequest/Response, PollResponse
│   ├── data/
│   │   ├── DeviceStore.kt       # DataStore-backed { deviceToken, screenId, workspaceId, screenName }
│   │   └── DeviceFingerprint.kt # Stable, anonymous SHA-256(device id)
│   ├── boot/BootReceiver.kt     # ACTION_BOOT_COMPLETED → relaunch Activity
│   └── ui/
│       ├── pairing/{PairingScreen, PairingViewModel}.kt
│       ├── player/{PlayerScreen,  PlayerViewModel}.kt
│       └── theme/{Color, Theme}.kt
└── res/
    ├── drawable/banner.xml      # Fire TV banner (320x180)
    ├── values/{strings, themes, colors}.xml
    ├── xml/data_extraction_rules.xml
    └── mipmap-anydpi-v26/ic_launcher.xml
```

## How to build (one-time setup)

This repo doesn't ship the gradle-wrapper.jar binary. Two ways to populate it:

**Option A — Open in Android Studio** (recommended)
Just open the project root. Android Studio detects the `gradle/wrapper/gradle-wrapper.properties` and downloads the wrapper on first sync.

**Option B — From CLI** (if you have Gradle installed)
```bash
gradle wrapper --gradle-version 8.11.1
```

After that:

```bash
./gradlew :app:assembleAmazonDebug   # builds the APK for Amazon Appstore distribution (Fire TV)
./gradlew :app:assemblePlayDebug     # builds the APK for Google Play distribution
./gradlew :app:assembleAmazonRelease # signed-release build (needs keystore.properties)
```

Output APKs live in `app/build/outputs/apk/`.

## Sideloading to a Fire TV during development

1. On the Fire TV: Settings → My Fire TV → Developer options → enable **ADB debugging** and **Apps from Unknown Sources**.
2. From your Mac:
   ```bash
   adb connect <fire-tv-ip>:5555
   adb install -r app/build/outputs/apk/amazon/debug/app-amazon-debug.apk
   adb shell monkey -p co.loopr.player -c android.intent.category.LEANBACK_LAUNCHER 1
   ```
   Replace `<fire-tv-ip>` with the IP shown under Settings → My Fire TV → About → Network.

The TV will display a 6-character code. On a phone or laptop, sign in to `app.loopr.studio`, click **Pair a Fire TV**, paste the code, click claim. Within a few seconds the TV flips to "Connected".

## Configuration

`API_BASE_URL` is baked into `BuildConfig` from `app/build.gradle.kts`. To point at staging, change to `https://staging.api.loopr.studio` and rebuild.

## Roadmap

- **v0.2** — ExoPlayer playback (Media3 with `SurfaceView`), playlist polling endpoint, image + video + URL slide types.
- **v0.3** — WebSocket push for instant playlist updates, decoder telemetry (`device_event` rows), local cache via Room + filesystem.
- **v1.0** — Soak test pass at 4K60 HEVC, Amazon Appstore submission, Google Play submission.

## License

Proprietary — © 2026 Chaosology. All rights reserved.
