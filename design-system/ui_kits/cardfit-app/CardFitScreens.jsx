// CardFit app screens — Bayaan-restyled recreations of the Compose screens
// (ui/screens/*.kt). Loaded via Babel; components are shared on window.
const DS = window.BayaanCardFitDesignSystem_94a7f5

function MatIcon({ name, style }) {
  return <span className="material-symbols-rounded" aria-hidden="true" style={style}>{name}</span>
}

function SectionLabel({ children }) {
  return <div style={{ fontFamily: 'var(--font-display)', fontSize: 14, fontWeight: 600, color: 'var(--text-heading)', marginTop: 4 }}>{children}</div>
}

function HelpText({ children }) {
  return <p style={{ margin: 0, fontSize: 12, lineHeight: 1.5, color: 'var(--text-muted)' }}>{children}</p>
}

// --- Custom card size dialog (CustomSizeDialog.kt) ---
// All math in mm; UI converts at the boundary (1 cm = 10 mm, 1 inch = 25.4 mm).
// Allowed 20–300 mm per side.
const UNIT_MM = { cm: 10, inch: 25.4 }
const CUSTOM_MIN_MM = 20
const CUSTOM_MAX_MM = 300
function fmtNum(v) { const r = Math.round(v * 100) / 100; return r % 1 === 0 ? String(Math.round(r)) : String(r) }

