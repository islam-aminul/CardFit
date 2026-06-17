# CardFit — Play Console Listing & Launch Prep

Everything below is paste-ready for the Play Console, plus the answers to the declarations and a checklist for the steps you do on your own machine.

---

## 1. App title (max 30 characters)

Pick one (all clear of the "CardioFit" fitness namespace and under the limit):
- **CardFit – ID Card & Photo**  (25)
- **CardFit: ID & Photo to PDF**  (26)

Recommended: **CardFit – ID Card & Photo**

## 2. Short description (max 80 characters)

Pick one:
- **Lay out ID cards & photos at exact size. Export print-ready PDF or sized JPEG.**  (78)
- **Scan ID cards & photos, place at true size, export PDF or size-capped JPEG.**  (76)

## 3. Full description (max 4000 characters) — paste as-is

```
CardFit puts both sides of an identity card on a single page, perfectly centered and at the card's true physical size, ready to print or to upload to an online form. It also includes an ID-photo studio and a task mode for organising a full set of documents for an application.

Everything happens on your device. CardFit works fully offline, with no ads, no accounts, and no tracking, and it never sends your documents or photos anywhere.

DOCUMENTS & CARDS
• Scan one or both sides and place them centered on a single page — at true physical size for print, or compressed under a KB cap you set for upload.
• Print mode renders the card at its exact real-world size (for example a standard card at 85.6 x 54 mm), so a printed copy matches the original.
• Automatic card-type and orientation detection (PAN, Aadhaar, Voter ID/EPIC, admit cards), with manual override and custom sizes in cm or inch.
• Rounded-corner trim for PVC cards: cleans up the off-colour corner spots left when a card is scanned on a coloured surface, by trimming to the true card corner radius and filling white.
• Optional searchable-text layer so the exported PDF's text can be selected and searched.
• OCR name suggestion fills the filename from the scan — a suggestion only, and identity numbers are never extracted.
• Export as PDF or JPEG; choose paper size and grayscale.

ID-PHOTO STUDIO
• Capture or pick a photo (held upright automatically), then frame the face with a pinch-zoom and drag crop, rotate, and press-and-hold Compare to see the original.
• Auto-enhance, fine brightness/contrast/saturation, and optional one-tap background removal to white.
• Preset sizes — Passport, Visa, Stamp — plus custom in cm or inch; the crop locks to the chosen size.
• Export a single sized upload JPEG (exact pixels, optional KB cap), or a print sheet with the number of copies you need arranged in neat rows.

TASKS
• Group several people's documents and photos into one named application set, saved on your device.
• Reorder, rename, set per-item upload limits, and export each item separately or as one combined multi-page PDF.

PRIVACY BY DESIGN
• Fully offline — the app does not request internet access.
• No data collection, no analytics, no ads.
• Your documents and photos are processed on your device and saved only where you choose.

Supported identity cards include PAN, Aadhaar (PVC card) and voter ID cards, as well as admit cards, custom sizes and free sizing.

CardFit is an independent utility. It is not affiliated with, endorsed by, or connected to the Government of India, the Election Commission of India, UIDAI, the Income Tax Department, or any other authority. All card artwork in the app is generic and illustrative.
```

## 4. Categorisation
- **App category:** Tools (Productivity is an acceptable alternative).
- **Tags / search terms to set:** document scanner, PDF maker, ID photo, passport photo, image to PDF.

---

## 5. Declarations (answers)

### Privacy policy URL
Host the privacy-policy file and enter its public URL:
`https://bayaan.consultancy.firm.in/cardfit/privacy`
(Publish the privacy policy at exactly this path, and make sure it loads publicly without login.)

### Data safety form
- Does your app collect or share any of the required user data types? → **No.**
- The app collects no data and shares no data; all processing is on-device.
- (If the form asks about specific types, mark none collected and none shared.)
- Data encrypted in transit → not applicable (no transmission).
- Users can request data deletion → data lives only on the device; uninstalling removes it.

### Ads
- Contains ads? → **No.**

### In-app purchases
- **No** (none in this version).

### Content rating (IARC questionnaire)
- Answer **No** to all questions about violence, sexual content, language, controlled substances, gambling, and user-to-user communication.
- Expected result: **Everyone / PEGI 3** (a general-audience rating).

### Target audience and content
- Target age groups: **18 and over only.** Do not include any under-18 group — this keeps you out of the Families policy program and fits a tool for handling official documents.
- Is your app directed at children? → **No.**

### Government / sensitive content
- There is no specific form field for this, but it is the main policy risk for your category. Your full description already includes the independent/unofficial disclaimer, and the in-app artwork is generic. Keep both that way: never imply official affiliation, and never use real government logos, emblems, or holograms.

---

## 6. Graphics you need to upload (produced on your side)
- **App icon:** 512 x 512 px, 32-bit PNG (your red-to-blue gradient card icon).
- **Feature graphic:** 1024 x 500 px.
- **Phone screenshots:** at least 2 (recommended 4–6) — e.g. card-type selection, the both-sides layout preview, the upload size control, the photo studio, and a task. Capture them from the app on a device or emulator.

---

## 7. Build & release checklist (on your machine)
1. Set a `versionCode` (e.g. 1) and `versionName` (e.g. "1.0") in the Gradle build.
2. Generate an **upload keystore** and enroll in **Play App Signing** (Google holds the app-signing key; a lost upload key can be reset). Back up the upload key anyway.
3. Build a **signed release AAB** (`./gradlew bundleRelease`) with R8/minify on.
4. Install and test the **release** build on a physical device — confirm scanning, export at exact size (ruler check), upload size caps, photo studio, and task mode all work, and that airplane-mode (offline) works end to end.
5. Upload the AAB to **Internal testing** first; review the auto-generated **pre-launch report** for crashes.
6. When the D-U-N-S number arrives → create the **Organization** account (name: Bayaan Consultancy; address ending 743424), complete verification, then promote the release to **Production** with a **staged rollout** (start ~20%).

---

## 8. Notes
- Title naming avoids the impersonation policy; the body names supported cards (which helps users find the app) while clearly stating you are independent — this combination is standard and acceptable.
- An organization account is exempt from the 12-tester / 14-day closed-testing gate, so once verified you can go straight to production.
- I am not a lawyer; given the app collects no data, the privacy and data-safety position is low-risk, but a quick professional review is reasonable if you want extra certainty before publishing.
