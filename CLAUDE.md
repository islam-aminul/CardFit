# CLAUDE.md — CardFit (ID Card Layout App)

## 1. What this app does
**App name: CardFit.** **Package ID (namespace + applicationId): `in.firm.consultancy.bayaan.cardfit`** — fixed and permanent; never change it after first publish.

A privacy-first Android app that scans both sides of an identity card and lays them out, both sides centered on a single page, sized either at true physical dimensions for printing or compressed under a size cap for portal upload. All processing is on-device. No cloud, no network, no analytics.

## 2. Non-negotiable constraints (HARD RULES)
- **Fully offline.** Do NOT declare the INTERNET permission in the manifest. Do NOT add any networking library (no Retrofit, OkHttp, Ktor, Volley). On-device ML Kit does not need your app's INTERNET permission.
- **No cloud, no analytics, no telemetry, no crash-reporting SDK, no ads.**
- **No copyleft dependencies.** Do NOT use iText or PDFBox. Use the Android framework `android.graphics.pdf.PdfDocument` for PDF.
- **No real government logos/holograms/emblems.** Card-type tiles use original, generic, stylized card illustrations only.
- **Identity numbers never leave the device and never appear in filenames or logs.**
- **All image/PDF/OCR work runs off the main thread** (coroutines on Dispatchers.Default/IO).
- Do not invent features beyond this spec. If tempted, stop.

## 3. Tech stack (verify latest stable versions before pinning)
- Language: Kotlin. UI: Jetpack Compose + Material 3. Architecture: MVVM, unidirectional data flow, StateFlow.
- AndroidX: activity-compose, lifecycle-viewmodel-compose, navigation-compose, datastore-preferences, exifinterface.
- Concurrency: Kotlin Coroutines + Flow.
- Capture: Google ML Kit Document Scanner (play-services-mlkit-document-scanner) — it provides its own capture/crop UI. Do NOT add CameraX.
- OCR: Google ML Kit Text Recognition, **bundled** Latin model (so no runtime model download; reinforces offline behavior).
- PDF: framework `PdfDocument`. Raster: framework `android.graphics` (Bitmap/Canvas) + `Bitmap.compress(JPEG, …)`. JPEG DPI metadata via androidx `ExifInterface`.
- Storage: MediaStore (Downloads on API 29+). Sharing: FileProvider + ACTION_SEND.
- Licenses: Google OSS Licenses Gradle plugin + an in-app "Open source licenses" screen.
- Optional: coil-compose for preview thumbnails.
- Build: `namespace` and `applicationId` both = `in.firm.consultancy.bayaan.cardfit`; compileSdk latest stable, minSdk 24, targetSdk latest stable, Gradle version catalog.

## 4. Architecture
- `domain/` — pure Kotlin: data models + pure functions (units math, layout calc, filename, slugify, size-search). NO Android imports here so it is fully unit-testable on the JVM.
- `data/` — scanning wrapper, OCR wrapper, renderers (PDF, JPEG), MediaStore saver, DataStore prefs. These wrap Android/ML Kit behind interfaces so they can be faked in tests.
- `ui/` — Compose screens + ViewModels (StateFlow state, events in).
- Single source of truth: a `ScanSession` holding the captured/cropped sides, independent of render settings, so the user can re-export to another mode without re-scanning. ViewModel retains it across config changes.

## 5. Domain model

