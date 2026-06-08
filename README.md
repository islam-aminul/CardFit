# CardFit

A privacy-first Android app that scans both sides of an identity card and lays them out — both
sides centered on a single page — sized either at true physical dimensions for printing or
compressed under a size cap for portal upload. **Everything runs on-device. No cloud, no network,
no analytics.**

- **Package ID:** `in.firm.consultancy.bayaan.cardfit` (permanent)
- Kotlin · Jetpack Compose · Material 3 · MVVM (single-module app)

## Requirements

- **JDK 17+** (the project builds and runs on JDK 25; Kotlin targets JVM 17 bytecode).
- **Android SDK** with:
  - Platform **API 37** (auto-downloaded by AGP on first build; `android-36`/`android-37` are fetched as needed)
  - Build-tools **37.0.0**
- A `local.properties` pointing at the SDK, e.g. `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`
  (generated automatically by Android Studio).

All library/plugin versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml):
Android Gradle Plugin 9.2.1, Gradle 9.4.1, Kotlin 2.4.0, Compose BOM 2026.05.01, plus AndroidX,
Coroutines, ML Kit (document scanner + bundled Latin text recognition), and the Google OSS Licenses
plugin. `compileSdk`/`targetSdk = 37`, `minSdk = 24`.

> AGP 9 has **built-in Kotlin support**, so the `kotlin-android` plugin is *not* applied; the Kotlin
> version is pinned via a `buildscript` classpath in the root `build.gradle.kts` to match the Compose
> compiler plugin.

## Build & test

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew lint                 # static analysis (must be error-free)
./gradlew testDebugUnitTest    # JVM unit tests
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Architecture

- `domain/` — pure Kotlin, **no Android imports**, fully JVM-testable: models, units math, layout
  calculator, slugify, filename builder, JPEG size targeting, and the OCR name parsers.
- `data/` — Android/ML Kit behind interfaces (`Scanner`, `Ocr`, `PdfRenderer`, `JpegRenderer`,
  `FileSaver`, `Prefs`) with the real implementations and an `Exporter` orchestrator.
- `ui/` — Compose screens + ViewModels (`AppViewModel` holds the single-source-of-truth
  `ScanSession`; `ExportViewModel` and `NameViewModel` hold the Android-backed work).

## Privacy & offline guarantees

- **No `INTERNET` permission** — declared nowhere, and the permission that Play-services libraries
  try to merge in is explicitly stripped (`tools:node="remove"`). The only permissions in the merged
  manifest are `CAMERA` and `WRITE_EXTERNAL_STORAGE` (the latter capped at API 28 for legacy saving).
- **No networking library** (no Retrofit/OkHttp/Ktor/Volley) and no analytics/ads/crash-reporting.
- ML Kit's document scanner UI/models come from Google Play services (a separate process); the
  bundled Latin OCR model is statically linked — neither needs the app's INTERNET permission.
- Identity numbers are never extracted, stored, logged, or placed in filenames.

## Documented defaults (CLAUDE.md §14)

| Setting | Default |
|---|---|
| Print vertical placement | Card pair centered vertically on the sheet |
| Card gap | 8 mm |
| Upload margin | 6 mm |
| Aadhaar / EPIC | PVC card (CR-80, 85.6 × 54 mm) only |
| Output formats | PDF and JPEG (no PNG) |
| Default paper | A4 |
| Print DPI | 300 |
| Upload DPI | 200 |
| Readability floor | ≈ 150 dpi effective |
| Minimum JPEG quality | 30 |
| OCR auto-name | English/Latin text only; suggestion only (never auto-finalized) |
| Aadhaar masking | Out of scope for v1 |
| Source | Closed-source by default |

### Key formulas (CLAUDE.md §6)

- mm → points: `pt = mm * 72 / 25.4`
- mm → pixels: `px = round(mm * dpi / 25.4)`
- Reference: A4 = 210 × 297 mm = 595.28 × 841.89 pt; CR-80 = 242.65 × 153.07 pt = 1011 × 638 px @ 300 dpi.

### Filename template (CLAUDE.md §9)

`{nameSlug}-{cardTypeSlug}-{purpose}-{yyMMdd}-{HHmm}.{ext}`
e.g. `aminul-islam-pan-upload-260608-1430.jpeg`. Same-minute collisions append `-{ss}`, then `-2`, `-3`, …

## Manual test checklist

On-device checks (the automated suite covers the pure logic; these need a real device + Play services):

**Card type**
- [ ] All tiles render with the stylized illustrations (no real emblems).
- [ ] Custom opens the mm dialog and accepts width/height; Free needs no dimensions.

**Scan**
- [ ] First scan requests CAMERA at runtime; denying shows the permission message.
- [ ] Front scan works; thumbnail + Retake appear.
- [ ] Back is skippable; Next enables once the front exists.
- [ ] Cancelling the scanner leaves existing state intact (no error).

**Configure**
- [ ] Print / Upload / Both selection drives the generated-file summary.
- [ ] Crop-marks toggle appears only when Print is selected.
- [ ] Max-size field appears only when Upload is selected.
- [ ] Paper / format / grayscale changes reflect in the summary.

**Name (OCR)**
- [ ] For PAN/Aadhaar/EPIC, a name suggestion pre-fills the (editable) field when detected.
- [ ] No name detected → empty field, "No name detected" hint.
- [ ] Editing the field is never overwritten; nothing auto-finalizes.

**Preview & export**
- [ ] Preview image renders.
- [ ] Save writes to `Downloads/CardFit/` with the templated filename (verify in Files/Downloads).
- [ ] On API < 29, Save requests WRITE_EXTERNAL_STORAGE; denying shows the error state.
- [ ] Share opens the chooser; single file uses ACTION_SEND, two files ACTION_SEND_MULTIPLE.
- [ ] Selecting **Both** generates exactly two files (print + upload).
- [ ] A too-small upload cap surfaces the "smallest legible version" warning (never silent).
- [ ] "Change output settings" returns to Configure and re-exports the **same** scan (no re-scan).

**Print vs upload sizing**
- [ ] Print PDF: cards at true physical size; crop marks (when enabled) sit just outside each card.
- [ ] Upload: image is full-width and stays under the chosen KB cap.
- [ ] Saved JPEG carries the correct DPI density (check file properties).

**Settings/About**
- [ ] "Open source licenses" opens the OSS licenses screen with content.
- [ ] Privacy statement is shown.

**Offline**
- [ ] In airplane mode, scanning, OCR, rendering, saving, and sharing all work end-to-end.
