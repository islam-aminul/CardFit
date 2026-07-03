import React from 'react'
import { VoiceArcsMark } from './VoiceArcsMark'

// Full logo lockup: Voice arcs mark + "Bayaan / CONSULTANCY" wordmark.
// Ported from the site's Logo.js.
export function Logo({ light = false, className = '' }) {
  return (
    <span className={className} style={{ display: 'inline-flex', alignItems: 'center', gap: '10px' }}>
      <VoiceArcsMark size={36} light={light} />
      <span style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
        <span
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: '20px',
            fontWeight: 600,
            letterSpacing: 'var(--tracking-tight)',
            color: light ? '#fff' : 'var(--color-midnight-800)',
          }}
        >
          Bayaan
        </span>
        <span
          style={{
            marginTop: '4px',
            fontSize: '10px',
            fontWeight: 500,
            letterSpacing: 'var(--tracking-wordmark)',
            color: 'var(--color-teal-500)',
          }}
        >
          CONSULTANCY
        </span>
      </span>
    </span>
  )
}
