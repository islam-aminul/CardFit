# Bayaan · CardFit Design System

The design language of **Bayaan Consultancy** (bayaan.consultancy.firm.in) — a product studio and software consultancy — applied to both its marketing website and its flagship Android app **CardFit**, a privacy-first, fully-offline app for everyday document paperwork (scan IDs, build passport photos, export print-ready PDFs at exact physical size).

*Bayaan* (بیان) means clear, eloquent expression. The whole visual identity expresses "a clear voice": concentric **voice arcs** radiating from a focal dot, calm indigo **midnight** authority, and a single bright **teal** signal.

## Sources

- **Website source**: https://github.com/bayaanconsultancy/bayaan-consultancy (Next.js + Tailwind 4). Full page + component source preserved in `reference/bayaan-site/` (as `.txt`). Explore the repo for anything not captured here.
- **CardFit Android app**: local codebase `CardFit/` (Jetpack Compose, Material 3) and https://github.com/islam-aminul/CardFit. Screen specs live in its `CLAUDE.md` §11; the shipped v1 theme was `dynamicColor` Material defaults — this design system is the Bayaan-branded redesign target.
- Bayaan brand SVGs copied from the site repo into `assets/brand/`.

## CONTENT FUNDAMENTALS

- **Voice**: first-person plural "we" on the website ("We build software that speaks clearly."); the app speaks to "you" and about "your device". Direct, plain, confident. No jargon, no exclamation marks, no emoji anywhere.
- **Sentences are short and declarative.** Headlines are full sentences with a period: "We ship our own software first." "Have something to build?"
- **Privacy is the recurring theme**, stated as fact, not marketing: "Everything stays on your device." "We default to collecting nothing." "No sales runaround."
- **Casing**: sentence case everywhere — headings, buttons ("Start a project", "Get in touch"), app tiles ("Documents & cards"). ALL-CAPS only for eyebrows and the CONSULTANCY wordmark.
- **App copy is instructional and specific**: "Scan the front to detect the card size." "Blank = no cap." Subtitles explain the consequence, not the feature: "Both sides, true size" / "Compressed to a cap".
- **Numbers/units**: exact and technical where it matters — "85.6 × 54 mm (CR-80)", "Size cap in KB".
- Eyebrow labels are 2–4 words: "What we do", "Proof, not promises", "How we work".

## VISUAL FOUNDATIONS

