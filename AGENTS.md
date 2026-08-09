# AI agent instructions — Kompass

These instructions apply to any AI agent (Claude, GPT, Gemini, or otherwise) working in this
repository, in any session. They are not optional context — treat them as required reading before
writing or editing any code here.

## 1. Read the existing docs before starting work

Before making any change, read, in this order:

1. **[README.md](README.md)** — what the app is, how to build/run/test it, permissions, current
   status, and the CI pipeline (`.github/workflows/`).
2. **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — package map, and critically, the "Design
   decisions and history" section. This documents bugs that were already found and fixed once
   (the compass spin-reset bug), features that were built, found to be broken, and deliberately
   removed (the multi-orientation bubble level), and constraints that look arbitrary but aren't
   (the portrait lock, the splash screen setup, why declination uses `LocationManager` and not
   Play Services).
3. **[docs/TESTING.md](docs/TESTING.md)** — testing philosophy, the paired-test requirement (see
   §3.2 below), and a human-auditable table of every test file, what it covers, and what's
   deliberately not covered yet.

Do not re-derive decisions that are already explained in these docs. Do not silently reintroduce
something the docs say was deliberately removed (e.g. multi-orientation leveling) without
surfacing that history to the user first. If a task seems to conflict with something documented,
say so explicitly rather than guessing.

## 2. Update the docs when you change things

If your changes affect anything the docs describe — architecture, package layout, a documented
design decision, build/test steps, permissions, dependencies, known limitations, or tests —
**update README.md, docs/ARCHITECTURE.md, and/or docs/TESTING.md in the same session**, before
considering the task done. Specifically:

- New non-trivial design decision, tradeoff, or bug fix with a non-obvious root cause → add it to
  the "Design decisions and history" section of `docs/ARCHITECTURE.md`, following the existing
  style (what happened, why, what the fix was, what to watch for if it regresses).
- New package/file with a real responsibility → update the package map in
  `docs/ARCHITECTURE.md`.
- New dependency, permission, or changed build/test step → update `README.md`.
- Feature removed → don't just delete the code silently; note in `docs/ARCHITECTURE.md` why, the
  same way the multi-orientation level removal is documented, so a future session doesn't
  rebuild it from scratch without knowing it was already tried.
- Resolved a "Known limitation" from the docs → remove or update that entry.
- **New test file added, or an existing one's scope changes** → update the test directory table
  in `docs/TESTING.md` (file, test count, what it covers). Re-check the count against
  `app/build/test-results/testDebugUnitTest/*.xml` — don't guess.

Docs that drift out of sync with the code are worse than no docs (they actively mislead the next
session). Treat doc updates as part of the change, not a follow-up task.

## 3. Best practices for this stack (Kotlin + Jetpack Compose + Android)

This project follows these conventions already; keep following them rather than introducing
inconsistent patterns.

### 3.1 Extract pure logic, keep it framework-free -- but don't reinvent what the platform already provides

Non-trivial math or decision logic that this app itself is inventing (angle handling, thresholds,
mode selection, label mapping, haptic-trigger rules) should live in small, framework-free Kotlin
objects/functions — no `Context`, `SensorManager`, `Canvas`, or other Android classes in the
signature. This is what makes `AngleMath`, `LevelMath`, `CompassPoint`, `AccuracyPresentation`,
and `FeedbackRules` testable in plain JUnit on the JVM, with no emulator or Robolectric. When you
write new logic like this, follow the same pattern: extract it, don't bury it inline inside a
`ViewModel`, `SensorEventListener`, or Composable.

**This does not mean reimplement platform math to make it plain-JVM testable.** If the Android
SDK already provides a correct, official utility for the exact computation you need — e.g.
`SensorManager.getAltitude()` for barometric altitude, `GeomagneticField.declination` for magnetic
declination — call that directly. Do not hand-transcribe the same formula into your own object
just so a unit test can invoke it outside an Android runtime. This happened once already:
`AltitudeMath` originally reimplemented the barometric formula by hand for testability, duplicating
`SensorManager.getAltitude()`, which does the identical calculation. It was corrected to delegate
to the platform method instead, once this was called out explicitly. Reimplementing platform math
is itself a bug risk (transcription errors, missed edge cases the platform already handles) and
duplicates logic Google has already written and tested — plain-JVM testability is a nice property
when it's free, not a goal worth an incorrect-by-duplication tradeoff.

The practical corollary: when a piece of logic is "just call the platform API," it doesn't get a
paired pure-JVM test (§3.2) — it goes in `docs/TESTING.md`'s "Not covered, and why" table instead,
with the reason being "delegates to platform API X, not reimplemented." That's a deliberate,
acceptable outcome, not a gap to fill by writing a Robolectric test purely for coverage's sake,
unless there's app-specific logic *around* the platform call (e.g. smoothing, unit conversion,
fallback selection) that's worth testing in isolation.

### 3.2 Testing convention: Given/When/Then names, no BDD framework dependency — and it's mandatory