function CustomSizeDialog({ initialWmm = 85.6, initialHmm = 54, onCancel, onConfirm }) {
  const { SelectableCard, AppTextField } = DS
  const [unit, setUnit] = React.useState('cm')
  const [w, setW] = React.useState(fmtNum(initialWmm / UNIT_MM.cm))
  const [h, setH] = React.useState(fmtNum(initialHmm / UNIT_MM.cm))
  const toMm = (t) => { const v = parseFloat(t); return isNaN(v) ? null : v * UNIT_MM[unit] }
  const inBounds = (mm) => mm != null && mm >= CUSTOM_MIN_MM && mm <= CUSTOM_MAX_MM
  const wMm = toMm(w), hMm = toMm(h)
  const valid = inBounds(wMm) && inBounds(hMm)
  const switchUnit = (u) => {
    if (u === unit) return
    const cw = parseFloat(w), ch = parseFloat(h)
    if (!isNaN(cw)) setW(fmtNum(cw * UNIT_MM[unit] / UNIT_MM[u]))
    if (!isNaN(ch)) setH(fmtNum(ch * UNIT_MM[unit] / UNIT_MM[u]))
    setUnit(u)
  }
  return (
    <div style={{ position: 'absolute', inset: 0, background: 'rgba(22,24,58,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, zIndex: 5 }}>
      <div style={{ background: '#fff', borderRadius: 16, padding: 20, width: '100%', boxSizing: 'border-box', boxShadow: 'var(--shadow-float)', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 600, color: 'var(--text-heading)' }}>Custom card size</div>
        <p style={{ margin: 0, fontSize: 13, color: 'var(--color-ink)' }}>Enter the physical size of your card.</p>
        <div style={{ display: 'flex', gap: 8 }}>
          {['cm', 'inch'].map((u) => <SelectableCard key={u} label={u} selected={unit === u} onClick={() => switchUnit(u)} />)}
        </div>
        <AppTextField label={`Width (${unit})`} value={w} onChange={setW} />
        <AppTextField label={`Height (${unit})`} value={h} onChange={setH} />
        <p style={{ margin: 0, fontSize: 12, color: (w && !inBounds(wMm)) || (h && !inBounds(hMm)) ? '#B3261E' : 'var(--text-muted)' }}>
          Allowed: {fmtNum(CUSTOM_MIN_MM / UNIT_MM[unit])}–{fmtNum(CUSTOM_MAX_MM / UNIT_MM[unit])} {unit}.
        </p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="bv-btn bv-btn--ghost" onClick={onCancel}>Cancel</button>
          <button className="bv-btn bv-btn--primary" style={valid ? undefined : { opacity: 0.4, cursor: 'default' }} onClick={() => valid && onConfirm(wMm, hMm)}>Use size</button>
        </div>
      </div>
    </div>
  )
}

// --- Home (HomeScreen.kt) ---
function CfHome({ go }) {
  const { ScreenScaffold, HomeTile } = DS
  return (
    <ScreenScaffold title="CardFit" height={FRAME_H}>
      <HelpText>Everything stays on your device.</HelpText>
      <HomeTile title="Documents & cards" subtitle="Scan any document or ID — both sides laid out on one page."
        icon={<MatIcon name="document_scanner" />} iconBg="var(--accent-soft)" iconColor="var(--color-teal-700)"
        onClick={() => go('cardType')} />
      <HomeTile title="Photo" subtitle="Crop, enhance, and size a passport / visa / stamp photo."
        icon={<MatIcon name="photo_camera" />} iconBg="rgba(107,138,116,0.15)" iconColor="var(--color-sage-700)"
        onClick={() => go('photo')} />
      <HomeTile title="Tasks" subtitle="Collect several people's documents into one application set."
        icon={<MatIcon name="folder_copy" />} iconBg="var(--color-midnight-50)" iconColor="var(--color-midnight-600)"
        onClick={() => go('tasks')} />
      <div style={{ marginTop: 'auto' }}>
        <HomeTile title="About" subtitle="Privacy, version, and open-source licenses."
          icon={<MatIcon name="info" />} iconBg="var(--color-midnight-50)" iconColor="var(--color-midnight-600)"
          onClick={() => go('about')} />
      </div>
    </ScreenScaffold>
  )
}

// --- Step 1: card type (CardTypeScreen.kt) ---
const CARD_TYPES = [
  { key: 'pan', label: 'PAN', sub: '85.6 × 54 mm' },
  { key: 'aadhaar', label: 'Aadhaar', sub: '85.6 × 54 mm' },
  { key: 'epic', label: 'Voter ID (EPIC)', sub: '85.6 × 54 mm' },
  { key: 'admit', label: 'Admit card', sub: 'Fit to page' },
  { key: 'custom', label: 'Custom', sub: 'Your dimensions' },
  { key: 'free', label: 'Free', sub: 'Fit to width' },
]

function CfCardType({ go, setApp }) {
  const { ScreenScaffold, CardArtwork } = DS
  const [customOpen, setCustomOpen] = React.useState(false)
  const pick = (t) => {
    if (t.key === 'custom') { setCustomOpen(true); return }
    setApp((a) => ({ ...a, cardType: t.key })); go('scan')
  }
  return (
    <ScreenScaffold title="Choose card type" height={FRAME_H} onBack={() => go('home')}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {CARD_TYPES.map((t) => (
          <button key={t.key} className="bv-home-tile" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 8, padding: 12 }}
            onClick={() => pick(t)}>
            <CardArtwork type={t.key} />
            <span style={{ textAlign: 'center' }}>
              <span style={{ display: 'block', fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 600, color: 'var(--text-heading)' }}>{t.label}</span>
              <span style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)' }}>{t.sub}</span>
            </span>
          </button>
        ))}
      </div>
      {customOpen ? (
        <CustomSizeDialog
          onCancel={() => setCustomOpen(false)}
          onConfirm={(wMm, hMm) => { setCustomOpen(false); setApp((a) => ({ ...a, cardType: 'custom', customWmm: wMm, customHmm: hMm })); go('scan') }}
        />
      ) : null}
    </ScreenScaffold>
  )
}

