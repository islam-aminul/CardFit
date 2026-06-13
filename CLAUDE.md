# CLAUDE.md — CardFit (ID Card & Photo Layout App)

> **Status note.** v1 shipped the ID-card scan-and-layout flow (sections 1–15). The app has since
> grown four documented phases that this file now covers as part of the spec:
> **Phase 11** searchable-PDF text layer, **Phase 12** aspect-ratio detection & sizing override,
> **Phase 13** the ID-photo flow, **Phase 14** task mode (multi-document sets). The hard rules in
> section 2 are unchanged and absolute. "Do not invent features beyond this file" still applies — this
> file, including the phase sections (16–19), is the spec.

## 1. What this app does
**App name: CardFit.** **Package ID (namespace + applicationId): `in.firm.consultancy.bayaan.cardfit`** — fixed and permanent; never change it after first publish.

A privacy-first Android app with three on-device tools:
1. **Document/ID layout** — scans both sides of an identity card and lays them out, both sides centered on a single page, sized either at true physical dimensions for printing or compressed under a size cap for portal upload.
2. **ID-photo studio** (Phase 13) — takes/picks a photo, edits it non-destructively (rotate, crop to a size aspect, brightness/contrast/saturation, deterministic auto-enhance, opt-in background removal), then exports a single sized JPEG (upload) or a single-page print sheet of repeated copies (print).
3. **Task mode** (Phase 14) — groups several documents and/or photos under a named set (e.g. an application), each with its own person name, and exports them together (including an optional combined multi-page PDF under one shared size cap).

All processing is on-device. No cloud, no network, no analytics.

## 2. Non-negotiable constraints (HARD RULES)
- **Fully offline.** Do NOT declare the INTERNET permission in the manifest. Do NOT add any networking library (no Retrofit, OkHttp, Ktor, Volley). On-device ML Kit does not need your app's INTERNET permission. (The optional Subject Segmentation model is delivered by Play services itself — still no INTERNET permission in this app.)
- **No cloud, no analytics, no telemetry, no crash-reporting SDK, no ads.**
- **No copyleft dependencies.** Do NOT use iText or PDFBox. Use the Android framework `android.graphics.pdf.PdfDocument` for PDF. (kotlinx-serialization-json, used for on-device task JSON, is Apache-2.0 and allowed.)
- **No real government logos/holograms/emblems.** Card-type tiles and all export-option illustrations use original, generic, stylized drawings only.
- **Identity numbers never leave the device and never appear in filenames or logs.** Task JSON stores only person names and image filenames — never ID numbers, never absolute device paths.
- **All image/PDF/OCR work runs off the main thread** (coroutines on Dispatchers.Default/IO).
- Do not invent features beyond this file. If tempted, stop.

## 3. Tech stack (versions pinned in the Gradle version catalog)
- Language: Kotlin (2.4.0). UI: Jetpack Compose + Material 3 (Compose BOM 2026.05.01). Architecture: MVVM, unidirectional data flow, StateFlow.
- Build: AGP 9.2.1, Gradle 9.4.1. **AGP 9.0+ compiles Kotlin via built-in support — do NOT add the `org.jetbrains.kotlin.android` plugin.** `namespace` and `applicationId` both = `in.firm.consultancy.bayaan.cardfit`; compileSdk 37, buildToolsVersion 37.0.0, minSdk 24, targetSdk 37; Java/JVM target 17; Gradle version catalog (`gradle/libs.versions.toml`).
- AndroidX: activity-compose, lifecycle-runtime/viewmodel-compose, navigation-compose, datastore-preferences, exifinterface, core-ktx.
- Concurrency: Kotlin Coroutines + Flow (coroutines 1.11.0; coroutines-test for unit tests).
- Serialization: kotlinx-serialization-json (1.11.0) — task metadata only, on-device JSON.
- Icons: `material-icons-extended` (used for control glyphs, e.g. the copies stepper).
- **Capture (documents):** Google ML Kit Document Scanner (`play-services-mlkit-document-scanner`) — its own capture/crop UI. **Do NOT add CameraX.**
- **Capture (photos, Phase 13):** Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) for the gallery and `ActivityResultContracts.TakePicture` writing to a private FileProvider URI for the camera. Still no CameraX.
- OCR: Google ML Kit Text Recognition, **bundled** Latin model (no runtime model download; reinforces offline). Used both for auto-naming (section 10) and the searchable-PDF text layer (Phase 11).
- Segmentation: ML Kit **Subject Segmentation** (`play-services-mlkit-subject-segmentation`, beta) for opt-in photo background removal. The model is requested from Play services at install time; inference is fully on-device.
- PDF: framework `PdfDocument`. Raster: framework `android.graphics` (Bitmap/Canvas) + `Bitmap.compress(JPEG, …)`. JPEG DPI metadata via androidx `ExifInterface`.
- Storage: MediaStore (Downloads on API 29+; `WRITE_EXTERNAL_STORAGE` capped at `maxSdkVersion=28` for the legacy path). Sharing: FileProvider + ACTION_SEND / ACTION_SEND_MULTIPLE.
- Licenses: the Google OSS Licenses Gradle plugin alias is declared in the catalog, but is **not applied** in the app module. Instead the plugin's *release* output is committed as raw resources (`res/raw/oss_licenses`, `oss_license_metadata`) and a custom loader (`data/OssLicenses.kt`) + Compose `LicensesScreen` shows the same attributions in both debug and release. (Deviation from the original "apply the plugin" plan; kept because the plugin only populates data for release builds.)
- Preview thumbnails: coil-compose (coil3).