```kotlin
enum class CardType(val widthMm: Double?, val heightMm: Double?, val slug: String, val fitMode: FitMode) {
    PAN(85.6, 54.0, "pan", FitMode.ACTUAL_SIZE),
    AADHAAR(85.6, 54.0, "aadhaar", FitMode.ACTUAL_SIZE),   // PVC card only
    EPIC(85.6, 54.0, "epic", FitMode.ACTUAL_SIZE),         // new PVC card only
    ADMIT_CARD(null, null, "admit-card", FitMode.FIT_PAGE),
    CUSTOM(null, null, "custom", FitMode.ACTUAL_SIZE),     // dimensions supplied at runtime
    FREE(null, null, "free", FitMode.FIT_WIDTH)
}
enum class FitMode { ACTUAL_SIZE, FIT_WIDTH, FIT_PAGE }
enum class PaperSize(val widthMm: Double, val heightMm: Double) {
    A4(210.0, 297.0), A5(148.0, 210.0), LETTER(215.9, 279.4), LEGAL(215.9, 355.6)
    // CUSTOM handled separately with runtime mm
}
enum class OutputMode { PRINT, UPLOAD }     // user may select one or both
enum class OutputFormat { PDF, JPEG }
data class RenderConfig(
    val mode: OutputMode,
    val paper: PaperSize,
    val format: OutputFormat,
    val dpi: Int,                 // default 300 print, 200 upload
    val grayscale: Boolean,
    val cropMarks: Boolean,       // print only
    val maxFileSizeKb: Int?       // upload only
)
data class ScannedSide(val imageUri: Uri /* corrected, cropped */)
data class ScanSession(val cardType: CardType, val front: ScannedSide?, val back: ScannedSide?,
                       val customWidthMm: Double? = null, val customHeightMm: Double? = null)
```

## 6. The math (implement EXACTLY; unit-test it)
- mm → PDF points: `pt = mm * 72.0 / 25.4`
- mm → pixels at dpi: `px = Math.round(mm * dpi / 25.4)`
- Reference: A4 = 210×297 mm = 595.28×841.89 pt; CR-80 = 85.6×54 mm = 242.65×153.07 pt = 1011×638 px @300dpi.
- Default print DPI 300; default upload DPI 200 (configurable).
- **Layout rules** (single page, both sides or one):
  - Always **center horizontally**.
  - Stack vertically with a gap. DEFAULT GAP = 8 mm.
  - PRINT (ACTUAL_SIZE): each card drawn at its exact mm size; the whole stack is **centered vertically** on the page.
  - UPLOAD / FIT_WIDTH: each card scaled to (page width − 2× margin), preserving aspect ratio; DEFAULT MARGIN = 6 mm; canvas height = content height + margins (crop to content).
  - FIT_PAGE (admit card): scale the single image to fit within the page minus margins, preserving aspect ratio.
  - One-sided sessions: center the single image using the same rules.
- **Aspect-ratio reconciliation:** after scan/crop, the card may not match the target ratio. Crop-to-fit (center-crop the minimal amount); never stretch.

## 7. File-size targeting (upload)
Pure function with an injected compressor `(quality:Int) -> Int /*bytes*/` so it is testable:

```
fun chooseQuality(maxBytes, minQuality=30, compress): Result
  // binary search quality in [minQuality..100]; return the HIGHEST quality whose size <= maxBytes
  // if compress(minQuality) > maxBytes -> signal NEEDS_DOWNSCALE
```

Caller flow: render layout bitmap once at target DPI → run chooseQuality. If NEEDS_DOWNSCALE, reduce DPI/dimensions by ~15% and retry, down to a legibility floor (do not go below ~150 dpi effective for a card). If still impossible, save the best-effort smallest legible version and surface a warning to the user — never produce silent unreadable output. For PDF-upload, embed the JPEG-compressed bitmap and include a small overhead margin when comparing to the cap.

## 8. Output generation
- **PDF (print):** `PdfDocument`; page width/height in points (round to Int); draw each card bitmap into a points-rect at exact physical size; honor crop marks (thin 0.3pt corner ticks just outside each card) when enabled.
- **PDF (upload):** same engine, FIT_WIDTH sizing, compressed embedded image.
- **JPEG:** render layout to a Bitmap (white background) at target pixel dimensions; `Bitmap.compress(JPEG, quality)`; then write DPI density to the saved file via `ExifInterface` (TAG_X_RESOLUTION, TAG_Y_RESOLUTION = dpi, TAG_RESOLUTION_UNIT = inches).
- **Grayscale:** apply a saturation-0 ColorMatrix when enabled (shrinks file).
- **Multi-select:** if both PRINT and UPLOAD chosen, generate both files from the same ScanSession.

