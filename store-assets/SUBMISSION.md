# CardFit — Play Console submission reference

Everything needed to fill in the Console, with absolute paths. Generated 31 July 2026.

---

## 1. Identity

| Field | Value |
|---|---|
| Package / applicationId | `in.firm.consultancy.bayaan.cardfit` |
| versionCode | `2` |
| versionName | `1.0` |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |
| Developer name (public) | Bayaan Consultancy |
| Developer account ID | 8374502335673146520 |
| Developer email (public) | bayaan@consultancy.firm.in |
| Website | https://bayaan.consultancy.firm.in/ |
| Privacy policy URL | https://bayaan.consultancy.firm.in/cardfit/privacy |

## 2. Artifacts to upload

| What | Absolute path | Size |
|---|---|---|
| **App bundle (upload this)** | `C:\Users\aminu\Projects\CardFit\app\build\outputs\bundle\release\app-release.aab` | 25,211,663 B |
| Release APK (device testing only, do NOT upload) | `C:\Users\aminu\Projects\CardFit\app\build\outputs\apk\release\app-release.apk` | 45,941,216 B |
| R8 mapping (deobfuscates crash reports) | `C:\Users\aminu\Projects\CardFit\app\build\outputs\mapping\release\mapping.txt` | 55,292,848 B |

AGP embeds the mapping inside the AAB at
`BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`, so Console normally picks
it up automatically. Confirm under **Release → App bundle explorer → Downloads**; if the
deobfuscation file is missing, upload `mapping.txt` manually. This matters — R8 already hid a
crash-on-launch in this app once.

These are build outputs and are gitignored. To recreate:

```
cd C:\Users\aminu\Projects\CardFit
gradlew.bat clean bundleRelease assembleRelease
```

## 3. Signing

Upload key — register this fingerprint when enrolling in Play App Signing.

```
SHA-256  a4142ac36fc77c49169f8521a0e944d69f4618e9f44f31da88c1dd41bd9186d4
SHA-1    b1efbfb0d2e4a7a884860c3a988f158e41a265c8
MD5      dddfe8b139d2c5986a1c1adf95ce299a
DN       CN=CardFit, OU=Bayaan, O=Bayaan Consultancy, L=Kolkata, ST=WB, C=IN
Key      2048-bit RSA, SHA384withRSA, valid to 16 Dec 2053
```

| Item | Path |
|---|---|
| Keystore (never commit) | `C:\Users\aminu\Projects\CardFit\app\upload-keystore.jks` |
| Previous keystore, superseded | `C:\Users\aminu\Projects\CardFit\app\upload-keystore.jks.bak-20260731` |
| Credentials | `C:\Users\aminu\Projects\CardFit\keystore.properties` |

The superseded key had `O=Firm Consultancy` (an artifact of the package name) and fingerprint
`db5d02cd…c17f0`. It was never published — **ignore it entirely**.

Back up `upload-keystore.jks` and `keystore.properties` somewhere off this machine. Losing them
means an upload-key reset request to Google.

To re-print the fingerprint:

```
"%LOCALAPPDATA%\Android\Sdk\build-tools\37.0.0\apksigner.bat" verify --print-certs ^
  "C:\Users\aminu\Projects\CardFit\app\build\outputs\apk\release\app-release.apk"
```

## 4. Store listing text

**App name** (25/30)

```
CardFit — ID Card & Photo
```

**Short description** (66/80)

```
Scan, size and print ID cards and passport photos — fully offline.
```

**Full description** (2104/4000)

```
CardFit prepares identity documents and photos for printing or uploading — and it does the
whole job on your phone. There is no account, no cloud, and no internet permission. Your
documents never leave your device, because the app has no way to send them anywhere.

PRINT AT TRUE SIZE
Scan the front and back of a card and CardFit lays both sides on a single page at their exact
physical size, so a printed card measures what it should. Choose A4, A5, Letter or Legal,
add crop marks, and print or save a PDF.

MEET ANY UPLOAD LIMIT
Portals that demand "under 200 KB" are the reason this app exists. Set a size cap and CardFit
finds the best quality that fits underneath it, warning you rather than silently producing
something unreadable.

PASSPORT AND VISA PHOTOS
Crop to passport, visa or stamp sizes — or your own measurements. Rotate, adjust brightness,
contrast and saturation, auto-enhance in one tap, or remove the background. Then export a
single sized image, or a full sheet of copies arranged for printing with cut marks.

DOCUMENTS AND RECEIPTS
Scan multi-page documents that fit the sheet, or receipts and bills placed at their real
width. Edge detection and corner adjustment are built into the scanner.

APPLICATION SETS
Group everything one application needs — several documents and photos, each with its own
name — and export them together, including a combined PDF under a single shared size limit.

BUILT TO STAY ON YOUR DEVICE
• No internet permission is declared at all
• No account, no sign-in, no ads, no analytics, no tracking
• Text recognition only suggests a file name; identity numbers are never extracted, stored,
  logged, or written into a file name
• Open-source licences for every component are listed in the app

Sizes can be entered in centimetres or inches. Files are saved to your Downloads folder, and
you can open, print or share them from there whenever you want.

CardFit is an independent utility. It is not affiliated with, endorsed by, or connected to any
government department or issuing authority, and all card artwork in the app is generic and
illustrative.
```