## 4. Architecture
- `domain/` — pure Kotlin, NO Android imports, fully JVM-unit-testable. Models + pure functions:
  - `model/Models.kt` (CardType, PaperSize, OutputMode/Format, RenderConfig, ScannedSide, ScanSession), `Layout.kt`/`LayoutPlanner.kt` (layout calc), `Units.kt`, `Filename.kt`/`Slug.kt`, `Quality.kt`/`SizeTargeting.kt`/`CombinedSizeTargeting.kt`, `CardClassifier.kt` (Phase 12), `TextLayer.kt` (Phase 11), `NameParser.kt`, `Defaults.kt`, `DimensionUnit.kt`.
  - Photo (Phase 13): `PhotoSize.kt` (PhotoSize/PhotoPaper), `PhotoSession.kt` (PhotoEditParams/ResolvedPhotoSize), `PhotoCrop.kt`, `PhotoGrid.kt`, `AutoEnhance.kt`.
  - Task (Phase 14): `task/Task.kt` (Task/DocumentEntry/EntryKind/TaskJson).
- `data/` — Android/ML Kit wrapped behind interfaces (`Interfaces.kt`) so they can be faked in tests: `scanner/` (ML Kit document scanner), `ocr/` (ML Kit text recognition), `render/` (AndroidPdfRenderer, AndroidJpegRenderer, RenderSupport), `export/` (Exporter), `photo/` (PhotoProcessor, PhotoExporter, PhotoRenderers, BackgroundSegmenter + ML Kit impl), `task/` (AndroidTaskStore, TaskExporter, CombinedPdfRenderer), `AndroidFileSaver.kt` (MediaStore), `AndroidPrefs.kt` (DataStore), `OssLicenses.kt`.
- `ui/` — Compose screens + ViewModels (StateFlow state, events in). ViewModels: `AppViewModel` (document flow), `ExportViewModel` (document preview/export), `NameViewModel` (OCR naming), `PhotoViewModel` (photo flow), `TaskViewModel` (task mode), `SettingsViewModel` (DataStore prefs). Reusable components in `ui/components/` (section 11). Navigation in `ui/navigation/CardFitNavGraph.kt`.
- Single source of truth per flow: a `ScanSession` (documents) / `PhotoState` (photos) holds the captured/edited inputs independent of render settings, so the user can re-export to another mode without re-scanning/re-editing. ViewModels retain it across config changes.

## 5. Domain model

