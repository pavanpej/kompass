# Store assets

Google Play Console submission assets for Kompass.

- **`icon-512.png`** (512×512) — a full-bleed square, no pre-baked corner rounding, rasterized
  from the same path data as the real launcher icon but using the flat square background
  (`ic_launcher_background.xml`'s pathData, not `docs/images/logo.svg`'s rounded-rect version).
  Play Store applies its own corner mask on upload -- pre-rounding here would double up with
  that. `docs/images/logo.svg` (rounded corners) is correct for the README, where nothing else
  applies a mask; it is NOT the right source for this file. Regenerate with `sharp` if the real
  icon ever changes (see git history for the generation script).
- **`feature-graphic-1024x500.png`** (1024×500) — same icon, composited with a "Kompass"
  wordmark, using the app's own color palette (`#000000` background, `#FF8600` accent).
- **`screenshots/`** — real on-device captures (`adb exec-out screencap`), not mockups or an
  emulator: main compass screen, settings screen, and a locked-bearing state (showing the
  sun/moon markers too). Any screenshot showing lat/long has had it overwritten with dummy
  coordinates (San Francisco) before committing -- the originals had the maintainer's real
  location baked into a screenshot destined for a public store listing. If regenerating, redact
  location the same way before committing; don't skip that step.
