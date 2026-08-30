# Store assets

Google Play Console submission assets for Kompass.

- **`icon-512.png`** (512×512) — rasterized directly from the same path data as the real
  launcher icon (`docs/images/logo.svg`), not a re-drawing. Regenerate with `sharp` if the real
  icon ever changes — see git history for the generation script (rendered the SVG at 512×512,
  no manual coordinate work).
- **`feature-graphic-1024x500.png`** (1024×500) — same icon, composited with a "Kompass"
  wordmark, using the app's own color palette (`#000000` background, `#FF8600` accent).
- **`screenshots/`** — real on-device captures (`adb exec-out screencap`), not mockups or an
  emulator. `01-compass.png`'s lat/long has been overwritten with dummy coordinates (San
  Francisco) before committing -- the original had the maintainer's real location baked into a
  screenshot destined for a public store listing. If regenerating, redact location the same way
  before committing; don't skip that step.
