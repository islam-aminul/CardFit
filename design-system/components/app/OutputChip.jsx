import React from 'react'

// Output-combination chip (OutputChip.kt), e.g. "Print · A4 · PDF".
export function OutputChip({ children }) {
  return <span className="bv-chip">{children}</span>
}
