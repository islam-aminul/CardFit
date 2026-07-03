import React from 'react'

// Outlined text field (M3 OutlinedTextField, Bayaan-restyled): label above,
// teal focus ring, optional helper text.
export function AppTextField({ label, value, onChange, placeholder, help, type = 'text', style }) {
  return (
    <div className="bv-field" style={style}>
      {label ? <label>{label}</label> : null}
      <input type={type} value={value} placeholder={placeholder} onChange={(e) => onChange && onChange(e.target.value)} />
      {help ? <span className="bv-field__help">{help}</span> : null}
    </div>
  )
}
