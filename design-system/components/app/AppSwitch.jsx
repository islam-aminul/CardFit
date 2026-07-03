import React from 'react'

// Labeled toggle row + switch (ConfigureScreen.kt ToggleRow / M3 Switch, Bayaan-restyled).
export function AppSwitch({ label, checked = false, onChange, enabled = true }) {
  const sw = (
    <button
      className="bv-switch"
      data-on={checked}
      role="switch"
      aria-checked={checked}
      disabled={!enabled}
      onClick={() => onChange && onChange(!checked)}
      style={enabled ? undefined : { opacity: 0.4 }}
    ></button>
  )
  if (!label) return sw
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', width: '100%' }}>
      <span style={{ fontSize: 'var(--text-base)', color: enabled ? 'var(--color-ink)' : 'var(--text-muted)' }}>{label}</span>
      {sw}
    </div>
  )
}
