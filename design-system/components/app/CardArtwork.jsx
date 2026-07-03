import React from 'react'

// Card-type illustrations — SVG port of the app's Compose Canvas CardArtwork.kt.
// Geometry is copied 1:1 (viewBox 317×200 = the 1.585 CR-80 ratio); the original
// Material accent palette is remapped onto Bayaan midnight/teal/sage.
// Deliberately abstract: no real government logos, emblems, or seals.

const cardAccents = {
  pan: '#2A2F6B',      // midnight-600 (was blue)
  aadhaar: '#14B8A6',  // teal-500 (was teal)
  epic: '#23275B',     // midnight-700 (was indigo)
  admit: '#0E9384',    // teal-600 (was orange)
  custom: '#8E96C4',   // midnight-300 (was blue-grey)
  free: '#6B8A74',     // sage-500 (was green)
}
const CHIP_GOLD = '#CBA135'
const W = 317
const H = 200

function IdCard({ accent, withChip, photoOnRight, clipId }) {
  const pad = 19
  const photoW = 82
  const photoH = 104
  const photoTop = 60
  const photoLeft = photoOnRight ? W - pad - photoW : pad
  const cx = photoLeft + photoW / 2
  const headR = 18
  const headCy = photoTop + photoH * 0.32
  const domeRx = photoW * 0.3
  const domeRy = photoH * 0.275
  const domeY = headCy + headR * 0.5 + domeRy
  const barsLeft = photoOnRight ? pad : photoLeft + photoW + 19
  const barsRight = photoOnRight ? photoLeft - 19 : W - pad
  const barsW = barsRight - barsLeft
  const barH = 14
  const bars = [1.0, 0.75, 0.55, 0.9]
  return (
    <g clipPath={`url(#${clipId})`}>
      <rect width={W} height={H} fill={accent} opacity="0.12" />
      <rect width={W} height={H * 0.2} fill={accent} />
      <rect x={photoLeft} y={photoTop} width={photoW} height={photoH} rx="9" fill="#fff" />
      <circle cx={cx} cy={headCy} r={headR} fill={accent} opacity="0.55" />
      <path d={`M ${cx - domeRx} ${domeY} A ${domeRx} ${domeRy} 0 0 1 ${cx + domeRx} ${domeY} Z`} fill={accent} opacity="0.55" />
      {bars.map((frac, i) => (
        <rect key={i} x={barsLeft} y={68 + i * 26} width={barsW * frac} height={barH} rx={barH / 2} fill={accent} opacity="0.45" />
      ))}
      {withChip ? <rect x={pad} y={H * 0.78} width={W * 0.14} height={H * 0.13} rx="7" fill={CHIP_GOLD} /> : null}
    </g>
  )
}

function DocumentPage({ accent }) {
  const pageW = W * 0.52
  const pageH = H * 0.86
  const left = (W - pageW) / 2
  const top = (H - pageH) / 2
  const corner = pageW * 0.06
  const photoW = pageW * 0.24
  const lineLeft = left + pageW * 0.1
  const lineH = pageH * 0.045
  const fracs = [0.45, 0.4, 0.5, 0, 0.8, 0.7, 0.85, 0.6]
  return (
    <g>
      <rect width={W} height={H} fill={accent} opacity="0.1" />
      <rect x={left} y={top} width={pageW} height={pageH} rx={corner} fill="#fff" />
      <rect x={left} y={top} width={pageW} height={pageH * 0.14} rx={corner} fill={accent} />
      <rect x={left} y={top + pageH * 0.07} width={pageW} height={pageH * 0.07} fill={accent} />
      <rect x={left + pageW - photoW - pageW * 0.08} y={top + pageH * 0.2} width={photoW} height={photoW * 1.2} rx={corner * 0.4} fill={accent} opacity="0.25" />
      {fracs.map((frac, i) =>
        frac > 0 ? (
          <rect key={i} x={lineLeft} y={top + pageH * 0.22 + i * (lineH + pageH * 0.055)} width={pageW * 0.8 * frac} height={lineH} rx={lineH / 2} fill={accent} opacity="0.35" />
        ) : null
      )}
    </g>
  )
}

function DashedFrame({ accent }) {
  return (
    <rect x={W * 0.08} y={H * 0.1} width={W * 0.84} height={H * 0.8} rx={H * 0.09}
      fill="none" stroke={accent} strokeWidth={W * 0.012} strokeDasharray={`${W * 0.04} ${W * 0.03}`} />
  )
}

function Arrow({ accent, x1, y1, x2, y2 }) {
  const head = W * 0.05
  const sw = W * 0.012
  const dx = x2 - x1, dy = y2 - y1
  const len = Math.hypot(dx, dy) || 1
  const ux = dx / len, uy = dy / len
  const px = -uy, py = ux
  const mk = (tx, ty, bx, by) => (
    <g>
      <line x1={tx} y1={ty} x2={bx + px * head * 0.6} y2={by + py * head * 0.6} stroke={accent} strokeWidth={sw} />
      <line x1={tx} y1={ty} x2={bx - px * head * 0.6} y2={by - py * head * 0.6} stroke={accent} strokeWidth={sw} />
    </g>
  )
  return (
    <g strokeLinecap="round">
      <line x1={x1} y1={y1} x2={x2} y2={y2} stroke={accent} strokeWidth={sw} />
      {mk(x1, y1, x1 + ux * head, y1 + uy * head)}
      {mk(x2, y2, x2 - ux * head, y2 - uy * head)}
    </g>
  )
}

function CustomCard({ accent }) {
  return (
    <g>
      <rect width={W} height={H} fill={accent} opacity="0.08" />
      <DashedFrame accent={accent} />
      <Arrow accent={accent} x1={W * 0.22} y1={H * 0.5} x2={W * 0.78} y2={H * 0.5} />
      <Arrow accent={accent} x1={W * 0.5} y1={H * 0.26} x2={W * 0.5} y2={H * 0.74} />
    </g>
  )
}

function FreeCard({ accent }) {
  return (
    <g>
      <rect width={W} height={H} fill={accent} opacity="0.1" />
      <DashedFrame accent={accent} />
      <rect x={W * 0.18} y={H * 0.26} width={W * 0.3} height={H * 0.22} rx={H * 0.045} fill={accent} opacity="0.35" />
      <rect x={W * 0.5} y={H * 0.42} width={W * 0.32} height={H * 0.3} rx={H * 0.045} fill={accent} opacity="0.25" />
      <circle cx={W * 0.3} cy={H * 0.66} r={W * 0.07} fill={accent} opacity="0.45" />
    </g>
  )
}

export function CardArtwork({ type = 'pan', style, className = '' }) {
  const accent = cardAccents[type] || cardAccents.pan
  const clipId = `card-clip-${type}`
  let body
  if (type === 'admit') body = <DocumentPage accent={accent} />
  else if (type === 'custom') body = <CustomCard accent={accent} />
  else if (type === 'free') body = <FreeCard accent={accent} />
  else body = <IdCard accent={accent} withChip={type === 'pan'} photoOnRight={type === 'aadhaar'} clipId={clipId} />
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className={className} style={{ display: 'block', width: '100%', borderRadius: '8px', ...style }} aria-hidden="true">
      <defs>
        <clipPath id={clipId}><rect width={W} height={H} rx={H * 0.09} /></clipPath>
      </defs>
      {body}
    </svg>
  )
}
