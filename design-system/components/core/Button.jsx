import React from 'react'

// Site Button.js port. Pill button, five variants.
export function Button({ variant = 'primary', block = false, href, children, className = '', ...rest }) {
  const cls = ['bv-btn', `bv-btn--${variant}`, block ? 'bv-btn--block' : '', className].filter(Boolean).join(' ')
  if (href) {
    return <a href={href} className={cls} {...rest}>{children}</a>
  }
  return <button type="button" className={cls} {...rest}>{children}</button>
}