// --- Step 2: scan (ScanScreen.kt) ---
function ScanSlotTile({ label, required, scanned, cardType, onScan }) {
  const { CardArtwork } = DS
  return (
    <div className="bv-card" style={{ padding: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 600, color: 'var(--text-heading)' }}>{label}</span>
        <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{required ? 'Required' : 'Optional'}</span>
      </div>
      {scanned ? (
        <CardArtwork type={cardType} />
      ) : (
        <div style={{ aspectRatio: '1.585', borderRadius: 8, border: '2px dashed var(--color-midnight-200)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: 12 }}>
          Not scanned yet
        </div>
      )}
      <button className={`bv-btn bv-btn--${scanned ? 'ghost' : 'primary'} bv-btn--block`} onClick={onScan}>
        {scanned ? 'Retake' : `Scan ${label.toLowerCase()}`}
      </button>
    </div>
  )
}

function CfScan({ go, app, setApp }) {
  const { ScreenScaffold } = DS
  return (
    <ScreenScaffold title="Scan card" height={FRAME_H}
      bottomBar={<>
        <button className="bv-btn bv-btn--primary bv-btn--block" disabled={!app.front} style={app.front ? undefined : { opacity: 0.4, cursor: 'default' }} onClick={() => app.front && go('configure')}>Next</button>
        <button className="bv-btn bv-btn--ghost bv-btn--block" onClick={() => go('cardType')}>Back</button>
      </>}>
      <HelpText>Uses the on-device document scanner — images never leave your phone.</HelpText>
      <ScanSlotTile label="Front" required scanned={app.front} cardType={app.cardType} onScan={() => setApp((a) => ({ ...a, front: true }))} />
      <ScanSlotTile label="Back" scanned={app.back} cardType={app.cardType} onScan={() => setApp((a) => ({ ...a, back: true }))} />
    </ScreenScaffold>
  )
}

// --- Step 3: configure (ConfigureScreen.kt) ---
function MiniArt({ kind }) {
  // Tiny abstract artworks for Purpose / Paper / Format tiles (ExportArtwork.kt equivalents).
  const a = 'currentColor'
  if (kind === 'print') return (
    <svg viewBox="0 0 60 44" style={{ height: '100%', width: '100%' }}><rect x="10" y="4" width="40" height="14" rx="3" fill={a} opacity=".45"/><rect x="6" y="18" width="48" height="16" rx="4" fill={a} opacity=".8"/><rect x="16" y="30" width="28" height="10" rx="2" fill="#fff" stroke={a} strokeOpacity=".5"/></svg>)
  if (kind === 'upload') return (
    <svg viewBox="0 0 60 44" style={{ height: '100%', width: '100%' }}><path d="M30 34 V12 M30 12 l-9 9 M30 12 l9 9" fill="none" stroke={a} strokeWidth="4" strokeLinecap="round" opacity=".8"/><rect x="10" y="36" width="40" height="4" rx="2" fill={a} opacity=".45"/></svg>)
  if (kind === 'a4' || kind === 'a5') return (
    <svg viewBox="0 0 60 44" style={{ height: '100%', width: '100%' }}><rect x={kind === 'a4' ? 18 : 21} y="4" width={kind === 'a4' ? 25 : 18} height="36" rx="2" fill="#fff" stroke={a} strokeOpacity=".7"/><rect x={kind === 'a4' ? 22 : 24} y="10" width={kind === 'a4' ? 17 : 12} height="3" rx="1.5" fill={a} opacity=".4"/><rect x={kind === 'a4' ? 22 : 24} y="16" width={kind === 'a4' ? 13 : 9} height="3" rx="1.5" fill={a} opacity=".4"/></svg>)
  if (kind === 'pdf') return (
    <svg viewBox="0 0 60 44" style={{ height: '100%', width: '100%' }}><rect x="18" y="4" width="24" height="36" rx="3" fill={a} opacity=".15"/><rect x="18" y="4" width="24" height="36" rx="3" fill="none" stroke={a} strokeOpacity=".6"/><text x="30" y="28" textAnchor="middle" fontSize="10" fontWeight="700" fill={a}>PDF</text></svg>)
  return (
    <svg viewBox="0 0 60 44" style={{ height: '100%', width: '100%' }}><rect x="12" y="6" width="36" height="32" rx="3" fill={a} opacity=".15"/><circle cx="22" cy="16" r="4" fill={a} opacity=".6"/><path d="M14 34 l12-12 8 8 6-6 8 10z" fill={a} opacity=".5"/></svg>)
}

function CfConfigure({ go, app, setApp }) {
  const { ScreenScaffold, IllustratedTile, AppSwitch, AppTextField, OutputChip, SelectableCard } = DS
  const [customOpen, setCustomOpen] = React.useState(false)
  const toggle = (setKey, val) => setApp((a) => {
    const s = new Set(a[setKey]); s.has(val) ? s.delete(val) : s.add(val); return { ...a, [setKey]: s }
  })
  const chips = []
  for (const m of app.modes) for (const p of (m === 'Print' ? app.papers : [null])) for (const f of app.formats) {
    if (m === 'Upload' && f === 'PDF' && !app.papers.size) continue
    chips.push([m, p, f].filter(Boolean).join(' · '))
  }
  const complete = app.modes.size && app.formats.size && (app.papers.size || !app.modes.has('Print'))
  return (
    <ScreenScaffold title="Configure output" height={FRAME_H}
      bottomBar={<>
        <button className="bv-btn bv-btn--primary bv-btn--block" style={complete ? undefined : { opacity: 0.4, cursor: 'default' }} onClick={() => complete && go('name')}>Next</button>
        <button className="bv-btn bv-btn--ghost bv-btn--block" onClick={() => go('scan')}>Back</button>
      </>}>
      <SectionLabel>Purpose</SectionLabel>
      <div style={{ display: 'flex', gap: 8 }}>
        <IllustratedTile label="Print" subtitle="Both sides, true size" selected={app.modes.has('Print')} onClick={() => toggle('modes', 'Print')} style={{ flex: 1 }} artwork={<MiniArt kind="print" />} />
        <IllustratedTile label="Upload" subtitle="Compressed to a cap" selected={app.modes.has('Upload')} onClick={() => toggle('modes', 'Upload')} style={{ flex: 1 }} artwork={<MiniArt kind="upload" />} />
      </div>
      <SectionLabel>Paper (up to 2)</SectionLabel>
      <div style={{ display: 'flex', gap: 8 }}>
        {['A4', 'A5'].map((p) => (
          <IllustratedTile key={p} label={p} selected={app.papers.has(p)} onClick={() => toggle('papers', p)} style={{ flex: 1 }} artwork={<MiniArt kind={p.toLowerCase()} />} />
        ))}
      </div>
      <SectionLabel>Format</SectionLabel>
      <div style={{ display: 'flex', gap: 8 }}>
        <IllustratedTile label="PDF" subtitle="Vector, searchable" selected={app.formats.has('PDF')} onClick={() => toggle('formats', 'PDF')} style={{ flex: 1 }} artwork={<MiniArt kind="pdf" />} />
        <IllustratedTile label="JPEG" subtitle="Single flat image" selected={app.formats.has('JPEG')} onClick={() => toggle('formats', 'JPEG')} style={{ flex: 1 }} artwork={<MiniArt kind="jpeg" />} />
      </div>
      <AppSwitch label="Grayscale" checked={app.grayscale} onChange={(v) => setApp((a) => ({ ...a, grayscale: v }))} />
      <AppSwitch label="Trim rounded corners" checked={app.roundCorners} onChange={(v) => setApp((a) => ({ ...a, roundCorners: v }))} />
      <HelpText>Rounds the corners and removes off-colour corner spots from PVC cards like PAN / Aadhaar / Voter ID. Turn off for square-corner paper cards.</HelpText>
      {app.modes.has('Print') ? <AppSwitch label="Crop marks (print)" checked={app.cropMarks} onChange={(v) => setApp((a) => ({ ...a, cropMarks: v }))} /> : null}
      {app.formats.has('PDF') ? (<>
        <AppSwitch label="Searchable PDF text" checked={app.searchable} onChange={(v) => setApp((a) => ({ ...a, searchable: v }))} />
        <HelpText>Embeds the recognized text into the PDF as a hidden, selectable layer.</HelpText>
      </>) : null}
      {app.modes.has('Upload') ? (<>
        <SectionLabel>Max upload size</SectionLabel>
        <AppTextField label="Size cap in KB (blank = no cap)" value={app.sizeCap} onChange={(v) => setApp((a) => ({ ...a, sizeCap: v.replace(/\D/g, '') }))} />
      </>) : null}
      {chips.length ? (<>
        <SectionLabel>Files ({chips.length})</SectionLabel>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>{chips.map((c) => <OutputChip key={c}>{c}</OutputChip>)}</div>
      </>) : null}
      <SectionLabel>Card size</SectionLabel>
      <HelpText>Detected: CR-80, landscape (ratio 1.59)</HelpText>
      <HelpText>{app.sizeOverride === 'Custom' && app.customWmm
        ? `Sizing: ${fmtNum(app.customWmm)} × ${fmtNum(app.customHmm)} mm (custom)`
        : 'Sizing: 85.6 × 54 mm (CR-80)'}</HelpText>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {['Automatic', 'Custom'].map((l) => (
          <SelectableCard key={l} label={l} selected={app.sizeOverride === l}
            onClick={() => { if (l === 'Custom') { setCustomOpen(true) } else { setApp((a) => ({ ...a, sizeOverride: l })) } }} />
        ))}
      </div>
      {customOpen ? (
        <CustomSizeDialog initialWmm={app.customWmm || 85.6} initialHmm={app.customHmm || 54}
          onCancel={() => setCustomOpen(false)}
          onConfirm={(wMm, hMm) => { setCustomOpen(false); setApp((a) => ({ ...a, sizeOverride: 'Custom', customWmm: wMm, customHmm: hMm })) }}
        />
      ) : null}
    </ScreenScaffold>
  )
}

// --- Step 4: name (NameScreen.kt) ---
function CfName({ go, app, setApp }) {
  const { ScreenScaffold, AppTextField } = DS
  return (
    <ScreenScaffold title="Name on file" height={FRAME_H}
      bottomBar={<>
        <button className="bv-btn bv-btn--primary bv-btn--block" onClick={() => go('preview')}>Next</button>
        <button className="bv-btn bv-btn--ghost bv-btn--block" onClick={() => go('configure')}>Back</button>
      </>}>
      <p style={{ margin: 0, fontSize: 14, lineHeight: 1.55, color: 'var(--color-ink)' }}>Used only for the filename. Edit freely; nothing is auto-finalized.</p>
      <AppTextField label="Holder name (optional)" value={app.name} onChange={(v) => setApp((a) => ({ ...a, name: v }))} />
      <HelpText>Suggested from the scan — edit if it's not quite right.</HelpText>
    </ScreenScaffold>
  )
}

// --- Step 5: preview & export (PreviewScreen.kt) ---
function CfPreview({ go, app, setApp, reset }) {
  const { ScreenScaffold, CardArtwork, OutputChip } = DS
  const [done, setDone] = React.useState(null)
  const chips = []
  for (const m of app.modes) for (const p of (m === 'Print' ? app.papers : [null])) for (const f of app.formats) {
    chips.push([m, p, f].filter(Boolean).join(' · '))
  }
  const gray = app.grayscale ? 'grayscale(1)' : 'none'
  return (
    <ScreenScaffold title="Preview & export" height={FRAME_H}
      bottomBar={done ? (<>
        <button className="bv-btn bv-btn--primary bv-btn--block" onClick={() => { reset(); go('cardType') }}>New scan</button>
        <button className="bv-btn bv-btn--ghost bv-btn--block" onClick={() => { reset(); go('home') }}>Home</button>
      </>) : (<>
        <button className="bv-btn bv-btn--primary bv-btn--block" onClick={() => setDone('saved')}>Save</button>
        <button className="bv-btn bv-btn--ghost bv-btn--block" onClick={() => setDone('shared')}>Share</button>
      </>)}>
      <div style={{ background: '#fff', border: '1px solid var(--border-subtle)', borderRadius: 12, padding: '28px 40px', display: 'flex', flexDirection: 'column', gap: 20, aspectRatio: '0.707', justifyContent: 'center', filter: gray }}>
        <CardArtwork type={app.cardType} style={{ width: '78%', margin: '0 auto' }} />
        {app.back ? <CardArtwork type={app.cardType} style={{ width: '78%', margin: '0 auto' }} /> : null}
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>{chips.map((c) => <OutputChip key={c}>{c}</OutputChip>)}</div>
      <button className="bv-btn bv-btn--ghost" style={{ alignSelf: 'flex-start' }} onClick={() => go('configure')}>Change output settings</button>
      {done ? (
        <div style={{ borderRadius: 10, background: 'var(--accent-soft)', padding: '10px 14px', fontSize: 13, color: 'var(--color-teal-700)' }}>
          {done === 'saved' ? `Saved ${chips.length} file(s) to Downloads/CardFit${app.name ? ` as "${app.name}…"` : ''}.` : 'Share sheet opened with the exported files.'}
        </div>
      ) : null}
    </ScreenScaffold>
  )
}

// --- Tasks (TaskListScreen.kt) ---
function CfTasks({ go }) {
  const { ScreenScaffold } = DS
  const [tasks, setTasks] = React.useState([{ id: 1, name: 'Visa application — family', docs: 3 }])
  const [creating, setCreating] = React.useState(false)
  const [name, setName] = React.useState('')
  return (
    <ScreenScaffold title="Tasks" height={FRAME_H} onBack={() => go('home')}>
      <p style={{ margin: 0, fontSize: 14, lineHeight: 1.55, color: 'var(--color-ink)' }}>Group several people's documents into one application set, then export them together.</p>
      <button className="bv-btn bv-btn--primary bv-btn--block" onClick={() => { setCreating(true); setName('') }}>New task</button>
      {tasks.length === 0 ? <HelpText>No tasks yet. Create one to get started.</HelpText> : null}
      {tasks.map((t) => (
        <div key={t.id} className="bv-home-tile" style={{ cursor: 'pointer', width: '100%', boxSizing: 'border-box' }}>
          <span style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <span style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontWeight: 600, color: 'var(--text-heading)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.name || '(untitled task)'}</span>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{t.docs} document(s)</span>
          </span>
          <button className="bv-btn bv-btn--ghost" style={{ padding: '6px 14px', fontSize: 12, flexShrink: 0, whiteSpace: 'nowrap' }} onClick={(e) => { e.stopPropagation(); setTasks((ts) => ts.filter((x) => x.id !== t.id)) }}>Delete</button>
        </div>
      ))}
      {creating ? (
        <div style={{ position: 'absolute', inset: 0, background: 'rgba(22,24,58,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, zIndex: 5 }}>
          <div style={{ background: '#fff', borderRadius: 16, padding: 20, width: '100%', boxSizing: 'border-box', boxShadow: 'var(--shadow-float)', display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 600, color: 'var(--text-heading)' }}>New task</div>
            <div className="bv-field">
              <label>Task name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} autoFocus />
            </div>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <button className="bv-btn bv-btn--ghost" onClick={() => setCreating(false)}>Cancel</button>
              <button className="bv-btn bv-btn--primary" style={name.trim() ? undefined : { opacity: 0.4 }} onClick={() => { if (name.trim()) { setTasks((ts) => [...ts, { id: Date.now(), name: name.trim(), docs: 0 }]); setCreating(false) } }}>Create</button>
            </div>
          </div>
        </div>
      ) : null}
    </ScreenScaffold>
  )
}

// --- About (About & open-source licenses) ---
function CfAbout({ go }) {
  const { ScreenScaffold } = DS
  const licenses = [
    { name: 'Jetpack Compose & AndroidX', license: 'Apache-2.0' },
    { name: 'ML Kit Document Scanner', license: 'Google Play services terms' },
    { name: 'ML Kit Text Recognition', license: 'Google Play services terms' },
    { name: 'Kotlin Coroutines', license: 'Apache-2.0' },
  ]
  return (
    <ScreenScaffold title="About" height={FRAME_H} onBack={() => go('home')}>
      <div className="bv-card" style={{ padding: '24px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, textAlign: 'center' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 24, fontWeight: 600, color: 'var(--text-heading)' }}>CardFit</span>
        <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>Version 1.0 · fully offline</span>
        <span style={{ fontSize: 13, lineHeight: 1.55, color: 'var(--color-ink)', maxWidth: 280 }}>Scan IDs and documents, prepare ID/passport photos, and export print-ready pages — all on your device.</span>
      </div>
      <SectionLabel>Privacy</SectionLabel>
      <div className="bv-card" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {['Everything stays on your device.', 'No accounts, no tracking, no network access.', 'Files are saved only where you choose.'].map((line) => (
          <span key={line} style={{ display: 'flex', gap: 8, alignItems: 'baseline', fontSize: 13, color: 'var(--color-ink)' }}>
            <span style={{ width: 6, height: 6, borderRadius: 999, background: 'var(--color-teal-500)', flexShrink: 0, position: 'relative', top: -2 }}></span>{line}
          </span>
        ))}
      </div>
      <SectionLabel>Open-source licenses</SectionLabel>
      <div className="bv-card" style={{ padding: '4px 16px' }}>
        {licenses.map((l, i) => (
          <div key={l.name} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, padding: '12px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
            <span style={{ fontSize: 13, color: 'var(--color-ink)' }}>{l.name}</span>
            <span style={{ fontSize: 12, color: 'var(--text-muted)', flexShrink: 0 }}>{l.license}</span>
          </div>
        ))}
      </div>
      <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, paddingTop: 8 }}>
        <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Built by</span>
        <img src="../../assets/brand/logo.svg" alt="Bayaan Consultancy" style={{ height: 32 }} />
      </div>
    </ScreenScaffold>
  )
}

// --- Simple placeholder screens ---
function CfPlaceholder({ go, title, body, backTo = 'home' }) {
  const { ScreenScaffold } = DS
  return (
    <ScreenScaffold title={title} height={FRAME_H} onBack={() => go(backTo)}>
      <HelpText>{body}</HelpText>
    </ScreenScaffold>
  )
}

const FRAME_H = 720

Object.assign(window, { CfHome, CfCardType, CfScan, CfConfigure, CfName, CfPreview, CfTasks, CfAbout, CfPlaceholder, FRAME_H })