## 5. Graphics

| Asset | Path | Spec |
|---|---|---|
| App icon | `C:\Users\aminu\Projects\CardFit\store-assets\play_icon_512x512.png` | 512×512, 32-bit PNG |
| Feature graphic | `C:\Users\aminu\Projects\CardFit\store-assets\play_feature_graphic_1024x500.png` | 1024×500, 24-bit PNG |

Phone screenshots — all `1080×2160`, JPEG RGB, ratio exactly 2.00, in
`C:\Users\aminu\Projects\CardFit\store-assets\`, uploaded in this order:

| # | File | Caption |
|---|---|---|
| 1 | `01_home.jpg` | One app for every document you need to prepare. |
| 2 | `02_layout_preview.jpg` | Both sides, one page, true physical size. |
| 3 | `03_scan_card.jpg` | Scan the front and back — corners adjust automatically. |
| 4 | `04_choose_card.jpg` | Preset sizes for common ID cards — or set your own. |
| 5 | `05_configure_output.jpg` | Print at true size, or compress under any upload limit. |
| 6 | `06_edit_photo.jpg` | Crop, enhance, and remove the background — right on your phone. |
| 7 | `07_photo_print_grid.jpg` | Passport photos, arranged and ready to print. |
| 8 | `08_application_sets.jpg` | Organize a full application's documents in one place. |

Captions are Play's optional per-screenshot text; if your listing flow doesn't offer the field,
they are simply reference notes. Source of truth: `store-assets\captions.txt`.

## 6. Declarations

**Data safety** — the whole form is negative, and it matches the privacy policy:

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all user data encrypted in transit? | N/A — no data leaves the device |
| Do you provide a way to request data deletion? | N/A — nothing is collected |

Supporting fact if challenged: the app declares no `INTERNET` permission. Verify with

```
"%LOCALAPPDATA%\Android\Sdk\build-tools\37.0.0\aapt2.exe" dump badging ^
  "C:\Users\aminu\Projects\CardFit\app\build\outputs\apk\release\app-release.apk"
```

Declared permissions, and why:

| Permission | Reason |
|---|---|
| `android.permission.CAMERA` | Capturing documents and photos, requested at runtime |
| `android.permission.WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28) | Saving exports on Android 9 and older |
| `android.permission.READ_EXTERNAL_STORAGE` (maxSdkVersion 28) | Implied by the above on legacy API levels |
| `…cardfit.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Auto-added by AndroidX, app-local, not user-facing |

No sensitive-permission declaration form is required: no All Files Access, no location, no
SMS/call log, no accessibility, no foreground service.

**Other declarations**

| Item | Answer |
|---|---|
| Ads | No ads |
| Content rating | Utility, no objectionable content — expect Everyone / PEGI 3 |
| Target audience | 18 and over (the privacy policy states the app is for adults) |
| App category | **Productivity** — the shelf document scanners sit on (CamScanner, Adobe Scan, Microsoft Lens). Tools is file managers, cleaners and keyboards |
| News app | No |
| COVID-19 contact tracing / status | No |
| Financial features | No |
| Health | No |
| Government app | **No** — CardFit is independent and unaffiliated |

## 7. Order of operations in Console

1. Create app → name, default language, app/game, free/paid.
2. **Set up → App signing:** enrol in Play App Signing, register upload cert
   `a4142ac3…86d4`.
3. **Store listing:** paste §4 text, upload §5 graphics.
4. **App content:** privacy policy URL, data safety, ads, content rating, target audience,
   news, government declarations (§6).
5. **Production → Create release:** upload the `.aab` from §2, add release notes.
6. Confirm the deobfuscation mapping is attached (§2), then roll out.

## 8. Known deviations

- **Screenshots are 1080×2160, exactly 2:1** — the maximum ratio Play permits. They are cropped
  from 1080×2340 device captures to remove the status bar and gesture pill; raw captures are
  kept in `C:\Users\aminu\Projects\CardFit\screenshots\`.
- **The listing text never names PAN, Aadhaar or Voter ID.** Preset sizes are identical for all
  three (CR-80, 85.6 × 54 mm), so naming issuing schemes adds discoverability but invites a
  reviewer to check for implied official affiliation. Add it only deliberately, and never to
  the app name.
- **The release build is verified for launch, print geometry and export correctness**, but has
  not had a broad manual pass. Print geometry was confirmed exactly: 85.60 × 54.00 mm per card,
  10.00 mm gap, centred on A4.
