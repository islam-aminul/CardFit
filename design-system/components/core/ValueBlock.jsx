import React from 'react'

// Value / principle block: short accent bar over heading + body (site ValueBlock.js).
export function ValueBlock({ title, body, tone = 'teal', className = '' }) {
  return (
    <div className={className}>
      <div className={`bv-bar bv-bar--${tone}`}></div>
      <h3 style={{ marginTop: '16px', marginBottom: 0, fontSize: 'var(--text-lg)', fontWeight: 600, color: 'var(--text-heading)' }}>{title}</h3>
      <p style={{ marginTop: '8px', marginBottom: 0, fontSize: 'var(--text-sm)', lineHeight: 1.625, color: 'var(--text-body)' }}>{body}</p>
    </div>
  )
}
