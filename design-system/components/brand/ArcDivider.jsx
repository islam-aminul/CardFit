import React from 'react'

// A thin paired-arc divider — the voice signal stretched flat between
// sections. Ported verbatim from the site's ArcDivider.js. Decorative.
export function ArcDivider({ className = '', style }) {
  return (
    <svg
      viewBox="0 0 1200 32"
      preserveAspectRatio="none"
      aria-hidden="true"
      className={className}
      style={{ display: 'block', height: '24px', width: '100%', ...style }}
    >
      <path d="M0 24 Q 600 2 1200 24" fill="none" stroke="#2DC7B5" strokeWidth="1.5" opacity="0.55" />
      <path d="M0 29 Q 600 9 1200 29" fill="none" stroke="#5DCAA5" strokeWidth="1.25" opacity="0.3" />
      <circle cx="24" cy="24" r="2.5" fill="#14B8A6" />
    </svg>
  )
}
