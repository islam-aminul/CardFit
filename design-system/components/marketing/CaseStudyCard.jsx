import React from 'react'
import { Tag } from '../core/Tag'

// Flagship / case-study card (site CaseStudyCard.js): midnight visual panel + copy.
export function CaseStudyCard({ href = '#', label, status, title, body, tags = [], visual, className = '' }) {
  return (
    <a href={href} className={`bv-case-card ${className}`}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.2fr' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--color-midnight-900)', padding: '40px 32px' }}>
          {visual}
        </div>
        <div style={{ padding: '32px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: 'var(--text-xs)', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.14em', color: 'var(--color-sage-600)' }}>{label}</span>
            {status ? <span className="bv-pill"><span className="bv-dot"></span>{status}</span> : null}
          </div>
          <h3 style={{ margin: '14px 0 0', fontSize: 'var(--text-2xl)', fontWeight: 600, color: 'var(--text-heading)' }}>{title}</h3>
          <p style={{ margin: '10px 0 0', fontSize: 'var(--text-sm)', lineHeight: 1.625, color: 'var(--text-body)' }}>{body}</p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '18px' }}>
            {tags.map((t) => <Tag key={t}>{t}</Tag>)}
          </div>
        </div>
      </div>
    </a>
  )
}