- **Colors**: three families. *Midnight* (indigo #16183A→#EEF0F8) carries authority — headings, primary buttons, dark panels. *Teal* (#14B8A6) is "the signal" — the one bright accent for CTAs on dark, focus rings, selection, status dots, links (teal-600 on light). *Sage* (#6B8A74) is the calm second voice — alternate eyebrows, supporting accents. Page background is paper #F4F6FB; content sections sit on white.
- **Type**: Space Grotesk (semibold, tracking −0.025em, leading 1.1) for every heading; Inter for everything else (body leading 1.625). Scale: 60/48 hero → 30 section → 18 card title → 16/14 body → 12 uppercase labels tracked 0.2em.
- **Backgrounds**: flat paper or white. No photography, no textures, no gradients except the single radial teal glow inside the ArcField. Dark surfaces are flat midnight-900 rounded-24 panels.
- **The arc motif is the only decoration**: ArcField behind heroes (focal dot right) and dark CTA panels (dot left); ArcDivider between sections; VoiceArcsMark as icon. Arcs animate by drawing in (stroke-dashoffset).
- **Animation**: 600ms fade + 12px rise on `cubic-bezier(0.22,1,0.36,1)`, staggered ~60–150ms; 150ms color transitions on hover. No bounces, no parallax, no infinite loops. Reduced-motion is respected.
- **Hover states**: borders darken (midnight-100 → midnight-200), buttons shift one shade (midnight-800→700, teal-500→400), links underline, icon tiles tint teal. Nothing moves or scales on hover.
- **Press/selected (app)**: selected tiles get a 2px teal border + 10% teal tint; disabled = 40% opacity.
- **Borders, not shadows**: every card edge is a 1px midnight-100 border on white. No box shadows anywhere (reserve `--shadow-float` for true floating UI like dialogs).
- **Corner radii**: 8px chips · 10–12px tiles/inputs · 16px cards · 24px feature panels · pill (9999) for all buttons, tags, and accent bars. Never square.
- **Layout**: 1120px container, 24px side padding, 80px section rhythm, generous whitespace. App screens: 16px padding, 12px vertical gaps, pinned bottom action bar (primary filled + ghost Back, both full-width).
- **Transparency/blur**: the site header is white at 80% opacity with backdrop blur — the only blur in the system. Tints are done with alpha (teal/10, midnight-600/90 text).
- **Imagery**: none. Product visuals are abstract flat SVG illustrations in brand colors (see `assets/cardfit-visual.svg`, CardArtwork). Cool color temperature throughout.

## ICONOGRAPHY

- **The brand mark family is the icon system** on the website: VoiceArcsMark inside midnight-50 tiles is the only "icon" the site uses. There is no icon font on the site; unicode arrows (→, ›) serve as affordances.
- **The app uses Material Symbols** (Compose `material-icons-extended`: `document_scanner`, `photo_camera`, `folder_copy`, `info`…). On the web, load Material Symbols Rounded from Google Fonts CDN — this is a like-for-like substitution of the same Google icon set.
- **Original abstract illustrations** replace anything sensitive: CardArtwork draws generic ID-card layouts with no real government logos, emblems, or seals (a hard rule from CardFit's CLAUDE.md).
- No emoji, ever.
- Logos: `assets/brand/logo.svg` / `logo-dark.svg` (lockups), `icon.svg` / `icon-dark.svg` (mark only), `favicon.svg`. The CardFit launcher icon is a rainbow-gradient adaptive icon in the app codebase (`res/drawable/ic_launcher_*.xml`) — intentionally not reproduced here; the Bayaan redesign would replace it.

## Components

All exported under `window.BayaanCardFitDesignSystem_94a7f5`. Styling classes live in `components/components.css` (`.bv-*`).

**Brand** (`components/brand/`): `VoiceArcsMark`, `Logo`, `ArcField`, `ArcDivider`
**Core** (`components/core/`): `Button`, `Eyebrow`, `Tag`, `ValueBlock`, `Container`
**Marketing** (`components/marketing/`): `ServiceCard`, `CaseStudyCard`
**App** (`components/app/`): `ScreenScaffold`, `HomeTile`, `SelectableCard`, `IllustratedTile`, `OutputChip`, `AppSwitch`, `AppTextField`, `CardArtwork`

Intentional additions (no direct site/app counterpart): `AppSwitch`, `AppTextField` (Bayaan restylings of the Material 3 Switch/OutlinedTextField the app uses), `CardArtwork` accent remap to Bayaan palette. Site's `Header`, `Footer`, `ContactForm`, `Reveal` are recreated inside the website UI kit rather than as primitives; `ClarityProvider` (analytics) is intentionally omitted.

## Index

- `styles.css` — global entry; imports `tokens/` (colors, typography, fonts, spacing, shape, motion, base) + `components/components.css`
- `components/` — see above; each folder has `.jsx` + `.d.ts` + `.prompt.md` + a card HTML
- `guidelines/` — foundation specimen cards (colors, type, spacing, shape, brand motifs, motion)
- `assets/brand/` — Bayaan logo/mark SVGs · `assets/cardfit-visual.svg` — CardFit case-study illustration
- `ui_kits/cardfit-app/` — the Bayaan-redesigned CardFit app (interactive click-through)
- `ui_kits/bayaan-website/` — the bayaan.consultancy.firm.in homepage recreation
- `reference/bayaan-site/` — original site source (read-only reference)
- `SKILL.md` — agent skill entry point

## Caveats

- Fonts load from Google Fonts CDN (exactly what the site does via next/font); no binaries are bundled.
- CardFit v1 shipped with Material dynamic color; the app UI kit here is the **Bayaan-branded redesign**, not a recreation of the shipped Material look.
