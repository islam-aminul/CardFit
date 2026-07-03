import React from 'react'

// Page container (site Container.js): max-width column with side padding.
export function Container({ children, className = '', style }) {
  return (
    <div className={className} style={{ maxWidth: 'var(--container-max)', margin: '0 auto', padding: '0 var(--container-pad)', ...style }}>
      {children}
    </div>
  )
}
