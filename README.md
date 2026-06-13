# CardFit

A privacy-first, **fully-offline** Android app for everyday document paperwork. Scan IDs and
documents, prepare ID/passport photos, and bundle a family's paperwork into one application set —
laid out for printing at true physical size or compressed for portal upload.

**Everything runs on-device. No internet permission, no cloud, no network, no analytics, no ads.**

- **Package ID:** `in.firm.consultancy.bayaan.cardfit` (permanent)
- Kotlin · Jetpack Compose · Material 3 · MVVM (single-module app)
- By [Bayaan Consultancy](https://bayaan.consultancy.firm.in/)

## Features

CardFit opens to three flows:

### 📄 Documents & cards
- Scan one or both sides of any document or ID with the on-device ML Kit document scanner.
- Both sides are laid out **centered on a single page** — at true physical size for print, or
  compressed under a KB cap for upload.
- **Automatic card-type detection** and orientation-aware sizing (PAN, Aadhaar, Voter ID/EPIC,
  admit cards, custom, or free) with a manual override.
- **Rounded-corner trim** for PVC cards (PAN/Aadhaar/Voter ID): trims each card's corners to the true
  ISO ID-1 radius and fills them white, removing the off-colour corner spots left when a card is
  scanned on a coloured surface (bedsheet, tabletop). On by default for those cards; toggle off for
  square-corner paper cards.
- **Searchable PDF text layer** (an invisible, on-device OCR layer so the PDF is selectable) — opt-in,
  off by default for privacy.
- **OCR name suggestion** fills the filename field from the scan (a suggestion only — never
  auto-finalized, and identity numbers are never extracted).

### 🖼️ Photo
- Capture with the camera or pick from the gallery (EXIF orientation is honored, so captures come up
  upright).
- **Edit** on one screen with a pinned live preview: a fixed-frame crop you **pinch-zoom and drag**
  to frame the face, rotate, press-and-hold **Compare** with the original, opt-in **background
  removal** (on-device ML Kit subject segmentation), auto-enhance, and advanced brightness / contrast
  / saturation.
- Pick a size — **Passport, Visa, Stamp, or Custom** — and the crop aperture locks to it.
- **Export** as a single upload JPEG (exact pixels at a chosen DPI, optional KB cap) or a single-page
  **print grid PDF** (chosen paper, copy count snapped to whole rows, photos anchored to the top
  margin, a light-gray border around each).

### 🗂️ Tasks
- Collect several people's documents and photos into one task (persisted across restarts).
- Reorder / rename / delete entries, set per-entry upload caps.
- Export each individually or as one **combined multi-page PDF**.

All artwork (card/photo/format tiles) is original schematic illustration — no real logos, emblems,
or seals.

## Privacy & offline guarantees

- **No `INTERNET` permission** — declared nowhere, and the permission that Play-services libraries
  try to merge in is explicitly stripped (`tools:node="remove"`). The only permissions in the merged
  manifest are `CAMERA` and `WRITE_EXTERNAL_STORAGE` (the latter capped at API 28 for legacy saving).
- **No networking library** (no Retrofit/OkHttp/Ktor/Volley), and no analytics, ads, or
  crash-reporting SDKs.
- All ML runs **on-device**: the ML Kit document scanner UI/models live in the Google Play services
  process; Latin text recognition is statically bundled; subject segmentation is served by Play
  services. None require the app's INTERNET permission.
- **Identity numbers are never** extracted, stored, logged, or placed in filenames.
- Open-source attributions are bundled and shown in an **offline** licenses screen (no web fetch).

## Tech stack

Versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml):

- **Android Gradle Plugin 9.2.1**, **Gradle 9.4.1**, **Kotlin 2.4.0**, **Compose BOM 2026.05.01**
- Jetpack Compose + Material 3, Navigation Compose, Lifecycle/ViewModel, Coroutines
- ML Kit (on-device): Document Scanner, Latin Text Recognition, Subject Segmentation
- DataStore (preferences), kotlinx-serialization (task persistence), Coil (thumbnails),
  ExifInterface (JPEG DPI + orientation), the framework `PdfDocument` (no iText/PDFBox)
- `compileSdk`/`targetSdk = 37`, `minSdk = 24`

> AGP 9 has **built-in Kotlin support**, so the `kotlin-android` plugin is *not* applied; the Kotlin
> version is pinned via a `buildscript` classpath in the root `build.gradle.kts` to match the Compose
> compiler plugin.

## Requirements

- **JDK 17+** (builds and runs on JDK 25; Kotlin targets JVM 17 bytecode).
- **Android SDK** with platform **API 37** and build-tools **37.0.0** (AGP fetches them on first build).
- A `local.properties` pointing at the SDK, e.g.
  `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` (Android Studio generates it).

## Build, test & run

```bash
./gradlew assembleDebug        # build the debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # JVM unit tests (pure domain logic)
./gradlew lint                 # static analysis
./gradlew installDebug         # install onto a connected device
```

## Architecture

- `domain/` — pure Kotlin, **no Android imports**, fully JVM-testable: models, units math, layout +
  photo-grid calculators, slugify, filename builder, JPEG size targeting, card classifier, crop math,
  and the OCR name parsers.
- `data/` — Android/ML Kit behind interfaces (`Scanner`, `Ocr`, `PdfRenderer`, `JpegRenderer`,
  `PhotoProcessor`, `BackgroundSegmenter`, `FileSaver`, `Prefs`) with real implementations and the
  `Exporter` / `PhotoExporter` orchestrators.
- `ui/` — Compose screens + ViewModels (`AppViewModel` holds the document `ScanSession`,
  `PhotoViewModel` the photo edit state, `TaskViewModel` the task set; `ExportViewModel` /
  `NameViewModel` hold the Android-backed work).

### Key formulas

- mm → points: `pt = mm * 72 / 25.4`
- mm → pixels: `px = round(mm * dpi / 25.4)`
- A4 = 210 × 297 mm; CR-80 ID = 85.6 × 54 mm; passport photo = 35 × 45 mm.

### Filename template

`{nameSlug}-{cardTypeSlug}-{purpose}-{yyMMdd}-{HHmm}.{ext}`
e.g. `aminul-islam-pan-upload-260608-1430.jpeg`. Same-minute collisions append `-{ss}`, then `-2`, `-3`, …

## Manual smoke test

The automated suite covers the pure logic; a real device + Play services is needed for the rest.

- **Offline:** in airplane mode, scanning, OCR, background removal, rendering, saving and sharing all
  work end-to-end.
- **Documents:** scan front (and optional back) → configure purpose/paper/format → name → preview →
  Save lands in `Downloads/` with the templated filename; Share opens the chooser.
- **Photo:** capture/pick → crop by pinch-zoom + drag → pick size → export upload JPEG and/or a print
  grid PDF; a too-small KB cap surfaces the "smallest legible version" warning (never silent).
- **Tasks:** add several entries → export individually and as one combined PDF.
- **About → Open-source licenses** lists every bundled library with its full license text, offline.

## License

Closed-source by default. © Bayaan Consultancy. Bundled open-source components retain their own
licenses (viewable in-app under About → Open-source licenses).
