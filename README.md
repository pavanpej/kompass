<p align="center">
  <img src="docs/images/logo.svg" width="96" height="96" alt="Kompass logo">
</p>

# Kompass

A minimalist Android compass + bubble level app, plus a growing set of related sensor/location
readouts (lat/long, magnetic field strength, barometric altitude, sun/moon bearing) — see the
maintainer's own framing: it's named Kompass, but it's not just a compass anymore. Dark,
high-contrast UI centered on one circular dial that shows magnetic/true heading (with 16-point
direction label) and a flat-surface bubble level merged into the same circle.

- **Package**: `com.pavanpej.kompass`
- **Language**: Kotlin, Jetpack Compose (Material 3)
- **Min SDK**: 26 (Android 8.0) · **Target/Compile SDK**: 37
- **License**: [MIT](LICENSE)

For architecture, design history, and the reasoning behind non-obvious decisions (there are a
few sharp edges in sensor math), see **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**. Read that
before making sensor-math or UI-layout changes — it documents bugs that were already found and
fixed once, and design decisions that look like "why not X" until you read the why-not.

## Features

- Compass dial with magnetic or true north (toggle pinned to the bottom of the screen; true north
  needs location permission to resolve magnetic declination)
- Bubble level merged into the dial (flat-surface tilt only — see architecture doc for why there's
  no "on its side" / "upright" mode)
- Tap the dial to lock the current heading as a target bearing (marker appears on the ring, tap
  again to release); haptic tick when you point back at it
- Approximate sun/moon bearing markers on the ring (see architecture doc for accuracy caveats —
  not verified against a real ephemeris)
- Lat/long readout under the direction label — tap to copy to clipboard; decimal or DMS format
  (set in Settings)
- Magnetic field strength (top-left) and approximate barometric altitude (below the dial —
  tentative, may be removed later)
- Haptic tick when the phone becomes level, crosses a cardinal direction (N/E/S/W), or returns to
  a locked target bearing
- Low-accuracy banner prompting a figure-8 recalibration wave, per Android's own guidance
- 16-point direction label (N, NNE, NE, ...) next to the degree readout
- Settings screen (gear icon, top-right): haptics on/off, default north mode on launch, coordinate
  format — all persist across restarts via DataStore
- True-black (`#000000`) AMOLED background, single Princeton Orange (`#FF8600`) accent

## Requirements to build

- **Android Studio** (any recent version with AGP 9.x / Kotlin 2.2 support) — easiest path, handles
  the SDK/JDK for you.
- **JDK**: the project has no local JDK bundled. If building from the command line (not via
  Android Studio), you must point `JAVA_HOME` at a JDK 17+ install. Android Studio ships one at:
  ```
  <Android Studio install dir>/jbr
  ```
  e.g. on Windows: `C:\Program Files\Android\Android Studio\jbr`
- **Android SDK**: Platform 37, Build-Tools matching AGP 9.3.1. Installed automatically by Android
  Studio's SDK Manager on first project sync.
- A **physical device** is required to test anything meaningful — the compass/level features need
  a real magnetometer + accelerometer, which emulators don't provide. The device needs Android 8.0
  (API 26) or newer.

## Get the code

```bash
git clone https://github.com/pavanpej/kompass.git
cd kompass
```

## Set up your device

1. On the device: **Settings → About phone → tap "Build number" 7 times** to unlock Developer
   Options (exact wording varies slightly by manufacturer/Android version).
2. **Settings → Developer options → enable "USB debugging."**
3. Connect the device to your computer via USB. On first connection you'll get an "Allow USB
   debugging?" prompt on the device — accept it (optionally checking "always allow from this
   computer").

## Build & run

**Via Android Studio (recommended):**
1. Open the cloned project root in Android Studio.
2. Let Gradle sync finish (first sync downloads dependencies, can take a few minutes).
3. Your device should now appear in the device dropdown in the toolbar — select it.
4. Run (▶).

**Via command line:**
```bash
# Windows (Git Bash), pointing at Android Studio's bundled JDK:
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat assembleDebug

# macOS/Linux, if JAVA_HOME isn't already set to a JDK 17+:
JAVA_HOME="$(/usr/libexec/java_home -v17)" ./gradlew assembleDebug
```
The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install it to your connected
device with `adb` (bundled with the Android SDK, normally already on your `PATH` if you've used
Android Studio before):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
`-r` reinstalls over any existing copy. If `adb` isn't found, it's at
`<Android SDK location>/platform-tools/adb` (SDK location is whatever `sdk.dir` in your local
`local.properties` points to, generated on first Android Studio sync).

## Tests

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat testDebugUnitTest
```

Unit tests are plain JUnit4, no emulator/Robolectric required — the whole suite runs in a couple
of seconds. See **[docs/TESTING.md](docs/TESTING.md)** for the testing philosophy, the
paired-test convention for new logic, and a full human-auditable directory of every test file and
what it covers.

## Permissions

| Permission | Why |
|---|---|
| `VIBRATE` | Haptic tick on level/cardinal-direction/bearing-lock events |
| `ACCESS_COARSE_LOCATION` | Powers lat/long, true-north declination, and sun/moon bearing. Requested once, proactively, the first time the compass screen appears (not at app launch) — see docs/ARCHITECTURE.md for why this changed from the original "only when tapping TRUE" behavior. Falls back gracefully (those readouts simply stay hidden / magnetic-only) if denied. |

## CI / GitHub Actions

**Not set up yet.** No `.github/workflows` directory exists. A workflow to build a debug APK per
commit is planned but not yet implemented — do not assume CI exists or is validating anything.
