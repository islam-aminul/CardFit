import React from 'react'

// Small uppercase label above a heading (site Eyebrow.js).
export function Eyebrow({ tone = 'teal', children, className = '' }) {
  return <p className={`bv-eyebrow bv-eyebrow--${tone} ${className}`}>{children}</p>
}
