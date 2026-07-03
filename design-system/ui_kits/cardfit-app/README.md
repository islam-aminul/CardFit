# CardFit app UI kit — Bayaan redesign

Interactive click-through of the CardFit document flow, restyled from Material dynamic-color to the Bayaan design language. Screens recreated from `CardFit/app/src/main/java/.../ui/screens/*.kt`:

- **Home** (HomeScreen.kt) — three flow tiles + About anchor
- **Choose card type** (CardTypeScreen.kt) — 2-col grid of CardArtwork tiles
- **Scan card** (ScanScreen.kt) — front (required) / back (optional) slots with retake
- **Configure output** (ConfigureScreen.kt) — multi-select Purpose/Paper/Format tiles, conditional toggles, file chips, card-size override
- **Name on file** (NameScreen.kt) — OCR-suggested filename field
- **Preview & export** (PreviewScreen.kt) — page preview, Save/Share, finish actions
- **Tasks** (TaskListScreen.kt) — task list + create dialog

Photo flow and Settings are stubbed (see the codebase for their specs). All primitives come from `components/app/` via the compiled bundle.
