import React from 'react'

// Small tappable select chip-card (SelectableCard.kt): filled tonal rest state,
// teal border + tint when selected.
export function SelectableCard({ label, selected = false, enabled = true, onClick }) {
  return (
    <button className="bv-selectable" data-selected={selected} data-disabled={!enabled} onClick={enabled ? onClick : undefined}>
      {label}
    </button>
  )
}
