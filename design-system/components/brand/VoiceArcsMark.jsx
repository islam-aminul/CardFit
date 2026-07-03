import React from 'react'

// The "Voice arcs" mark — concentric arcs radiating from a dot, expressing
// bayaan (بیان): a clear voice. Ported verbatim from the site's VoiceArcsMark.js.
export function VoiceArcsMark({ size = 36, light = false, className = '' }) {
  const dot = light ? '#FFFFFF' : '#1E2150'
  const arcInner = '#14B8A6'
  const arcMid = light ? '#FFFFFF' : '#2A2F6B'
  const arcOuter = '#5DCAA5'
  return (
    <svg viewBox="0 0 40 40" width={size} height={size} className={className} role="img" aria-label="Bayaan">
      <circle cx="12" cy="20" r="2.6" fill={dot} />
      <path d="M12 15 A5 5 0 0 1 12 25" fill="none" stroke={arcInner} strokeWidth="2.6" strokeLinecap="round" />
      <path d="M12 9.5 A10.5 10.5 0 0 1 12 30.5" fill="none" stroke={arcMid} strokeWidth="2.6" strokeLinecap="round" />
      <path d="M12 4 A16 16 0 0 1 12 36" fill="none" stroke={arcOuter} strokeWidth="2.6" strokeLinecap="round" />
    </svg>
  )
}
