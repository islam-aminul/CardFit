import React from 'react'

// Small outlined attribute pill (site Tag.js), e.g. "Fully offline".
export function Tag({ children, className = '' }) {
  return <span className={`bv-tag ${className}`}>{children}</span>
}
