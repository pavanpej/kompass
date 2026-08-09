# Kompass — Testing

This is the canonical home for the project's testing philosophy and a human-auditable directory
of every test file that exists. If you add, remove, or move a test file, update the directory
table below in the same change — an out-of-date test directory is worse than none.

## Philosophy

1. **Extract pure logic, then test it on the plain JVM.** Non-trivial math or decision logic
   (angle handling, thresholds, mode selection, label mapping) is pulled out of anything that
   needs `Context`, `SensorManager`, `Canvas`, `Vibrator`, or other Android framework classes,
   into small framework-free Kotlin `object`s/functions. That's what lets the entire suite run as
   plain JUnit4 — no Robolectric, no emulator, no instrumented device — in a couple of seconds.
2. **Given/When/Then names, no BDD framework.** Every test method name is a full sentence in
   backticks: `` `given X, when Y, then Z`() ``. No Kotest/Spek dependency was added for this —
   the sentence-as-method-name *is* the spec. This is deliberate specifically so a human (or an
   external reviewing AI agent) can audit test coverage by reading method names in an IDE test
   tree or a `git diff`, without reading a single assertion.
3. **One test file per pure-logic file**, mirroring the source package path under
   `app/src/test/java/com/pavanpej/kompass/...`.
4. **Every new non-trivial pure function or decision object gets a paired test file in the same
   change**, following this exact pattern. This is not optional cleanup — do it before considering
   the change done. If you're not sure whether new logic is "non-trivial" enough to warrant this,
   default to yes: the cost of a small test file is low, the cost of an untested angle-wraparound-
   style bug shipping again is not (see `docs/ARCHITECTURE.md`'s spin-reset bug writeup).
5. **Exception: don't reimplement platform math just to make it plain-JVM testable.** If Android
   already provides a correct utility for the exact computation (`SensorManager.getAltitude()`,
   `GeomagneticField.declination`, etc.), call it directly rather than hand-transcribing the same
   formula into an object of our own. That logic goes in the "Not covered, and why" table below
   instead of getting a paired test — see AGENTS.md §3.1 for the full reasoning (this happened
   once already with `AltitudeMath`, corrected after the fact).

## Test directory

Human-auditable map of every test file, what it covers, and how many cases it has. Counts current
as of the last time this file was updated — re-run `./gradlew testDebugUnitTest` and check
`app/build/test-results/testDebugUnitTest/*.xml` if you need the live count.

| Test file | Tests | Source under test | What it covers |
|---|---|---|---|
| [`sensor/AngleMathTest.kt`](../app/src/test/java/com/pavanpej/kompass/sensor/AngleMathTest.kt) | 13 | [`sensor/AngleMath.kt`](../app/src/main/java/com/pavanpej/kompass/sensor/AngleMath.kt) | Shortest-arc angle delta, exponential smoothing across the 0/360 seam, the unwrap-for-animation logic (this is the regression suite for the "spin-reset bug" — see `docs/ARCHITECTURE.md`), and declination addition/wraparound. |
| [`sensor/CompassPointTest.kt`](../app/src/test/java/com/pavanpej/kompass/sensor/CompassPointTest.kt) | 9 | [`sensor/CompassPoint.kt`](../app/src/main/java/com/pavanpej/kompass/sensor/CompassPoint.kt) | 16-point compass label mapping (N/NE/E/.../NNW), including wraparound at 0°/360° and boundary cases near sector edges. |
| [`sensor/AccuracyPresentationTest.kt`](../app/src/test/java/com/pavanpej/kompass/sensor/AccuracyPresentationTest.kt) | 4 | [`sensor/AccuracyPresentation.kt`](../app/src/main/java/com/pavanpej/kompass/sensor/AccuracyPresentation.kt) | Which `SensorManager` accuracy levels trigger the "move in a figure-8" calibration banner. |
| [`sensor/LevelMathTest.kt`](../app/src/test/java/com/pavanpej/kompass/sensor/LevelMathTest.kt) | 4 | [`sensor/LevelMath.kt`](../app/src/main/java/com/pavanpej/kompass/sensor/LevelMath.kt) | Flat-surface horizontal/vertical tilt angle derivation from the gravity vector. |
| [`sensor/MagnetometerMathTest.kt`](../app/src/test/java/com/pavanpej/kompass/sensor/MagnetometerMathTest.kt) | 4 | [`sensor/MagnetometerMath.kt`](../app/src/main/java/com/pavanpej/kompass/sensor/MagnetometerMath.kt) | Magnetic field magnitude from the raw 3-axis field vector. |
| [`location/CoordinateFormatterTest.kt`](../app/src/test/java/com/pavanpej/kompass/location/CoordinateFormatterTest.kt) | 4 | [`location/CoordinateFormatter.kt`](../app/src/main/java/com/pavanpej/kompass/location/CoordinateFormatter.kt) | Decimal and DMS lat/long formatting, hemisphere letters (N/S/E/W) for both signs, zero-padding in DMS seconds. |
| [`astronomy/CelestialMathTest.kt`](../app/src/test/java/com/pavanpej/kompass/astronomy/CelestialMathTest.kt) | 6 | [`astronomy/CelestialMath.kt`](../app/src/main/java/com/pavanpej/kompass/astronomy/CelestialMath.kt) | **Structural only** (valid range, determinism, expected daily trend) — NOT verified against a real ephemeris; see `docs/ARCHITECTURE.md`'s "Sun/moon bearing accuracy" before treating this as a precision guarantee. |
| [`FeedbackRulesTest.kt`](../app/src/test/java/com/pavanpej/kompass/FeedbackRulesTest.kt) | 15 | [`FeedbackRules.kt`](../app/src/main/java/com/pavanpej/kompass/FeedbackRules.kt) | Haptic-tick decision rules: nearest cardinal direction, angular distance (seam-safe), "is on a cardinal", "is level", "is on a locked target bearing". |

**Total: 59 tests, all plain JUnit4, all pure-function/pure-object, zero Android framework
dependencies.**

## Not covered, and why

| Not tested | Why | What would be needed |
|---|---|---|
| `CompassSensorManager` | Registers real `SensorManager` listeners; the interesting logic it calls (`AngleMath`) is already tested directly | Robolectric or an instrumented on-device test |
| `CompassViewModel` | Constructs a real `Vibrator`/`CompassSensorManager` via `Application` context; its decision logic is delegated to the already-tested `FeedbackRules`/`LevelMath` | Robolectric, or refactor to inject fakes |
| `DeclinationProvider` | Calls real `GeomagneticField` | Robolectric with shadow location providers |
| `AltitudeMath` | Delegates to platform `SensorManager.getAltitude()` rather than reimplementing the barometric formula (see AGENTS.md §3.1 "don't reinvent the wheel") | Robolectric, if the delegation logic itself ever grows beyond a one-line call |
| `LocationTracker` | Registers real `LocationManager` listeners | Robolectric with shadow location providers, or an instrumented on-device test |
| `SettingsRepository` | Calls real DataStore (`Context.dataStore`) | Robolectric, or an instrumented test with a test DataStore instance |
| `CompassDial` / any Composable | Compose UI rendering, Canvas drawing, animation feel, tap-to-lock gesture | Compose UI testing (`androidx.compose.ui.test`) — the `androidx-compose-ui-test-junit4` dependency is already present but unused for this purpose |
| `MainActivity` permission flow | Activity Result API + real permission dialog, including the `pendingLocationAction` routing between the proactive lat/long request and the north-mode toggles | Instrumented UI test |

None of these gaps are accidental oversights — they're the deliberate boundary of "keep the whole
suite running on the plain JVM in seconds." If a future session decides that boundary should move
(e.g. adding Robolectric for `CompassViewModel` coverage), update this table and the note above,
don't just add the tests silently.

## Running tests

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat testDebugUnitTest
```

Results: `app/build/test-results/testDebugUnitTest/*.xml` (one file per test class). A quick way
to get a human-readable per-class summary:

```bash
grep -h "tests=" app/build/test-results/testDebugUnitTest/*.xml
```