```kotlin
enum class CardType(val widthMm: Double?, val heightMm: Double?, val slug: String, val fitMode: FitMode) {
    PAN(85.6, 54.0, "pan", FitMode.ACTUAL_SIZE),
    AADHAAR(85.6, 54.0, "aadhaar", FitMode.ACTUAL_SIZE),   // PVC card only
    EPIC(85.6, 54.0, "epic", FitMode.ACTUAL_SIZE),         // new PVC card only
    ADMIT_CARD(null, null, "admit-card", FitMode.FIT_PAGE),
    CUSTOM(null, null, "custom", FitMode.ACTUAL_SIZE),     // dimensions supplied at runtime
    FREE(null, null, "free", FitMode.FIT_WIDTH)
}                                                          // @Serializable (task persistence)
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
    val maxFileSizeKb: Int?,      // upload only
    val roundCorners: Boolean = false,                   // trim PVC-card corners to white (ID-1 3.18 mm)
    val searchableText: Boolean = false,                 // Phase 11: PDF-only invisible OCR layer
    val sizeOverride: SizeOverride = SizeOverride.AUTOMATIC,  // Phase 12
)
// imageUri is the URI *string* (domain has no Android imports; data/ maps to android.net.Uri).
// widthPx/heightPx are the cropped image's pixels, used for Phase 12 aspect classification (0 = unknown).
data class ScannedSide(val imageUri: String, val widthPx: Int = 0, val heightPx: Int = 0)
data class ScanSession(val cardType: CardType, val front: ScannedSide?, val back: ScannedSide?,
                       val customWidthMm: Double? = null, val customHeightMm: Double? = null)
```

`RenderConfig` is the **resolved, per-file** config. The UI keeps multi-selects (`Set<OutputMode>`,
`Set<PaperSize>` capped at 2, `Set<OutputFormat>`) in `AppViewModel`; `renderConfigs()` produces the
**cartesian product** with spec defaults applied (print = 300 dpi, upload = 200 dpi; crop marks only
for print; max size only for upload; searchable text only for PDF). Photo and task models: sections
16–17. See also `CardClassifier` (Phase 12) and `DimensionUnit` (cm/inch entry → mm at the UI boundary).

## 6. The math (implement EXACTLY; unit-test it)
- mm → PDF points: `pt = mm * 72.0 / 25.4`
- mm → pixels at dpi: `px = Math.round(mm * dpi / 25.4)`
- Reference: A4 = 210×297 mm = 595.28×841.89 pt; CR-80 = 85.6×54 mm = 242.65×153.07 pt = 1011×638 px @300dpi.
- Default print DPI 300; default upload DPI 200 (configurable).
- **Layout rules** (single page, both sides or one):
  - Always **center horizontally**.
  - Stack vertically with a gap. DEFAULT GAP = 8 mm.
  - PRINT (ACTUAL_SIZE): each card drawn at its exact mm size; the whole stack is **centered vertically** on the page. Per-side sizes are allowed (front/back can differ by orientation, e.g. one captured portrait, one landscape).
  - UPLOAD / FIT_WIDTH: each card scaled to (page width − 2× margin), preserving aspect ratio; DEFAULT MARGIN = 6 mm; canvas height = content height + margins (crop to content).
  - FIT_PAGE: **fit-to-area** — scale the entire stack (cards + gaps) by `min(usableW/stackW, usableH/stackH)` (never upscale past full width), then center on the full page. Used by admit cards and by non-standard cards in print, so portrait cards don't overflow.
  - One-sided sessions: center the single image using the same rules.
- **Aspect-ratio reconciliation:** after scan/crop, the card may not match the target ratio. Crop-to-fit (center-crop the minimal amount); never stretch.
- **CR-80 detection (Phase 12, `CardClassifier`):** classify the front side by pixel ratio (`longer/shorter`). CR-80 band = **[1.50, 1.75]** (true CR-80 = 85.6/54 = 1.585); outside is `NON_STANDARD`. "Near CR-80" window = **[1.45, 1.85]** (offered as a "Force CR-80" correction). `SizeOverride` ∈ {AUTOMATIC, FORCE_CR80, CUSTOM} resolves to `SizingMode` ∈ {CR80, NON_STANDARD, CUSTOM}: CUSTOM cards default to CUSTOM; everything else to AUTOMATIC (detection).

## 7. File-size targeting (upload)
Pure function with an injected compressor `(quality:Int) -> Int /*bytes*/` so it is testable:

```
fun chooseQuality(maxBytes, minQuality=30, compress): Result
  // binary search quality in [minQuality..100]; return the HIGHEST quality whose size <= maxBytes
  // if compress(minQuality) > maxBytes -> signal NEEDS_DOWNSCALE
```

Caller flow: render layout bitmap once at target DPI → run chooseQuality. If NEEDS_DOWNSCALE, reduce DPI/dimensions by ~15% and retry, down to a legibility floor (do not go below ~150 dpi effective for a card). If still impossible, save the best-effort smallest legible version and surface a warning to the user — never produce silent unreadable output. For PDF-upload, embed the JPEG-compressed bitmap and include a small overhead margin when comparing to the cap.

