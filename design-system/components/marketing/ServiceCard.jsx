import React from 'react'
import { VoiceArcsMark } from '../brand/VoiceArcsMark'

// Service offering card (site ServiceCard.js). Icon tile reuses the voice-arc mark.
export function ServiceCard({ title, body, className = '' }) {
  return (
    <div className={`bv-card ${className}`}>
      <span className="bv-icontile"><VoiceArcsMark size={24} /></span>
      <h3 style={{ margin: '16px 0 0', fontSize: 'var(--text-lg)', fontWeight: 600, color: 'var(--text-heading)' }}>{title}</h3>
      <p style={{ margin: '8px 0 0', fontSize: 'var(--text-sm)', lineHeight: 1.625, color: 'var(--text-body)' }}>{body}</p>
    </div>
  )
}
