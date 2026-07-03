import React from 'react'

// App step-screen scaffold (ScreenScaffold.kt): top bar with step title,
// scrollable 16px-padded content column (12px gaps), pinned bottom action bar.
export function ScreenScaffold({ title, onBack, bottomBar, children, height = 640, className = '' }) {
  const resolvedBottom = bottomBar !== undefined ? bottomBar : (onBack ? <button className="bv-btn bv-btn--ghost bv-btn--block" onClick={onBack}>Back</button> : null)
  return (
    <div className={className} style={{ display: 'flex', flexDirection: 'column', height, background: 'var(--surface-page)', overflow: 'hidden' }}>
      <div className="bv-app-topbar"><h1>{title}</h1></div>
      <div style={{ flex: 1, overflowY: 'auto', padding: 'var(--app-screen-pad)', display: 'flex', flexDirection: 'column', gap: 'var(--app-gap)' }}>
        {children}
      </div>
      {resolvedBottom ? <div className="bv-app-bottombar">{resolvedBottom}</div> : null}
    </div>
  )
}