**Combined PDF (Phase 14, `CombinedSizeTargeting.targetCombinedSize`):** a combined PDF embeds one image per page, so a single shared JPEG quality is chosen for ALL pages at once (the compressor returns the SUM of every page's compressed size). Same binary-search-then-step-DPI-down-to-floor loop and best-effort warning as single documents; reserve `overheadBytes` for PDF structure below the cap.

## 8. Output generation
- **PDF (print):** `PdfDocument`; page width/height in points (round to Int); draw each card bitmap into a points-rect at exact physical size; honor crop marks (thin 0.3pt corner ticks just outside each card) when enabled.
- **PDF (upload):** same engine, FIT_WIDTH sizing, compressed embedded image.
- **Searchable PDF text layer (Phase 11):** when enabled (PDF only), overlay the OCR-recognized tokens as an invisible text layer aligned to the image (`TextLayer` maps OCR pixel boxes → page points). Makes the PDF text-selectable/searchable without showing extra glyphs. Default OFF — privacy by default (persisted via DataStore; section 14).
- **Rounded corners (PVC cards):** when enabled, trim each card's four corners to the ISO/IEC 7810 ID-1 radius (3.18 mm) and fill with the page white — removes the off-colour corner spots the rectangular scan crop leaves when a rounded card is scanned on a coloured surface. Shared anti-aliased helper (`RenderSupport.drawRoundedCardCorners`) applied across print PDF (points), upload JPEG, upload PDF, and the in-app preview (px). Default ON for PAN/Aadhaar/EPIC (`CardType.roundedByDefault`); off for others; not offered for FIT_WIDTH/FIT_PAGE (scaled) types.
- **JPEG:** render layout to a Bitmap (white background) at target pixel dimensions; `Bitmap.compress(JPEG, quality)`; then write DPI density to the saved file via `ExifInterface` (TAG_X_RESOLUTION, TAG_Y_RESOLUTION = dpi, TAG_RESOLUTION_UNIT = inches).
- **Grayscale:** apply a saturation-0 ColorMatrix when enabled (shrinks file).
- **Multi-select:** generate one file per `RenderConfig` from `renderConfigs()` (the modes × papers × formats product) — all from the same ScanSession.
- **Photo outputs (Phase 13):** single sized JPEG (upload) or single-page PDF grid of repeated copies (print) — section 16.
- **Task outputs (Phase 14):** per-entry files plus an optional combined multi-page PDF — section 17.

## 9. Filename
Template: `{nameSlug}-{docSlug}-{purpose}-{yyMMdd}-{HHmm}.{ext}`
- `docSlug` = card-type slug for documents, `photo` for photo entries. purpose ∈ {print, upload}. ext ∈ {pdf, jpeg}.
- slugify(name): lowercase → Unicode NFD normalize → drop diacritics → replace runs of non `[a-z0-9]` with single `-` → trim leading/trailing `-`. Empty name → `document`.
- Collision in same minute → append `-{ss}`; if still colliding, `-2`, `-3`, …
- Examples: `aminul-islam-pan-upload-260608-1430.jpeg`, `aminul-islam-pan-print-260608-1430.pdf`, `riya-photo-print-260608-1430.pdf`.

## 10. OCR auto-naming
- Run ML Kit Text Recognition (bundled Latin) on the FRONT side bitmap.
- Card-type-specific name parser (heuristics, not ML): use the known CardType to locate the holder's name — e.g. PAN: the line following a "Name"/नाम label and above father's-name; Aadhaar: the name line above the DOB line; EPIC: after "Elector's Name"/"Name". Return best-guess String or null.
- The result is a **suggestion only**: pre-fill an editable name field. NEVER auto-finalize the filename from OCR without showing the editable field. NEVER extract or store ID numbers.
- If OCR returns nothing, leave the field empty for manual entry; the app stays fully usable.

## 11. UI flow (Compose, Material 3, visual tiles, progressive disclosure)
**Home** is the entry point, offering the three flows (document scan, photo, task) plus Settings.

**A. Document/ID flow** (`AppViewModel` + `ExportViewModel`):
1. **Card type** — tappable tiles with original stylized card illustrations (`CardArtwork`, Compose Canvas; no real emblems). Includes Custom (mm/cm/inch inputs via `CustomSizeDialog`) and Free.
2. **Scan** — launch ML Kit Document Scanner for front, then back (skippable for single-side). Show thumbnails; allow retake.
3. **Configure** — purpose, paper (up to 2), and format are multi-select **illustrated tiles** (`IllustratedTile` + `ExportArtwork`, showing each option's outcome). Max-size field ONLY when Upload selected. Grayscale toggle. Crop-marks toggle ONLY when Print selected. Searchable-PDF toggle ONLY when PDF selected. Trim-rounded-corners toggle ONLY for actual-size card types (default ON for PVC cards; turn off for square-corner paper cards). Card-size detection/override section (Automatic / Force CR-80 / Custom). A chip per output file is shown (e.g. `Print · A4 · PDF`).
4. **Name** — editable field pre-filled by OCR suggestion.
5. **Preview & Export** — render preview of the page(s); Save (MediaStore) and Share (FileProvider). Generates one file per selected combination. Go back to change settings and re-export without re-scanning.

**B. Photo flow** (`PhotoViewModel`) — section 16: Source → Edit → Export.

**C. Task flow** (`TaskViewModel`) — section 17: Task list → Task detail → add documents/photos (reusing flows A/B) → export.

**Settings/About** — custom "Open source licenses" screen (section 3) + a short on-device-only privacy statement; persists the searchable-PDF default.

Reusable components (`ui/components/`): `ScreenScaffold`/`ScaffoldBottomBar`, `SelectableCard` (text-only multi-select), `IllustratedTile` + `ExportArtwork` (illustrated outcome tiles), `CardArtwork`, `CustomSizeDialog` (cm/inch), `PhotoCropFrame`.

Permissions: declare CAMERA; request at runtime before first camera use. `WRITE_EXTERNAL_STORAGE` only on API ≤ 28. Do NOT declare INTERNET.

## 12. Non-functional
- Background-thread all heavy work; show progress; keep UI responsive.
- Memory: an A4 page @300dpi ≈ 2480×3508 px ≈ 35 MB ARGB_8888. Downsample inputs with inSampleSize; recycle bitmaps; consider RGB_565 for the page canvas; process sequentially, never hold multiple full-page bitmaps.
- Robust error states: scan/capture canceled, OCR empty, size cap impossible (best-effort + warning), photo too large to fit the print sheet, no free storage, permission denied, segmentation-model not yet downloaded.
- Retain session state across rotation/process death (ViewModel; persist captured/edited image files, not just in-memory bitmaps). Task data persists as on-device JSON (`AndroidTaskStore`).

## 13. Testing the agent MUST do
- JVM unit tests (no Android) for everything in `domain/`. Existing suites (keep green and extend): `UnitsTest`, `LayoutTest`, `LayoutPlannerTest`, `SlugTest`, `FilenameTest`, `ModelsTest`, `QualityTest`, `SizeTargetingTest`, `CombinedSizeTargetingTest`, `NameParserTest`, `TextLayerTest`, `CardClassifierTest`, `DimensionUnitTest`, `PhotoSizeTest`, `PhotoCropTest`, `PhotoGridTest`, `AutoEnhanceTest`, `PhotoFilenameTest`, `TaskFilenameTest`, `task/TaskJsonTest`.
- Cover: mm→pt/px; layout position/size for 1 and 2 cards in each FitMode; slugify; filename builder + collision suffixing; `chooseQuality`/`targetCombinedSize` with a fake (monotonic) compressor (exact-fit, needs-downscale, below-floor); classifier bands; photo grid counts/positions and copy reconciliation; auto-levels; task JSON round-trip.
- Renderers/scanner/OCR/segmenter are wrapped behind interfaces; provide fakes so ViewModel/exporter logic is testable.
- Every phase must end with: `./gradlew assembleDebug` succeeds, `./gradlew lint` clean of errors, `./gradlew testDebugUnitTest` green. Report all three. (These require the Android SDK; if the environment has none, say so explicitly rather than claiming a pass.)

## 14. Documented DEFAULTS for open assumptions (change only if instructed)
- Vertical placement in print = pair centered vertically on sheet. Card gap = 8 mm; upload margin = 6 mm.
- Aadhaar/EPIC = PVC card (CR-80) only; printed Aadhaar letter / old paper EPIC out of scope.
- No PNG output. Aadhaar masking is OUT of v1 (future). OCR auto-name from English text only.
- Default paper = A4; default print DPI = 300; default upload DPI = 200; readability floor ≈ 150 dpi effective; minQuality = 30.
- CR-80 detection band = [1.50, 1.75]; near-CR-80 = [1.45, 1.85]. Custom size limits = 20–300 mm.
- Searchable-PDF text layer default = OFF — privacy by default (persisted).
- Rounded-corner trim default = ON for PVC cards (PAN/Aadhaar/EPIC), OFF otherwise; ID-1 corner radius = 3.18 mm.
- **Photo flow defaults:** default size = Passport (India) 35×45 mm (presets: Passport 35×45, Visa 51×51, Stamp 20×25, Custom); default mode = Upload; **photo upload DPI = 300** (note: higher than document upload's 200, intentional for photo sharpness); default print paper = A4 (photo papers: A4, A5, 4×6 in postcard, Letter); default copies = 4; cut marks default ON; photo print grid margin = 6 mm, gap = 3 mm; background removal & auto-enhance default OFF (opt-in). Copies round UP to fill the last row, then cap at one page.
- App is closed-source by default (no obligation to publish source).

## 15. DO NOT
- declare INTERNET, add networking, add analytics/ads, use iText/PDFBox/OpenCV, add CameraX, add the `kotlin.android` plugin, reproduce real ID emblems, put ID numbers (or absolute device paths) in filenames/logs/task JSON, block the main thread, hold multiple full-page bitmaps, or add features not in this file.

## 16. Photo flow (Phase 13)
Models: `PhotoSize` (PASSPORT_INDIA, VISA, STAMP, CUSTOM), `PhotoPaper` (A4, A5, POSTCARD_4X6, LETTER), `PhotoEditParams` (rotation 0/90/180/270, crop, brightness/contrast/saturation −100..100, autoEnhance, removeBackground), `ResolvedPhotoSize`, `CropRect`/`aspectCrop`, `PhotoGrid`/`gridLayout`/`resolveCopies`, `LevelsAdjustment`/`autoLevels`. State lives in `PhotoViewModel.PhotoState`; the original image URI is never overwritten — every preview/export re-derives from it, so edits are revertible.

Screens:
1. **Source** (`PhotoSourceScreen`) — capture (camera → private FileProvider URI, runtime CAMERA permission) or pick (Photo Picker).
2. **Edit** (`PhotoEditScreen`) — rotate, aspect-locked crop (`PhotoCropFrame`), brightness/contrast/saturation, auto-enhance, opt-in background removal (replace segmented background with white). Compare-with-original supported.
3. **Export** (`PhotoExportScreen`) — Upload and/or Print (illustrated tiles), name (optional), per-mode options: upload max-KB cap; print paper, **copies stepper (− / value / +, stepping by one full row = `perRow` photos, clamped to one row…one page)**, cut marks. Save/Share.

Outputs: **Upload** = one exact-pixel JPEG at the resolved size/DPI (size-targeted like section 7). **Print** = one single-page PDF grid (`PhotoGrid`): photos fit when `n*photo + (n-1)*gap ≤ usable` per axis; requested copies round up to fill the last row, then cap at `perPage`; a notice explains any adjustment or a "doesn't fit on this paper" error.

## 17. Task mode (Phase 14)
A `Task` (`id`, `name`, `createdAt`, `documents`, `defaultMaxFileSizeKb?`) groups `DocumentEntry` items (`EntryKind.DOCUMENT` or `PHOTO`), each with its own `personName`, type/size info, and bare image filenames relative to the task directory. Persisted as on-device JSON via `TaskJson` + `AndroidTaskStore` — **never** absolute paths or ID numbers.

Screens: **Task list** (`TaskListScreen`) and **Task detail** (`TaskDetailScreen`); adding a document or photo reuses flows A/B then returns to the detail. Export (`TaskExporter`): per-entry files for the chosen mode(s), plus an optional **combined multi-page PDF** (`CombinedPdfRenderer`) sized under one shared cap via `targetCombinedSize` (section 7). Image bytes live as app-private files; entries reference them by filename only.

## 18. Working agreements
- Keep `domain/` free of Android imports (model URIs are `String`; map at the `data/` boundary).
- New export options should follow the visual-tile pattern (`IllustratedTile` + a generic `ExportArtwork` illustration), not real previews and not real emblems.
- Add/extend JVM unit tests for any new pure logic, and run the three Gradle commands (section 13).