Tests are plain JUnit4 with backtick method names written as full sentences:
`` `given X, when Y, then Z`() ``. This is deliberate — it makes the test name itself the
documentation, readable in an IDE test tree or a diff without reading assertion code, and avoids
adding a BDD framework (Kotest/Spek) as a dependency. Keep using this style for new tests. One test
file per pure-logic file/object, mirroring the source package structure under `app/src/test/`.

**Every non-trivial pure function or decision object you write or modify must have a paired test
file in this style, added in the same change** — *unless* it's a thin delegation to a platform API
per §3.1's "don't reinvent the wheel" qualifier, in which case it belongs in the "not covered"
table instead. Outside that exception, this isn't a suggestion — it's the same rule §3.1's
extraction pattern exists to enable. If you follow §3.1 (extract logic into a framework-free
object) but skip the paired test, you've done half the job. See
**[docs/TESTING.md](docs/TESTING.md)** for the full directory of existing tests (add your new file
to that table) and the explicit list of what's deliberately *not* unit-tested (ViewModel, Compose
UI, sensor/location glue) and why — don't add tests for those without first reading why they were
left out.

### 3.3 Compose: hoist state, keep composables side-effect-honest

- Keep Composables focused on rendering; push state ownership up to a `ViewModel` (see
  `CompassViewModel`) or the caller, and pass values + callbacks down. Don't reach into Android
  framework services from inside a Composable body directly — use `LocalContext.current` only
  at the point you actually need a `Context` (e.g. a permission check), not as a general escape
  hatch.
- Side effects (sensor registration, permission requests, anything with a lifecycle) belong in
  `LaunchedEffect`, `DisposableEffect`, or a `ViewModel`'s `init`/`onCleared` — never directly in
  the composable function body outside of those.
- Animating a value that wraps (like a compass heading 0–360°) needs special handling —
  `animateFloatAsState` has no concept of modular arithmetic. See `AngleMath.unwrap` and the
  "spin-reset bug" writeup in `docs/ARCHITECTURE.md` before animating any angle.
- Prefer `remember`/`derivedStateOf` over recomputing expensive values on every recomposition, but
  don't over-apply this to genuinely cheap computations — it adds indirection for no benefit.

### 3.4 Sensors and other hardware: lifecycle-aware registration

Sensor listeners, location requests, and vibrators must be registered in a lifecycle-aware way
(started/stopped alongside the owning component, e.g. `CompassSensorManager.start()`/`stop()`
called from `CompassViewModel`'s `init`/`onCleared`) to avoid leaks and unnecessary battery drain.
Never register a sensor listener and forget to unregister it. When adding new sensor types, prefer
the least battery-intensive sensor rate that still meets the UX need (`SENSOR_DELAY_GAME` is
already a deliberate choice here, not the fastest available rate).

### 3.5 Permissions: request lazily, fail gracefully

Request runtime permissions only when the feature that needs them is actually invoked (see the
true-north toggle requesting `ACCESS_COARSE_LOCATION` only on tap, not on app launch), and always
have a graceful fallback if denied (falls back to magnetic north silently) rather than a hard
failure or a repeated nagging prompt.

### 3.6 Gradle / dependency management

All dependency versions go through `gradle/libs.versions.toml` (the version catalog) — never
hardcode a version string directly in `build.gradle.kts`. Keep one dependency = one entry, and
name aliases consistently with the existing `androidx-*` / plain-name convention already in the
catalog.

### 3.7 Design system discipline

All colors go through `ui/theme/KompassColors` — never hardcode a hex value inside a composable
or drawable path unless it's a one-off asset like the launcher icon (which has its own literal
colors matching the design system's values). If you need a new semantic color, add it to
`KompassColors` with a comment explaining when to use it, the same way `SurfaceElevated` is
documented there.

### 3.8 Verify before claiming done

Before reporting a change as complete:
- Run `./gradlew testDebugUnitTest` — fast, catches logic regressions.
- Run `./gradlew assembleDebug` — catches Compose/resource/manifest compile errors that unit tests
  won't.
- Run ktlint (see README's "Code style" section for the exact command) — CI enforces this on every
  PR; a change that fails it locally will fail `ci.yml` too.
- Neither of the above substitutes for running the app on a physical device for anything that's
  actually about feel — animation speed, haptic timing, visual centering/spacing, sensor
  responsiveness. If you haven't verified something on-device, say so explicitly rather than
  implying it's confirmed working.

### 3.9 Kotlin idioms

Favor `data class` for immutable value holders (`SensorData`, `TiltReading`-style shapes),
`enum class` for closed sets of modes (`NorthMode`), `object` for stateless
namespaces/singletons (`AngleMath`, `FeedbackRules`), and top-level `private const val` for
magic numbers instead of inlining them (see `LEVEL_THRESHOLD_DEGREES`, `MAX_TILT_DEGREES` for the
existing pattern). Avoid `!!` outside of cases where nullability has just been explicitly checked
in the same scope.
