import React from 'react'

// Home flow tile (HomeScreen.kt HomeTile): 52px circular icon, title, subtitle, chevron.
export function HomeTile({ title, subtitle, icon, iconBg = 'var(--accent-soft)', iconColor = 'var(--color-teal-700)', onClick }) {
  return (
    <button className="bv-home-tile" onClick={onClick}>
      <span className="bv-home-tile__icon" style={{ background: iconBg, color: iconColor }}>{icon}</span>
      <span style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '2px' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: '18px', fontWeight: 600, color: 'var(--text-heading)' }}>{title}</span>
        <span style={{ fontSize: 'var(--text-sm)', lineHeight: 1.45, color: 'var(--text-muted)' }}>{subtitle}</span>
      </span>
      <span aria-hidden="true" style={{ color: 'var(--text-muted)', fontSize: '20px' }}>›</span>
    </button>
  )
}