## 9. Filename
Template: `{nameSlug}-{cardTypeSlug}-{purpose}-{yyMMdd}-{HHmm}.{ext}`
- purpose ∈ {print, upload}. ext ∈ {pdf, jpeg}.
- slugify(name): lowercase → Unicode NFD normalize → drop diacritics → replace runs of non `[a-z0-9]` with single `-` → trim leading/trailing `-`. Empty name → `document`.
- Collision in same minute → append `-{ss}`; if still colliding, `-2`, `-3`, …
- Examples: `aminul-islam-pan-upload-260608-1430.jpeg`, `aminul-islam-pan-print-260608-1430.pdf`.

## 10. OCR auto-naming
- Run ML Kit Text Recognition (bundled Latin) on the FRONT side bitmap.
- Card-type-specific name parser (heuristics, not ML): use the known CardType to locate the holder's name — e.g. PAN: the line following a "Name"/नाम label and above father's-name; Aadhaar: the name line above the DOB line; EPIC: after "Elector's Name"/"Name". Return best-guess String or null.
- The result is a **suggestion only**: pre-fill an editable name field. NEVER auto-finalize the filename from OCR without showing the editable field. NEVER extract or store ID numbers.
- If OCR returns nothing, leave the field empty for manual entry; the app stays fully usable.

## 11. UI flow (Compose, Material 3, visual tiles, progressive disclosure)
1. **Card type** — tappable tiles with original stylized card illustrations (draw with Compose Canvas or simple vector assets; no real emblems). Includes Custom (mm inputs) and Free.
2. **Scan** — launch ML Kit Document Scanner for front, then back (skippable for single-side). Show thumbnails; allow retake.
3. **Configure** — purpose tiles: Print / Upload / Both. Paper-size tiles. Format. Show max-size field ONLY when Upload is selected. Grayscale toggle. Crop-marks toggle ONLY when Print is selected.
4. **Name** — editable field pre-filled by OCR suggestion.
5. **Preview & Export** — render preview of the page(s); Save (MediaStore) and Share (FileProvider). Generates one or two files per selection. Allow going back to change purpose and re-export without re-scanning.
6. **Settings/About** — "Open source licenses" (OSS plugin screen) + a short on-device-only privacy statement.
- Permissions: declare CAMERA; request at runtime before first scan. Do NOT declare INTERNET.

## 12. Non-functional
- Background-thread all heavy work; show progress; keep UI responsive.
- Memory: an A4 page @300dpi ≈ 2480×3508 px ≈ 35 MB ARGB_8888. Downsample inputs with inSampleSize; recycle bitmaps; consider RGB_565 for the page canvas; process sequentially, never hold multiple full-page bitmaps.
- Robust error states: scan canceled, OCR empty, size cap impossible, no free storage, permission denied.
- Retain ScanSession across rotation/process death (ViewModel + SavedStateHandle for keys; persist captured image files, not just in-memory bitmaps).

## 13. Testing the agent MUST do
- JVM unit tests (no Android) for everything in `domain/`: mm→pt, mm→px, layout position/size for 1 and 2 cards in each FitMode, slugify, filename builder + collision suffixing, and chooseQuality (with a fake compressor: monotonic size-vs-quality, exact-fit, needs-downscale cases).
- The renderers/scanner/OCR are wrapped behind interfaces; provide fakes so ViewModel logic is testable.
- Every phase must end with: `./gradlew assembleDebug` succeeds, `./gradlew lint` clean of errors, `./gradlew testDebugUnitTest` green. Report all three.

## 14. Documented DEFAULTS for open assumptions (change only if instructed)
- Vertical placement in print = pair centered vertically on sheet.
- Card gap = 8 mm; upload margin = 6 mm.
- Aadhaar/EPIC = PVC card (CR-80) only; printed Aadhaar letter / old paper EPIC out of scope.
- No PNG output. Aadhaar masking is OUT of v1 (future). OCR auto-name from English text only.
- Default paper = A4; default print DPI = 300; default upload DPI = 200; readability floor ≈ 150 dpi effective; minQuality = 30.
- App is closed-source by default (no obligation to publish source).

## 15. DO NOT
- declare INTERNET, add networking, add analytics/ads, use iText/PDFBox/OpenCV, reproduce real ID emblems, put ID numbers in filenames/logs, block the main thread, hold multiple full-page bitmaps, or add features not in this file.
