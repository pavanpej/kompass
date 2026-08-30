# Kompass — Architecture & Design History

This doc exists so an AI agent (or a human) picking up this project cold doesn't have to
rediscover things that were already found, debugged, and decided in earlier sessions. Read this
before touching sensor math, the compass dial layout, or the level feature.

## What this app is

A single-screen Android compass + bubble level (plus a growing set of other sensor/location
readouts — see the maintainer's own framing: "it's named Kompass, but it's not just a compass
anymore"), with a second (settings) screen reached via a gear icon. The main screen: one circular
dial (rotating tick marks + N/E/S/W labels show heading, a fixed arrow above the circle marks
"where the phone points", a crosshair + bubble merged into the center shows flat-surface tilt,
small spoke markers show the locked bearing and approximate sun/moon bearings), degree readout +
16-point direction label + lat/long (tap to copy) below it, magnetic field strength top-left,
approximate barometric altitude below the dial, a magnetic/true-north toggle pinned to the bottom
of the screen, and (conditionally) a low-accuracy banner. Tapping the dial locks the current
heading as a target bearing. The settings screen persists (via DataStore): haptics on/off, default
north mode on launch, and coordinate display format (decimal/DMS).

Design intent: dark (`#000000` true black, AMOLED), one accent color (Princeton Orange
`#FF8600`), minimal chrome, one widget doing double duty (compass + level) rather than a
dashboard of separate cards.

## Package map

```
com.pavanpej.kompass
├── MainActivity.kt          Activity + Compose entry point. Local Screen (Compass/Settings)
│                            state + Crossfade between them (no Navigation-Compose dependency),
│                            permission-gated location/true-north wiring shared by both screens
│                            and the proactive lat/long permission request, splash screen install
├── CompassViewModel.kt      Owns CompassSensorManager, LocationTracker, SettingsRepository,
│                            north-mode/declination/locked-bearing/sun-moon-azimuth state, haptic
│                            feedback triggering (delegates decisions to FeedbackRules)
├── FeedbackRules.kt         Pure decision logic for haptic ticks (isLevel, isOnCardinal,
│                            isOnBearing, nearestCardinal, angularDistance) -- framework-free,
│                            unit tested
├── sensor/
│   ├── SensorData.kt        Immutable snapshot: azimuth, gravityX/Y/Z, magneticFieldMicroTesla,
│   │                        barometricAltitudeMeters, accuracy
│   ├── CompassSensorManager.kt   Wraps SensorManager: TYPE_ROTATION_VECTOR (azimuth) +
│   │                             TYPE_GRAVITY (tilt) + TYPE_MAGNETIC_FIELD (field strength) +
│   │                             TYPE_PRESSURE (altitude), exposes StateFlow<SensorData>
│   ├── AngleMath.kt         Pure angle math: shortestDelta, lowPass, unwrap, addDegrees.
│   │                        THIS FILE FIXES A REAL BUG -- see "The spin-reset bug" below.
│   ├── LevelMath.kt         Flat-surface tilt angles from gravity vector (horizontalTilt/
│   │                        verticalTilt). Deliberately NOT multi-orientation -- see below.
│   ├── MagnetometerMath.kt  Magnetic field strength (magnitude of the raw field vector)
│   ├── AltitudeMath.kt      Barometric altitude from pressure -- thin delegation to
│   │                        SensorManager.getAltitude() (see "Altimeter" below)
│   ├── CompassPoint.kt      16-point compass label (N, NNE, NE, ...) from an azimuth
│   ├── AccuracyPresentation.kt  When to show the "move in a figure-8" calibration prompt
│   └── NorthMode.kt         enum MAGNETIC / TRUE
├── location/
│   ├── LocationFix.kt       Immutable snapshot: latitude, longitude, altitudeMeters, accuracyMeters
│   ├── LocationTracker.kt   Continuous, lifecycle-managed LocationManager updates (replaced a
│   │                        one-shot lookup previously owned by DeclinationProvider itself)
│   ├── DeclinationProvider.kt   Resolves magnetic declination from a LocationFix (plain
│   │                             LocationManager elsewhere, no Play Services dependency)
│   ├── CoordinateFormat.kt  enum DECIMAL / DMS
│   └── CoordinateFormatter.kt   Pure lat/long -> display string formatting, both formats
├── astronomy/
│   └── CelestialMath.kt     Approximate sun/moon compass bearing from lat/long + time -- see
│                            "Sun/moon bearing accuracy" below before trusting these to a degree
├── settings/
│   └── SettingsRepository.kt   DataStore-backed persistence: haptics-enabled, default north
│                                 mode, coordinate format; survives app restarts
└── ui/
    ├── CompassDial.kt       The one big composable: dial canvas + bubble level + heading text +
    │                        tap-to-lock bearing marker + sun/moon spoke markers + lat/long (tap
    │                        to copy)
    ├── SegmentedToggle.kt   Generic subtle multi-option toggle (text + underline, no filled
    │                        "pill" background) -- replaced the old NorthModeToggle; reused for
    │                        north mode (compass screen + settings) and coordinate format
    ├── MagneticFieldReadout.kt   Small top-left readout, magnetic field strength
    ├── AltitudeReadout.kt   Small readout, barometric altitude -- see "Altimeter" below,
    │                        deliberately its own tiny composable so it's trivial to remove
    ├── AccuracyBanner.kt    Conditional low-accuracy prompt
    ├── SettingsScreen.kt    Haptics switch + default-north-mode row + coordinate-format row,
    │                        reuses SegmentedToggle
    └── theme/
        ├── Color.kt         KompassColors -- the ONLY place color hex values should live
        ├── Theme.kt         MaterialTheme wiring, always-dark (no light mode / dynamic color)
        └── Type.kt          Default Material3 Typography (mostly untouched)
```

Tests mirror this under `app/src/test/java/com/pavanpej/kompass/...` — one test file per pure
logic file. There are no tests for `CompassSensorManager`, `CompassViewModel`, `LocationTracker`,
`DeclinationProvider`, `SettingsRepository`, or anything Compose-UI-visual, because they need real
Android framework classes (SensorManager, LocationManager, Vibrator, DataStore, Canvas) that
require Robolectric or an instrumented device test to exercise meaningfully — that tradeoff was
deliberate, see "Testing philosophy" below. `CelestialMath` IS tested, but only structurally (see
"Sun/moon bearing accuracy" below) since there was no internet access available to verify against
a real ephemeris.

## Design decisions and history (read before changing sensor math)

### The spin-reset bug (`AngleMath`)

Early versions animated the raw 0–360° heading directly with `animateFloatAsState`. Compose's
animation has no concept of angle wraparound: when the heading crossed 359° → 1°, the animation
saw a jump of -358 (or interpreted it as needing to go the "long way"), and the dial visibly spun
a full 360° every time you crossed north. This was a real, reported, user-visible bug — not a
hypothetical.

Fix: `AngleMath.unwrap()` keeps a continuously-growing/shrinking angle (can exceed 360 or go
negative) by always stepping via the shortest arc (`shortestDelta`), and only the wrapped
`[0, 360)` value is used for display text. `CompassDial` animates the *unwrapped* value with a
fast `tween(80ms)`, then wraps only at the point of rendering the degree number and the dial
rotation. If you ever see a "dial spins in a full circle" bug again, this is the mechanism that's
supposed to prevent it — check whether some new code path animates a raw wrapped angle instead of
going through `AngleMath`.

`AngleMath.lowPass()` is the same shortest-arc logic applied as an exponential smoothing filter,
used in `CompassSensorManager` to denoise the raw magnetometer-derived azimuth before it's even
unwrapped for animation.

### Why the bubble level is flat-surface only (no "on its side" / "upright" mode)

An earlier iteration added an auto-adaptive multi-orientation level: it detected whether the
phone was flat, standing upright in portrait, or resting on its side, and remapped which pair of
gravity axes fed the crosshair accordingly (this lived in a since-deleted `TiltMath.kt`). It also
briefly existed as three separate widgets (a circular level + two "torpedo level" style linear
vials) before being merged into the auto-adaptive single circle.

**This was removed.** Root cause of why it didn't work well: the mode-detection picked an axis
(e.g. "gravityX is dominant") but not its *sign*, and the same physical mode (e.g. "resting on an
edge") occurs with that axis being either strongly positive or strongly negative depending on
*which* edge — left edge down vs. right edge down. The angle formulas assumed a fixed sign, so:
- One of the two physical orientations for a given mode worked, the mirrored one didn't.
- Near the sign flip, `atan2`'s reference direction effectively rotated ~180°, and "level" ended
  up represented as ±180° instead of 0° for the mirrored case — so the level-check threshold
  (`abs(angle) < 1°`) never fired, and small tilts near ±180° look like erratic jumps between
  +179° and −179° (the reported "flickering").

A correct fix requires 6 modes (axis × sign), doubling the hysteresis/testing surface, for a
fairly niche use case (balancing a phone on a 1cm edge). Given that cost/benefit, it was
deliberately simplified back down to flat-only. **Do not reintroduce multi-orientation leveling
without discussing the tradeoff again** — if you do, the sign-aware 6-mode fix above is the
correct approach, not a patch on the 3-mode version.

`LevelMath` today only computes `horizontalTilt`/`verticalTilt` from `(gravityX, gravityZ)` and
`(gravityY, gravityZ)` — meaningful only when the phone is roughly flat. This is intentional, not
an oversight.

### True north / magnetic declination

`DeclinationProvider.declinationFor(fix: LocationFix)` takes a location instead of looking one up
itself -- it used to call `LocationManager.getLastKnownLocation()` directly (a one-shot lookup),
but now that `LocationTracker` exists as a continuously-updated location source (needed for the
lat/long readout), declination is computed from that same shared fix instead of doing a second,
separate `LocationManager` call. `CompassViewModel` recomputes declination automatically whenever
a new fix arrives while in `NorthMode.TRUE` (see its `locationFix.onEach { ... }` in `init`).

Both `LocationTracker` and the old one-shot lookup use plain `android.location.LocationManager`,
not Play Services' `FusedLocationProviderClient` -- avoids pulling in the whole Play Services
Location dependency for what only needs occasional coarse updates.

**Known gap**: if the device has never gotten *any* location fix (from any app or provider),
`declinationDegrees` (and `locationFix`) stay `null` indefinitely, and the UI silently stays on
magnetic north / hides the lat/long readout even after permission is granted. There's no
user-visible "no location fix available yet" message. If you're asked to improve this UX, this is
the first gap to close.

### Location permission timing changed: proactive, not just lazy-on-toggle

Originally, `ACCESS_COARSE_LOCATION` was requested lazily, only when the user tapped the "TRUE"
north toggle -- matching the general "request permissions only when the feature that needs them
is invoked" principle. Once lat/long became a default-visible readout (not gated behind a toggle),
that principle still applies, but "the feature being invoked" is now "viewing the compass screen
at all" -- so `MainActivity` requests location permission proactively via a `LaunchedEffect(Unit)`
the first time the compass screen composes, not at app launch/splash. It's still lazy relative to
app install (not requested until the relevant screen is shown), still falls back gracefully if
denied (lat/long readout just doesn't appear), and reuses the same `requestLocationPermission`
helper the north-mode toggles use. If a location-permission request ever needs to move earlier
(e.g. onboarding flow) or later (e.g. a dedicated "enable location" prompt), this is the one place
to change.

### Altimeter (barometric altitude) -- explicitly tentative

`AltitudeMath.fromPressure()` delegates to `SensorManager.getAltitude()` rather than
reimplementing the barometric formula by hand. An earlier version *did* reimplement it, purely to
keep it callable from a plain-JVM unit test -- that was corrected once the "don't reinvent the
wheel" principle in AGENTS.md §3.1 was made explicit: Android already provides this exact
calculation correctly, so hand-duplicating it only adds a second place for a transcription bug to
hide. The tradeoff is that `AltitudeMath` isn't unit-testable outside an Android runtime anymore
(see docs/TESTING.md's "Not covered, and why" table) -- an accepted cost, not a gap.

It uses the *standard* sea-level pressure reference (1013.25 hPa) by default, not the actual local
sea-level pressure for the day, so the number will drift with weather -- it's relative/approximate,
not GPS-grade. The product decision
here (from the person who commissioned this feature) was explicitly "try it out, I might remove
it later" -- `AltitudeReadout` is kept as its own tiny composable specifically so removing it later
is a one-line change in `MainActivity`/`KompassScreen`, not a refactor.

### Sun/moon bearing accuracy

`CelestialMath.sunAzimuthDegrees()` follows the well-known NOAA solar position algorithm (accurate
to a small fraction of a degree). `CelestialMath.moonAzimuthDegrees()` uses only the dozen or so
largest-amplitude periodic terms from Meeus's lunar theory (Astronomical Algorithms, ch. 47)
rather than the full ~60-term series -- expect roughly a degree of error for the moon, not
arcminute precision. Both share one internal ecliptic-to-azimuth conversion path (computed via
right ascension/declination + Greenwich Mean Sidereal Time) so there's a single place to fix if
the underlying spherical-astronomy math needs correcting, rather than two independently-derived
azimuth formulas.

**This was written without internet access to verify against a real ephemeris service.** The
formulas are well-established published algorithms, transcribed carefully, but `CelestialMathTest`
deliberately only asserts *structural* properties (valid [0, 360) range, determinism, the expected
monotonic trend of solar azimuth through a day) rather than specific reference-checked degree
values, precisely because those reference values couldn't be independently verified. If you have a
way to check this against a real ephemeris (e.g. an online solar/lunar position calculator),
tightening these tests with verified reference values would be a good follow-up -- and would be
the moment to promote this from "probably right" to "verified."

**Deliberately not implemented**: GPS course-over-ground vs. magnetic heading delta, and MGRS
coordinate format -- both were discussed and explicitly declined/deferred by the person directing
this project, not overlooked. Don't add either without checking in first.

### Portrait-only lock

`AndroidManifest.xml` sets `android:screenOrientation="portrait"` on `MainActivity`. This isn't
just a UI preference: raw accelerometer/gravity sensor values are reported in the device's
*physical* frame, not the *logical screen* frame. If the activity were allowed to rotate to
landscape, the screen's logical axes stop matching the physical frame the sensor code assumes,
which was previously observed to break tilt readings. Don't remove the orientation lock without
also auditing every place gravity axes are consumed (`LevelMath`, and formerly `TiltMath`).

### Splash screen

Uses `androidx.core:core-splashscreen` (not just the platform SplashScreen API) specifically so
the black-background/orange-icon splash is consistent across API 26+ devices, not just API 31+
(where the OS splash screen API exists natively). `Theme.Kompass.Starting` (extends
`Theme.SplashScreen`) is the manifest theme; `MainActivity.onCreate()` calls
`installSplashScreen()` *before* `super.onCreate()`. If the splash ever shows white again, check
that both the `<application>` and `<activity>` manifest themes still point at
`Theme.Kompass.Starting`, and that `windowBackground` on `Theme.Kompass` itself is still black
(there's a brief window between the splash handing off and Compose's first frame where the raw
`windowBackground` shows).

### App icon / in-app pointer arrow visual consistency

The launcher icon (`res/drawable/ic_launcher_foreground.xml`) is a small, mathematically-centered
notched arrowhead (Princeton Orange on true black), rotated 45° to face north-east. Its centroid
(not just its bounding box) was computed by hand via the shoelace formula and the shape shifted so
the centroid lands exactly on the icon's pivot — an off-center bounding box with a symmetric
centroid still *looks* off-center after rotation, which is a real trap if you resize/redesign it.

The in-app fixed heading-pointer arrow (drawn in `CompassDial`'s `Canvas`) deliberately echoes the
same notched-arrowhead silhouette, for visual consistency between the launcher icon and in-app UI.
It's drawn as fill + a round-joined/capped stroke of the same color layered on top, which is the
same trick used in the vector-drawable icon to fake rounded corners (Compose Canvas has no
"corner radius" for arbitrary filled polygons).

### Bearing lock

Tapping the compass dial (`CompassDial`'s `clickable` modifier, wrapping the whole `Canvas`) locks
the currently *displayed* heading (already north-mode-adjusted, not the raw magnetic azimuth) as a
target bearing via `CompassViewModel.toggleBearingLock`. Tapping again clears it. While locked, a
marker is drawn on the rotating dial ring at that bearing (same rotate-with-the-dial treatment as
the N/E/S/W labels — it's a fixed compass bearing, not a device-relative value, so it must NOT be
drawn in the same "fixed" layer as the heading-pointer arrow or the bubble level), and
`CompassViewModel.checkFeedback` fires a haptic tick via `FeedbackRules.isOnBearing` when the
*displayed* azimuth matches it again. Important: `checkFeedback` computes its own north-mode-adjusted
azimuth from raw `SensorData` (mirroring the adjustment `MainActivity`/`KompassScreen` applies for
display) specifically so the haptic fires in the same reference frame the user sees and tapped in
— locking against raw magnetic azimuth while displaying true-north-adjusted heading (or vice
versa) would silently misfire the haptic relative to what's on screen.

### Settings persistence and the permission-gating trap

Two preferences persist across restarts via `SettingsRepository` (DataStore, not SharedPreferences
— DataStore is the current recommended approach and has a straightforward `Flow`-based API):
haptics-enabled and default-north-mode-on-launch.

**A real bug caught before shipping**: the first version of the settings screen's default-north-mode
toggle called `CompassViewModel.setDefaultNorthMode()` (which applies the mode immediately) directly
from the UI, separately from the permission check — meaning selecting "TRUE" in settings flipped
the runtime mode to `NorthMode.TRUE` *before* `ACCESS_COARSE_LOCATION` was actually confirmed
granted. It happened to fail safe (no permission means `declinationDegrees` stays null, so display
falls back to magnetic anyway), but the toggle's own visual state would show "TRUE" selected
without permission having been granted — misleading. Fixed by routing *every* place that needs
location permission (the compass screen's north-mode toggle, the settings screen's default-mode
toggle, and later the proactive lat/long request) through one shared
`requestLocationPermission(onGranted: () -> Unit)` helper in `MainActivity` that only invokes the
callback after `ContextCompat.checkSelfPermission` confirms grant (or after the runtime permission
dialog resolves positively, via a `pendingLocationAction` held across the
`rememberLauncherForActivityResult` callback). If you add a fourth place that needs location
permission, route it through this same helper rather than checking permission ad hoc.

### North-mode toggle: subtle styling + bottom-bar placement for adaptivity

The north-mode toggle was originally a solid-orange-fill "pill" sitting directly below the dial.
Restyled into `SegmentedToggle` (text labels + a thin underline on the selected option, no fill)
because the solid fill read as a prominent call-to-action button, when it's meant to be a quiet
state indicator. It also moved from the scrollable body content into `Scaffold`'s `bottomBar` slot
in `MainActivity` (only rendered when `screen == Screen.Compass`) specifically so it adapts to
different screen sizes automatically -- `Scaffold` reserves space for `bottomBar` and applies
window-insets-aware padding to the body content itself, which is more robust across phones/
tablets/foldables than trying to hand-place it with `Modifier.align` + manual padding. **General
principle for this app going forward: prefer `Scaffold` slots, `weight()`, and scrollable
containers over fixed `dp` offsets when positioning things — the explicit product requirement is
that the UI adapts to all screen sizes, not just the one device it was built against.**

### CI pipeline

Four files, split by concern rather than one mega-workflow:

- **`.github/workflows/ci.yml`** — runs on every push to `main` and every PR. ktlint → Android
  Lint → unit tests → Jacoco coverage report (uploaded as an artifact, not gated on a threshold
  yet) → `assembleDebug` → uploads the debug APK as a build artifact. A separate PR-only job
  diffs `AndroidManifest.xml` against the base branch and posts a non-blocking `::warning::`
  annotation if a new `<uses-permission>` shows up, so it's never silently added without the
  README's permissions table being updated to match.
- **`.github/workflows/codeql.yml`** — kept separate from `ci.yml` because it has its own
  independent schedule (push/PR to `main` *plus* a weekly cron), not just a push/PR trigger.
- **`.github/workflows/release.yml`** — triggers on pushing a `v*` tag. Builds a **signed**
  release APK *and* AAB (`assembleRelease bundleRelease` in one Gradle invocation, so both share
  the same signing config without building twice) and creates a GitHub Release with both attached
  (the AAB is also uploaded separately as a build artifact). The AAB is what Play Console actually
  requires for upload -- the APK is kept around for direct/sideload installs, since that's what
  most people expect a GitHub Release asset to be. See "Release signing" below for how signing
  actually works, and "Version-from-tag" for where `versionName`/`versionCode` come from.
- **`.github/dependabot.yml`** — not a workflow (lives in `.github/`, not `.github/workflows/`),
  monthly-interval dependency PRs for both the `gradle` and `github-actions` ecosystems.

**Repo visibility is load-bearing for two of these.** CodeQL's `analyze` step scans fine
regardless, but *uploading* results to GitHub's code scanning UI needs GitHub Advanced Security,
which isn't available for private repos on the free plan ("Code scanning is not enabled for this
repository" — confirmed by actually running it against this repo while it was briefly private).
Branch protection with required status checks has the identical public-repo-or-paid-plan
constraint. This repo is public specifically so both of these work; if it ever goes private again,
expect both to break the same way, not as a new bug.

`main` currently requires only the **`CI / build-and-test`** status check to pass before merging
(`gh api repos/.../branches/main/protection`). Deliberately not required:
`CI / permission-check` (it's designed to always pass — see above, it only posts a warning
annotation, never fails — so requiring it is a no-op) and `CodeQL / Analyze` (the job succeeds
once it runs regardless of what it finds; findings surface as separate code-scanning alerts to
review manually, not as this check failing, so requiring it only guarantees the scan *ran* on
every PR, not that the code is clean).

### Release signing

`app/build.gradle.kts`'s `signingConfigs { create("release") { ... } }` block reads four
environment variables (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) and, **only if they're present**, decodes the
base64'd keystore to a file under the build directory and wires up signing for the `release`
build type. If they're absent (any local build, or a CI run without the secrets configured),
`assembleRelease` still succeeds -- it just produces `app-release-unsigned.apk` instead of
`app-release.apk` (verified locally: this is the actual AGP output-naming behavior, not a guess).
`bundleRelease` (the AAB) behaves differently here -- it always names its output
`app-release.aab` regardless of signed state (no `-unsigned` suffix convention like the APK has;
also verified locally), so don't use the filename as a signal for whether an AAB is actually
signed -- only the presence of the four env vars at build time determines that.
`release.yml` is the only workflow that sets these, from four GitHub Actions repo secrets of the
same names. The actual keystore file lives outside this repo entirely, on the maintainer's
machine (backed up separately) -- **never commit a real keystore**; `.gitignore` blocks `*.jks`/
`*.keystore`/`keystore.properties` as a backstop regardless of where one gets created.

### Version-from-tag

`defaultConfig`'s `versionCode`/`versionName` read from Gradle project properties
(`-PreleaseVersionCode=`/`-PreleaseVersionName=`) with the current hardcoded values (`1`/`"1.0"`)
as fallback defaults for local/debug builds. `release.yml` derives these from the pushed tag
itself (`versionName` = tag with the leading `v` stripped, e.g. `v1.2.0` → `1.2.0`; `versionCode`
= the GitHub Actions run number, which is guaranteed monotonically increasing) and passes them as
those properties -- so cutting a release never requires a manual version-bump commit to this file.

### Jacoco coverage report task

`jacocoTestReport` in `app/build.gradle.kts` is a hand-registered `JacocoReport` task (AGP has no
one-line "just give me a coverage report" API for a Compose project). One specific pitfall hit
and fixed while wiring it up: the task's `executionData` was originally globbed across the
*entire* build directory (`fileTree(layout.buildDirectory.get()) { include("**/*.exec") } }`),
which Gradle 9's stricter task-validation flags as an **undeclared implicit dependency** -- the
glob happened to also match outputs from unrelated tasks (asset merging, dexing) that
`jacocoTestReport` never declared a dependency on, so Gradle couldn't guarantee correct task
ordering. Fixed by scoping `executionData` to exactly
`outputs/unit_test_code_coverage/debugUnitTest/` -- the specific directory `testDebugUnitTest`
(an explicit `dependsOn`) writes its `.exec` file to, and nothing else touches. **If you add
another coverage-consuming task later, scope its file trees narrowly the same way** rather than
globbing the whole build directory "to be safe" -- that's exactly backwards for Gradle's
validation.

### Retrofitting ktlint onto an existing codebase

Running ktlint 1.5.0 with its bare default ruleset against the pre-existing codebase produced
~150 violations across nearly every file -- not because the code was inconsistent, but because
ktlint 1.x's default style is opinionated in ways that don't match how this project was actually
written: one-parameter-per-line function signatures, mandatory trailing commas on every call site,
forced line breaks for any multiline expression. Reformatting the entire codebase to match would
have been a large, purely-cosmetic diff unrelated to any real bug or inconsistency.

Instead, `.editorconfig` disables that specific set of style-preference rules (each with a comment
explaining why), while keeping the rules that catch genuine consistency issues: import ordering,
no wildcard imports, final newlines, indentation, spacing, naming. Two rules needed a targeted
fix rather than a blanket disable:
- `ktlint_function_naming_ignore_when_annotated_with = Composable` -- Compose's own official style
  guide mandates PascalCase for `@Composable` functions (`CompassDial`, not `compassDial`); without
  this override ktlint's naming rule would flag every single composable in the codebase.
- `ktlint_standard_max-line-length` is disabled entirely, specifically because it conflicts with
  this project's own documented testing convention (docs/TESTING.md) of long, full-sentence
  backtick test names -- wrapping those to fit a line limit would actively fight that convention.

The remaining genuine issues (a handful of missing braces on single-line `if`/`else`, one
mis-ordered import, a couple of missing final newlines) were fixed via `ktlint -F`, and the
unused default-template `ExampleInstrumentedTest.kt` was deleted outright rather than reformatted,
since it was already documented as unexercised boilerplate with zero value.

If you add new ktlint rule violations and are tempted to just disable the rule: check first
whether it's flagging something ktlint 1.x's default style prefers (probably fine to disable,
following the pattern above) versus something that's a genuine, previously-consistent convention
in this codebase getting broken by new code (fix the new code instead).

## Testing philosophy

See **[docs/TESTING.md](TESTING.md)** — the canonical home for testing philosophy, the paired-test
requirement for new logic, and a human-auditable directory of every test file and what it covers.
Read it before adding, moving, or removing any test.

## Known limitations / open TODOs

- No location-fix-unavailable messaging for true north (see "True north" above).
- No Compose UI tests at all -- the default-template instrumented test was removed (see "CI
  pipeline" below) since it was pure unexercised boilerplate; there is currently zero automated
  coverage of the settings screen, the bearing-lock tap gesture, or any Canvas-drawn UI.
- No CodeQL/ktlint/Jacoco baseline history yet to compare against -- this is genuinely the first
  run of all of it (see "CI pipeline" below for what's actually wired up now).
- The multi-orientation level (see above) is gone; if re-requested, treat it as a fresh design
  problem (6 modes, not 3) rather than restoring the deleted `TiltMath.kt` as-is.
- The locked bearing does not persist across app restarts (by design so far — it's a session-scoped
  "remember where I was pointing" aid, not a saved waypoint). If asked for saved/named bearings,
  that's a new feature, not a bug fix.
- No "location fix unavailable yet" messaging for lat/long, sun/moon bearing, or true north — all
  three silently stay hidden/unavailable until a fix arrives, with no in-between state shown.
- The altimeter is explicitly tentative (see "Altimeter" above) and may be removed later per
  product decision — don't be surprised if it's gone in a future session, and don't treat its
  presence as a stable contract.
- Sun/moon bearing math is unverified against a real ephemeris (see "Sun/moon bearing accuracy"
  above) — treat it as "probably correct," not confirmed, until someone checks it against a real
  source.
- GPS course-over-ground vs. magnetic heading delta, and MGRS coordinate format, were both
  discussed and explicitly deferred/declined — not gaps to silently fill in.

## Conventions for AI agents working on this repo

- **Color values**: only ever add/change colors in `ui/theme/Color.kt` (`KompassColors`). Never
  hardcode a hex value inside a composable.
- **Before claiming a change works**: run `./gradlew testDebugUnitTest assembleDebug` (both — unit
  tests don't catch Compose layout/compile issues, and a debug build doesn't catch logic
  regressions). Neither substitutes for actually running the app on a device for anything
  sensor/UI-feel related (animation speed, haptic timing, visual centering) — say so explicitly
  if you haven't verified on-device.
- **Sensor math changes**: check whether `AngleMath`, `LevelMath`, or `FeedbackRules` already
  cover the case before writing new math inline. If genuinely new math, extract it the same way
  (pure object, own file, paired test file).
- **Don't reintroduce removed features silently** — if asked for something that sounds like the
  old multi-orientation level or the old linear "torpedo" vials, mention the history above rather
  than assuming it should come back in its old form.
