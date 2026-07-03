import React from 'react'

// Illustrated select tile (IllustratedTile.kt): small artwork over label + optional
// subtitle; teal border + tint when selected; 40% opacity when disabled.
export function IllustratedTile({ label, subtitle, artwork, selected = false, enabled = true, onClick, style }) {
  return (
    <button className="bv-illustrated" data-selected={selected} data-disabled={!enabled} onClick={enabled ? onClick : undefined} style={style}>
      {artwork ? <span style={{ display: 'block', width: '100%', height: '44px' }}>{artwork}</span> : null}
      <span className="bv-illustrated__label">{label}</span>
      {subtitle ? <span className="bv-illustrated__subtitle">{subtitle}</span> : null}
    </button>
  )
}
