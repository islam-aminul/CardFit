/* @ds-bundle: {"format":4,"namespace":"BayaanCardFitDesignSystem_94a7f5","components":[{"name":"AppSwitch","sourcePath":"components/app/AppSwitch.jsx"},{"name":"AppTextField","sourcePath":"components/app/AppTextField.jsx"},{"name":"CardArtwork","sourcePath":"components/app/CardArtwork.jsx"},{"name":"HomeTile","sourcePath":"components/app/HomeTile.jsx"},{"name":"IllustratedTile","sourcePath":"components/app/IllustratedTile.jsx"},{"name":"OutputChip","sourcePath":"components/app/OutputChip.jsx"},{"name":"ScreenScaffold","sourcePath":"components/app/ScreenScaffold.jsx"},{"name":"SelectableCard","sourcePath":"components/app/SelectableCard.jsx"},{"name":"ArcDivider","sourcePath":"components/brand/ArcDivider.jsx"},{"name":"ArcField","sourcePath":"components/brand/ArcField.jsx"},{"name":"Logo","sourcePath":"components/brand/Logo.jsx"},{"name":"VoiceArcsMark","sourcePath":"components/brand/VoiceArcsMark.jsx"},{"name":"Button","sourcePath":"components/core/Button.jsx"},{"name":"Container","sourcePath":"components/core/Container.jsx"},{"name":"Eyebrow","sourcePath":"components/core/Eyebrow.jsx"},{"name":"Tag","sourcePath":"components/core/Tag.jsx"},{"name":"ValueBlock","sourcePath":"components/core/ValueBlock.jsx"},{"name":"CaseStudyCard","sourcePath":"components/marketing/CaseStudyCard.jsx"},{"name":"ServiceCard","sourcePath":"components/marketing/ServiceCard.jsx"}],"sourceHashes":{"components/app/AppSwitch.jsx":"1cf3efaa40a8","components/app/AppTextField.jsx":"41f7280b6b0e","components/app/CardArtwork.jsx":"8e865a8a9433","components/app/HomeTile.jsx":"680355b39cc8","components/app/IllustratedTile.jsx":"438c287255bf","components/app/OutputChip.jsx":"82661d2ff776","components/app/ScreenScaffold.jsx":"996a83d41a5d","components/app/SelectableCard.jsx":"3b5fa69dbd79","components/brand/ArcDivider.jsx":"6785f597e210","components/brand/ArcField.jsx":"3266fd7f6fcd","components/brand/Logo.jsx":"93fa1578f47e","components/brand/VoiceArcsMark.jsx":"08a8ab40f473","components/core/Button.jsx":"8b90d1a7d486","components/core/Container.jsx":"2d83b1f991b8","components/core/Eyebrow.jsx":"5d1ac70d2dce","components/core/Tag.jsx":"7df03371ddb6","components/core/ValueBlock.jsx":"23dff2d36c8a","components/marketing/CaseStudyCard.jsx":"f0417fb73bac","components/marketing/ServiceCard.jsx":"b6206ba6595b","ui_kits/cardfit-app/CardFitScreens.jsx":"233e9fa9435c"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.BayaanCardFitDesignSystem_94a7f5 = window.BayaanCardFitDesignSystem_94a7f5 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/app/AppSwitch.jsx
try { (() => {
// Labeled toggle row + switch (ConfigureScreen.kt ToggleRow / M3 Switch, Bayaan-restyled).
function AppSwitch({
  label,
  checked = false,
  onChange,
  enabled = true
}) {
  const sw = /*#__PURE__*/React.createElement("button", {
    className: "bv-switch",
    "data-on": checked,
    role: "switch",
    "aria-checked": checked,
    disabled: !enabled,
    onClick: () => onChange && onChange(!checked),
    style: enabled ? undefined : {
      opacity: 0.4
    }
  });
  if (!label) return sw;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: '12px',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 'var(--text-base)',
      color: enabled ? 'var(--color-ink)' : 'var(--text-muted)'
    }
  }, label), sw);
}
Object.assign(__ds_scope, { AppSwitch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/AppSwitch.jsx", error: String((e && e.message) || e) }); }

// components/app/AppTextField.jsx
try { (() => {
// Outlined text field (M3 OutlinedTextField, Bayaan-restyled): label above,
// teal focus ring, optional helper text.
function AppTextField({
  label,
  value,
  onChange,
  placeholder,
  help,
  type = 'text',
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "bv-field",
    style: style
  }, label ? /*#__PURE__*/React.createElement("label", null, label) : null, /*#__PURE__*/React.createElement("input", {
    type: type,
    value: value,
    placeholder: placeholder,
    onChange: e => onChange && onChange(e.target.value)
  }), help ? /*#__PURE__*/React.createElement("span", {
    className: "bv-field__help"
  }, help) : null);
}
Object.assign(__ds_scope, { AppTextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/AppTextField.jsx", error: String((e && e.message) || e) }); }

// components/app/CardArtwork.jsx
try { (() => {
// Card-type illustrations — SVG port of the app's Compose Canvas CardArtwork.kt.
// Geometry is copied 1:1 (viewBox 317×200 = the 1.585 CR-80 ratio); the original
// Material accent palette is remapped onto Bayaan midnight/teal/sage.
// Deliberately abstract: no real government logos, emblems, or seals.

const cardAccents = {
  pan: '#2A2F6B',
  // midnight-600 (was blue)
  aadhaar: '#14B8A6',
  // teal-500 (was teal)
  epic: '#23275B',
  // midnight-700 (was indigo)
  admit: '#0E9384',
  // teal-600 (was orange)
  custom: '#8E96C4',
  // midnight-300 (was blue-grey)
  free: '#6B8A74' // sage-500 (was green)
};
const CHIP_GOLD = '#CBA135';
const W = 317;
const H = 200;
function IdCard({
  accent,
  withChip,
  photoOnRight,
  clipId
}) {
  const pad = 19;
  const photoW = 82;
  const photoH = 104;
  const photoTop = 60;
  const photoLeft = photoOnRight ? W - pad - photoW : pad;
  const cx = photoLeft + photoW / 2;
  const headR = 18;
  const headCy = photoTop + photoH * 0.32;
  const domeRx = photoW * 0.3;
  const domeRy = photoH * 0.275;
  const domeY = headCy + headR * 0.5 + domeRy;
  const barsLeft = photoOnRight ? pad : photoLeft + photoW + 19;
  const barsRight = photoOnRight ? photoLeft - 19 : W - pad;
  const barsW = barsRight - barsLeft;
  const barH = 14;
  const bars = [1.0, 0.75, 0.55, 0.9];
  return /*#__PURE__*/React.createElement("g", {
    clipPath: `url(#${clipId})`
  }, /*#__PURE__*/React.createElement("rect", {
    width: W,
    height: H,
    fill: accent,
    opacity: "0.12"
  }), /*#__PURE__*/React.createElement("rect", {
    width: W,
    height: H * 0.2,
    fill: accent
  }), /*#__PURE__*/React.createElement("rect", {
    x: photoLeft,
    y: photoTop,
    width: photoW,
    height: photoH,
    rx: "9",
    fill: "#fff"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: cx,
    cy: headCy,
    r: headR,
    fill: accent,
    opacity: "0.55"
  }), /*#__PURE__*/React.createElement("path", {
    d: `M ${cx - domeRx} ${domeY} A ${domeRx} ${domeRy} 0 0 1 ${cx + domeRx} ${domeY} Z`,
    fill: accent,
    opacity: "0.55"
  }), bars.map((frac, i) => /*#__PURE__*/React.createElement("rect", {
    key: i,
    x: barsLeft,
    y: 68 + i * 26,
    width: barsW * frac,
    height: barH,
    rx: barH / 2,
    fill: accent,
    opacity: "0.45"
  })), withChip ? /*#__PURE__*/React.createElement("rect", {
    x: pad,
    y: H * 0.78,
    width: W * 0.14,
    height: H * 0.13,
    rx: "7",
    fill: CHIP_GOLD
  }) : null);
}
function DocumentPage({
  accent
}) {
  const pageW = W * 0.52;
  const pageH = H * 0.86;
  const left = (W - pageW) / 2;
  const top = (H - pageH) / 2;
  const corner = pageW * 0.06;
  const photoW = pageW * 0.24;
  const lineLeft = left + pageW * 0.1;
  const lineH = pageH * 0.045;
  const fracs = [0.45, 0.4, 0.5, 0, 0.8, 0.7, 0.85, 0.6];
  return /*#__PURE__*/React.createElement("g", null, /*#__PURE__*/React.createElement("rect", {
    width: W,
    height: H,
    fill: accent,
    opacity: "0.1"
  }), /*#__PURE__*/React.createElement("rect", {
    x: left,
    y: top,
    width: pageW,
    height: pageH,
    rx: corner,
    fill: "#fff"
  }), /*#__PURE__*/React.createElement("rect", {
    x: left,
    y: top,
    width: pageW,
    height: pageH * 0.14,
    rx: corner,
    fill: accent
  }), /*#__PURE__*/React.createElement("rect", {
    x: left,
    y: top + pageH * 0.07,
    width: pageW,
    height: pageH * 0.07,
    fill: accent
  }), /*#__PURE__*/React.createElement("rect", {
    x: left + pageW - photoW - pageW * 0.08,
    y: top + pageH * 0.2,
    width: photoW,
    height: photoW * 1.2,
    rx: corner * 0.4,
    fill: accent,
    opacity: "0.25"
  }), fracs.map((frac, i) => frac > 0 ? /*#__PURE__*/React.createElement("rect", {
    key: i,
    x: lineLeft,
    y: top + pageH * 0.22 + i * (lineH + pageH * 0.055),
    width: pageW * 0.8 * frac,
    height: lineH,
    rx: lineH / 2,
    fill: accent,
    opacity: "0.35"
  }) : null));
}
function DashedFrame({
  accent
}) {
  return /*#__PURE__*/React.createElement("rect", {
    x: W * 0.08,
    y: H * 0.1,
    width: W * 0.84,
    height: H * 0.8,
    rx: H * 0.09,
    fill: "none",
    stroke: accent,
    strokeWidth: W * 0.012,
    strokeDasharray: `${W * 0.04} ${W * 0.03}`
  });
}
function Arrow({
  accent,
  x1,
  y1,
  x2,
  y2
}) {
  const head = W * 0.05;
  const sw = W * 0.012;
  const dx = x2 - x1,
    dy = y2 - y1;
  const len = Math.hypot(dx, dy) || 1;
  const ux = dx / len,
    uy = dy / len;
  const px = -uy,
    py = ux;
  const mk = (tx, ty, bx, by) => /*#__PURE__*/React.createElement("g", null, /*#__PURE__*/React.createElement("line", {
    x1: tx,
    y1: ty,
    x2: bx + px * head * 0.6,
    y2: by + py * head * 0.6,
    stroke: accent,
    strokeWidth: sw
  }), /*#__PURE__*/React.createElement("line", {
    x1: tx,
    y1: ty,
    x2: bx - px * head * 0.6,
    y2: by - py * head * 0.6,
    stroke: accent,
    strokeWidth: sw
  }));
  return /*#__PURE__*/React.createElement("g", {
    strokeLinecap: "round"
  }, /*#__PURE__*/React.createElement("line", {
    x1: x1,
    y1: y1,
    x2: x2,
    y2: y2,
    stroke: accent,
    strokeWidth: sw
  }), mk(x1, y1, x1 + ux * head, y1 + uy * head), mk(x2, y2, x2 - ux * head, y2 - uy * head));
}
function CustomCard({
  accent
}) {
  return /*#__PURE__*/React.createElement("g", null, /*#__PURE__*/React.createElement("rect", {
    width: W,
    height: H,
    fill: accent,
    opacity: "0.08"
  }), /*#__PURE__*/React.createElement(DashedFrame, {
    accent: accent
  }), /*#__PURE__*/React.createElement(Arrow, {
    accent: accent,
    x1: W * 0.22,
    y1: H * 0.5,
    x2: W * 0.78,
    y2: H * 0.5
  }), /*#__PURE__*/React.createElement(Arrow, {
    accent: accent,
    x1: W * 0.5,
    y1: H * 0.26,
    x2: W * 0.5,
    y2: H * 0.74
  }));
}
function FreeCard({
  accent
}) {
  return /*#__PURE__*/React.createElement("g", null, /*#__PURE__*/React.createElement("rect", {
    width: W,
    height: H,
    fill: accent,
    opacity: "0.1"
  }), /*#__PURE__*/React.createElement(DashedFrame, {
    accent: accent
  }), /*#__PURE__*/React.createElement("rect", {
    x: W * 0.18,
    y: H * 0.26,
    width: W * 0.3,
    height: H * 0.22,
    rx: H * 0.045,
    fill: accent,
    opacity: "0.35"
  }), /*#__PURE__*/React.createElement("rect", {
    x: W * 0.5,
    y: H * 0.42,
    width: W * 0.32,
    height: H * 0.3,
    rx: H * 0.045,
    fill: accent,
    opacity: "0.25"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: W * 0.3,
    cy: H * 0.66,
    r: W * 0.07,
    fill: accent,
    opacity: "0.45"
  }));
}
function CardArtwork({
  type = 'pan',
  style,
  className = ''
}) {
  const accent = cardAccents[type] || cardAccents.pan;
  const clipId = `card-clip-${type}`;
  let body;
  if (type === 'admit') body = /*#__PURE__*/React.createElement(DocumentPage, {
    accent: accent
  });else if (type === 'custom') body = /*#__PURE__*/React.createElement(CustomCard, {
    accent: accent
  });else if (type === 'free') body = /*#__PURE__*/React.createElement(FreeCard, {
    accent: accent
  });else body = /*#__PURE__*/React.createElement(IdCard, {
    accent: accent,
    withChip: type === 'pan',
    photoOnRight: type === 'aadhaar',
    clipId: clipId
  });
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: `0 0 ${W} ${H}`,
    className: className,
    style: {
      display: 'block',
      width: '100%',
      borderRadius: '8px',
      ...style
    },
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("defs", null, /*#__PURE__*/React.createElement("clipPath", {
    id: clipId
  }, /*#__PURE__*/React.createElement("rect", {
    width: W,
    height: H,
    rx: H * 0.09
  }))), body);
}
Object.assign(__ds_scope, { CardArtwork });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/CardArtwork.jsx", error: String((e && e.message) || e) }); }

// components/app/HomeTile.jsx
try { (() => {
// Home flow tile (HomeScreen.kt HomeTile): 52px circular icon, title, subtitle, chevron.
function HomeTile({
  title,
  subtitle,
  icon,
  iconBg = 'var(--accent-soft)',
  iconColor = 'var(--color-teal-700)',
  onClick
}) {
  return /*#__PURE__*/React.createElement("button", {
    className: "bv-home-tile",
    onClick: onClick
  }, /*#__PURE__*/React.createElement("span", {
    className: "bv-home-tile__icon",
    style: {
      background: iconBg,
      color: iconColor
    }
  }, icon), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      gap: '2px'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: '18px',
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, title), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 'var(--text-sm)',
      lineHeight: 1.45,
      color: 'var(--text-muted)'
    }
  }, subtitle)), /*#__PURE__*/React.createElement("span", {
    "aria-hidden": "true",
    style: {
      color: 'var(--text-muted)',
      fontSize: '20px'
    }
  }, "\u203A"));
}
Object.assign(__ds_scope, { HomeTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/HomeTile.jsx", error: String((e && e.message) || e) }); }

// components/app/IllustratedTile.jsx
try { (() => {
// Illustrated select tile (IllustratedTile.kt): small artwork over label + optional
// subtitle; teal border + tint when selected; 40% opacity when disabled.
function IllustratedTile({
  label,
  subtitle,
  artwork,
  selected = false,
  enabled = true,
  onClick,
  style
}) {
  return /*#__PURE__*/React.createElement("button", {
    className: "bv-illustrated",
    "data-selected": selected,
    "data-disabled": !enabled,
    onClick: enabled ? onClick : undefined,
    style: style
  }, artwork ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'block',
      width: '100%',
      height: '44px'
    }
  }, artwork) : null, /*#__PURE__*/React.createElement("span", {
    className: "bv-illustrated__label"
  }, label), subtitle ? /*#__PURE__*/React.createElement("span", {
    className: "bv-illustrated__subtitle"
  }, subtitle) : null);
}
Object.assign(__ds_scope, { IllustratedTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/IllustratedTile.jsx", error: String((e && e.message) || e) }); }

// components/app/OutputChip.jsx
try { (() => {
// Output-combination chip (OutputChip.kt), e.g. "Print · A4 · PDF".
function OutputChip({
  children
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: "bv-chip"
  }, children);
}
Object.assign(__ds_scope, { OutputChip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/OutputChip.jsx", error: String((e && e.message) || e) }); }

// components/app/ScreenScaffold.jsx
try { (() => {
// App step-screen scaffold (ScreenScaffold.kt): top bar with step title,
// scrollable 16px-padded content column (12px gaps), pinned bottom action bar.
function ScreenScaffold({
  title,
  onBack,
  bottomBar,
  children,
  height = 640,
  className = ''
}) {
  const resolvedBottom = bottomBar !== undefined ? bottomBar : onBack ? /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--ghost bv-btn--block",
    onClick: onBack
  }, "Back") : null;
  return /*#__PURE__*/React.createElement("div", {
    className: className,
    style: {
      display: 'flex',
      flexDirection: 'column',
      height,
      background: 'var(--surface-page)',
      overflow: 'hidden'
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "bv-app-topbar"
  }, /*#__PURE__*/React.createElement("h1", null, title)), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: 'auto',
      padding: 'var(--app-screen-pad)',
      display: 'flex',
      flexDirection: 'column',
      gap: 'var(--app-gap)'
    }
  }, children), resolvedBottom ? /*#__PURE__*/React.createElement("div", {
    className: "bv-app-bottombar"
  }, resolvedBottom) : null);
}
Object.assign(__ds_scope, { ScreenScaffold });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/ScreenScaffold.jsx", error: String((e && e.message) || e) }); }

// components/app/SelectableCard.jsx
try { (() => {
// Small tappable select chip-card (SelectableCard.kt): filled tonal rest state,
// teal border + tint when selected.
function SelectableCard({
  label,
  selected = false,
  enabled = true,
  onClick
}) {
  return /*#__PURE__*/React.createElement("button", {
    className: "bv-selectable",
    "data-selected": selected,
    "data-disabled": !enabled,
    onClick: enabled ? onClick : undefined
  }, label);
}
Object.assign(__ds_scope, { SelectableCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/app/SelectableCard.jsx", error: String((e && e.message) || e) }); }

// components/brand/ArcDivider.jsx
try { (() => {
// A thin paired-arc divider — the voice signal stretched flat between
// sections. Ported verbatim from the site's ArcDivider.js. Decorative.
function ArcDivider({
  className = '',
  style
}) {
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 1200 32",
    preserveAspectRatio: "none",
    "aria-hidden": "true",
    className: className,
    style: {
      display: 'block',
      height: '24px',
      width: '100%',
      ...style
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M0 24 Q 600 2 1200 24",
    fill: "none",
    stroke: "#2DC7B5",
    strokeWidth: "1.5",
    opacity: "0.55"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M0 29 Q 600 9 1200 29",
    fill: "none",
    stroke: "#5DCAA5",
    strokeWidth: "1.25",
    opacity: "0.3"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "24",
    cy: "24",
    r: "2.5",
    fill: "#14B8A6"
  }));
}
Object.assign(__ds_scope, { ArcDivider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/brand/ArcDivider.jsx", error: String((e && e.message) || e) }); }

// components/brand/ArcField.jsx
try { (() => {
// The Voice-Arc field: concentric arcs radiating from a focal dot —
// bayaan, a clear signal carried outward. Used behind heroes and CTAs.
// Ported verbatim from the site's ArcField.js. Decorative only.
const arcFieldTones = {
  light: {
    glow: '#14B8A6',
    glowOpacity: 0.14,
    dot: '#14B8A6',
    arcs: [{
      r: 150,
      stroke: '#2DC7B5',
      w: 2,
      o: 0.85
    }, {
      r: 210,
      stroke: '#5DCAA5',
      w: 1.8,
      o: 0.7
    }, {
      r: 275,
      stroke: '#5DCAA5',
      w: 1.6,
      o: 0.55
    }, {
      r: 345,
      stroke: '#8E96C4',
      w: 1.5,
      o: 0.5
    }, {
      r: 420,
      stroke: '#B9BEDC',
      w: 1.3,
      o: 0.45
    }, {
      r: 500,
      stroke: '#B9BEDC',
      w: 1.2,
      o: 0.38
    }, {
      r: 585,
      stroke: '#D9DCEC',
      w: 1,
      o: 0.35
    }]
  },
  dark: {
    glow: '#14B8A6',
    glowOpacity: 0.2,
    dot: '#14B8A6',
    arcs: [{
      r: 150,
      stroke: '#2DC7B5',
      w: 2,
      o: 0.9
    }, {
      r: 210,
      stroke: '#5DCAA5',
      w: 1.8,
      o: 0.75
    }, {
      r: 275,
      stroke: '#93AC99',
      w: 1.6,
      o: 0.6
    }, {
      r: 345,
      stroke: '#6B8A74',
      w: 1.5,
      o: 0.5
    }, {
      r: 420,
      stroke: '#8E96C4',
      w: 1.3,
      o: 0.45
    }, {
      r: 500,
      stroke: '#B9BEDC',
      w: 1.2,
      o: 0.32
    }, {
      r: 585,
      stroke: '#D9DCEC',
      w: 1,
      o: 0.22
    }]
  }
};
function ArcField({
  tone = 'light',
  cx = 880,
  cy = 300,
  className = '',
  style
}) {
  const t = arcFieldTones[tone] ?? arcFieldTones.light;
  const gid = `arc-glow-${tone}-${cx}-${cy}`;
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 1040 560",
    preserveAspectRatio: "xMidYMid slice",
    "aria-hidden": "true",
    className: className,
    style: {
      pointerEvents: 'none',
      position: 'absolute',
      inset: 0,
      height: '100%',
      width: '100%',
      ...style
    }
  }, /*#__PURE__*/React.createElement("defs", null, /*#__PURE__*/React.createElement("radialGradient", {
    id: gid,
    cx: cx,
    cy: cy,
    r: "320",
    gradientUnits: "userSpaceOnUse"
  }, /*#__PURE__*/React.createElement("stop", {
    offset: "0%",
    stopColor: t.glow,
    stopOpacity: t.glowOpacity
  }), /*#__PURE__*/React.createElement("stop", {
    offset: "55%",
    stopColor: t.glow,
    stopOpacity: t.glowOpacity * 0.28
  }), /*#__PURE__*/React.createElement("stop", {
    offset: "100%",
    stopColor: t.glow,
    stopOpacity: "0"
  }))), /*#__PURE__*/React.createElement("rect", {
    width: "1040",
    height: "560",
    fill: `url(#${gid})`
  }), /*#__PURE__*/React.createElement("g", {
    fill: "none",
    strokeLinecap: "round",
    transform: `translate(${cx} ${cy})`
  }, t.arcs.map((a, i) => /*#__PURE__*/React.createElement("path", {
    key: i,
    d: `M0 -${a.r} A${a.r} ${a.r} 0 0 1 0 ${a.r}`,
    stroke: a.stroke,
    strokeWidth: a.w,
    opacity: a.o
  }))), /*#__PURE__*/React.createElement("circle", {
    cx: cx,
    cy: cy,
    r: "7",
    fill: t.dot
  }), /*#__PURE__*/React.createElement("circle", {
    cx: cx,
    cy: cy,
    r: "15",
    fill: "none",
    stroke: t.dot,
    strokeWidth: "1.5",
    opacity: "0.35"
  }));
}
Object.assign(__ds_scope, { ArcField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/brand/ArcField.jsx", error: String((e && e.message) || e) }); }

// components/brand/VoiceArcsMark.jsx
try { (() => {
// The "Voice arcs" mark — concentric arcs radiating from a dot, expressing
// bayaan (بیان): a clear voice. Ported verbatim from the site's VoiceArcsMark.js.
function VoiceArcsMark({
  size = 36,
  light = false,
  className = ''
}) {
  const dot = light ? '#FFFFFF' : '#1E2150';
  const arcInner = '#14B8A6';
  const arcMid = light ? '#FFFFFF' : '#2A2F6B';
  const arcOuter = '#5DCAA5';
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 40 40",
    width: size,
    height: size,
    className: className,
    role: "img",
    "aria-label": "Bayaan"
  }, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "20",
    r: "2.6",
    fill: dot
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 15 A5 5 0 0 1 12 25",
    fill: "none",
    stroke: arcInner,
    strokeWidth: "2.6",
    strokeLinecap: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 9.5 A10.5 10.5 0 0 1 12 30.5",
    fill: "none",
    stroke: arcMid,
    strokeWidth: "2.6",
    strokeLinecap: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 4 A16 16 0 0 1 12 36",
    fill: "none",
    stroke: arcOuter,
    strokeWidth: "2.6",
    strokeLinecap: "round"
  }));
}
Object.assign(__ds_scope, { VoiceArcsMark });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/brand/VoiceArcsMark.jsx", error: String((e && e.message) || e) }); }

// components/brand/Logo.jsx
try { (() => {
// Full logo lockup: Voice arcs mark + "Bayaan / CONSULTANCY" wordmark.
// Ported from the site's Logo.js.
function Logo({
  light = false,
  className = ''
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: className,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: '10px'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.VoiceArcsMark, {
    size: 36,
    light: light
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      lineHeight: 1
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: '20px',
      fontWeight: 600,
      letterSpacing: 'var(--tracking-tight)',
      color: light ? '#fff' : 'var(--color-midnight-800)'
    }
  }, "Bayaan"), /*#__PURE__*/React.createElement("span", {
    style: {
      marginTop: '4px',
      fontSize: '10px',
      fontWeight: 500,
      letterSpacing: 'var(--tracking-wordmark)',
      color: 'var(--color-teal-500)'
    }
  }, "CONSULTANCY")));
}
Object.assign(__ds_scope, { Logo });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/brand/Logo.jsx", error: String((e && e.message) || e) }); }

// components/core/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Site Button.js port. Pill button, five variants.
function Button({
  variant = 'primary',
  block = false,
  href,
  children,
  className = '',
  ...rest
}) {
  const cls = ['bv-btn', `bv-btn--${variant}`, block ? 'bv-btn--block' : '', className].filter(Boolean).join(' ');
  if (href) {
    return /*#__PURE__*/React.createElement("a", _extends({
      href: href,
      className: cls
    }, rest), children);
  }
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    className: cls
  }, rest), children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Button.jsx", error: String((e && e.message) || e) }); }

// components/core/Container.jsx
try { (() => {
// Page container (site Container.js): max-width column with side padding.
function Container({
  children,
  className = '',
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: className,
    style: {
      maxWidth: 'var(--container-max)',
      margin: '0 auto',
      padding: '0 var(--container-pad)',
      ...style
    }
  }, children);
}
Object.assign(__ds_scope, { Container });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Container.jsx", error: String((e && e.message) || e) }); }

// components/core/Eyebrow.jsx
try { (() => {
// Small uppercase label above a heading (site Eyebrow.js).
function Eyebrow({
  tone = 'teal',
  children,
  className = ''
}) {
  return /*#__PURE__*/React.createElement("p", {
    className: `bv-eyebrow bv-eyebrow--${tone} ${className}`
  }, children);
}
Object.assign(__ds_scope, { Eyebrow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Eyebrow.jsx", error: String((e && e.message) || e) }); }

// components/core/Tag.jsx
try { (() => {
// Small outlined attribute pill (site Tag.js), e.g. "Fully offline".
function Tag({
  children,
  className = ''
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: `bv-tag ${className}`
  }, children);
}
Object.assign(__ds_scope, { Tag });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Tag.jsx", error: String((e && e.message) || e) }); }

// components/core/ValueBlock.jsx
try { (() => {
// Value / principle block: short accent bar over heading + body (site ValueBlock.js).
function ValueBlock({
  title,
  body,
  tone = 'teal',
  className = ''
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: className
  }, /*#__PURE__*/React.createElement("div", {
    className: `bv-bar bv-bar--${tone}`
  }), /*#__PURE__*/React.createElement("h3", {
    style: {
      marginTop: '16px',
      marginBottom: 0,
      fontSize: 'var(--text-lg)',
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, title), /*#__PURE__*/React.createElement("p", {
    style: {
      marginTop: '8px',
      marginBottom: 0,
      fontSize: 'var(--text-sm)',
      lineHeight: 1.625,
      color: 'var(--text-body)'
    }
  }, body));
}
Object.assign(__ds_scope, { ValueBlock });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/ValueBlock.jsx", error: String((e && e.message) || e) }); }

// components/marketing/CaseStudyCard.jsx
try { (() => {
// Flagship / case-study card (site CaseStudyCard.js): midnight visual panel + copy.
function CaseStudyCard({
  href = '#',
  label,
  status,
  title,
  body,
  tags = [],
  visual,
  className = ''
}) {
  return /*#__PURE__*/React.createElement("a", {
    href: href,
    className: `bv-case-card ${className}`
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1.2fr'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--color-midnight-900)',
      padding: '40px 32px'
    }
  }, visual), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '32px'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: '12px'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 'var(--text-xs)',
      fontWeight: 500,
      textTransform: 'uppercase',
      letterSpacing: '0.14em',
      color: 'var(--color-sage-600)'
    }
  }, label), status ? /*#__PURE__*/React.createElement("span", {
    className: "bv-pill"
  }, /*#__PURE__*/React.createElement("span", {
    className: "bv-dot"
  }), status) : null), /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: '14px 0 0',
      fontSize: 'var(--text-2xl)',
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, title), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '10px 0 0',
      fontSize: 'var(--text-sm)',
      lineHeight: 1.625,
      color: 'var(--text-body)'
    }
  }, body), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '8px',
      marginTop: '18px'
    }
  }, tags.map(t => /*#__PURE__*/React.createElement(__ds_scope.Tag, {
    key: t
  }, t))))));
}
Object.assign(__ds_scope, { CaseStudyCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketing/CaseStudyCard.jsx", error: String((e && e.message) || e) }); }

// components/marketing/ServiceCard.jsx
try { (() => {
// Service offering card (site ServiceCard.js). Icon tile reuses the voice-arc mark.
function ServiceCard({
  title,
  body,
  className = ''
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: `bv-card ${className}`
  }, /*#__PURE__*/React.createElement("span", {
    className: "bv-icontile"
  }, /*#__PURE__*/React.createElement(__ds_scope.VoiceArcsMark, {
    size: 24
  })), /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: '16px 0 0',
      fontSize: 'var(--text-lg)',
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, title), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '8px 0 0',
      fontSize: 'var(--text-sm)',
      lineHeight: 1.625,
      color: 'var(--text-body)'
    }
  }, body));
}
Object.assign(__ds_scope, { ServiceCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketing/ServiceCard.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cardfit-app/CardFitScreens.jsx
try { (() => {
// CardFit app screens — Bayaan-restyled recreations of the Compose screens
// (ui/screens/*.kt). Loaded via Babel; components are shared on window.
const DS = window.BayaanCardFitDesignSystem_94a7f5;
function MatIcon({
  name,
  style
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-rounded",
    "aria-hidden": "true",
    style: style
  }, name);
}
function SectionLabel({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: 14,
      fontWeight: 600,
      color: 'var(--text-heading)',
      marginTop: 4
    }
  }, children);
}
function HelpText({
  children
}) {
  return /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 12,
      lineHeight: 1.5,
      color: 'var(--text-muted)'
    }
  }, children);
}

// --- Custom card size dialog (CustomSizeDialog.kt) ---
// All math in mm; UI converts at the boundary (1 cm = 10 mm, 1 inch = 25.4 mm).
// Allowed 20–300 mm per side.
const UNIT_MM = {
  cm: 10,
  inch: 25.4
};
const CUSTOM_MIN_MM = 20;
const CUSTOM_MAX_MM = 300;
function fmtNum(v) {
  const r = Math.round(v * 100) / 100;
  return r % 1 === 0 ? String(Math.round(r)) : String(r);
}
function CustomSizeDialog({
  initialWmm = 85.6,
  initialHmm = 54,
  onCancel,
  onConfirm
}) {
  const {
    SelectableCard,
    AppTextField
  } = DS;
  const [unit, setUnit] = React.useState('cm');
  const [w, setW] = React.useState(fmtNum(initialWmm / UNIT_MM.cm));
  const [h, setH] = React.useState(fmtNum(initialHmm / UNIT_MM.cm));
  const toMm = t => {
    const v = parseFloat(t);
    return isNaN(v) ? null : v * UNIT_MM[unit];
  };
  const inBounds = mm => mm != null && mm >= CUSTOM_MIN_MM && mm <= CUSTOM_MAX_MM;
  const wMm = toMm(w),
    hMm = toMm(h);
  const valid = inBounds(wMm) && inBounds(hMm);
  const switchUnit = u => {
    if (u === unit) return;
    const cw = parseFloat(w),
      ch = parseFloat(h);
    if (!isNaN(cw)) setW(fmtNum(cw * UNIT_MM[unit] / UNIT_MM[u]));
    if (!isNaN(ch)) setH(fmtNum(ch * UNIT_MM[unit] / UNIT_MM[u]));
    setUnit(u);
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'rgba(22,24,58,0.4)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 24,
      zIndex: 5
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      background: '#fff',
      borderRadius: 16,
      padding: 20,
      width: '100%',
      boxSizing: 'border-box',
      boxShadow: 'var(--shadow-float)',
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: 18,
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, "Custom card size"), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 13,
      color: 'var(--color-ink)'
    }
  }, "Enter the physical size of your card."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8
    }
  }, ['cm', 'inch'].map(u => /*#__PURE__*/React.createElement(SelectableCard, {
    key: u,
    label: u,
    selected: unit === u,
    onClick: () => switchUnit(u)
  }))), /*#__PURE__*/React.createElement(AppTextField, {
    label: `Width (${unit})`,
    value: w,
    onChange: setW
  }), /*#__PURE__*/React.createElement(AppTextField, {
    label: `Height (${unit})`,
    value: h,
    onChange: setH
  }), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 12,
      color: w && !inBounds(wMm) || h && !inBounds(hMm) ? '#B3261E' : 'var(--text-muted)'
    }
  }, "Allowed: ", fmtNum(CUSTOM_MIN_MM / UNIT_MM[unit]), "\u2013", fmtNum(CUSTOM_MAX_MM / UNIT_MM[unit]), " ", unit, "."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8,
      justifyContent: 'flex-end'
    }
  }, /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--ghost",
    onClick: onCancel
  }, "Cancel"), /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--primary",
    style: valid ? undefined : {
      opacity: 0.4,
      cursor: 'default'
    },
    onClick: () => valid && onConfirm(wMm, hMm)
  }, "Use size"))));
}

// --- Home (HomeScreen.kt) ---
function CfHome({
  go
}) {
  const {
    ScreenScaffold,
    HomeTile
  } = DS;
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "CardFit",
    height: FRAME_H
  }, /*#__PURE__*/React.createElement(HelpText, null, "Everything stays on your device."), /*#__PURE__*/React.createElement(HomeTile, {
    title: "Documents & cards",
    subtitle: "Scan any document or ID \u2014 both sides laid out on one page.",
    icon: /*#__PURE__*/React.createElement(MatIcon, {
      name: "document_scanner"
    }),
    iconBg: "var(--accent-soft)",
    iconColor: "var(--color-teal-700)",
    onClick: () => go('cardType')
  }), /*#__PURE__*/React.createElement(HomeTile, {
    title: "Photo",
    subtitle: "Crop, enhance, and size a passport / visa / stamp photo.",
    icon: /*#__PURE__*/React.createElement(MatIcon, {
      name: "photo_camera"
    }),
    iconBg: "rgba(107,138,116,0.15)",
    iconColor: "var(--color-sage-700)",
    onClick: () => go('photo')
  }), /*#__PURE__*/React.createElement(HomeTile, {
    title: "Tasks",
    subtitle: "Collect several people's documents into one application set.",
    icon: /*#__PURE__*/React.createElement(MatIcon, {
      name: "folder_copy"
    }),
    iconBg: "var(--color-midnight-50)",
    iconColor: "var(--color-midnight-600)",
    onClick: () => go('tasks')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 'auto'
    }
  }, /*#__PURE__*/React.createElement(HomeTile, {
    title: "About",
    subtitle: "Privacy, version, and open-source licenses.",
    icon: /*#__PURE__*/React.createElement(MatIcon, {
      name: "info"
    }),
    iconBg: "var(--color-midnight-50)",
    iconColor: "var(--color-midnight-600)",
    onClick: () => go('about')
  })));
}

// --- Step 1: card type (CardTypeScreen.kt) ---
const CARD_TYPES = [{
  key: 'pan',
  label: 'PAN',
  sub: '85.6 × 54 mm'
}, {
  key: 'aadhaar',
  label: 'Aadhaar',
  sub: '85.6 × 54 mm'
}, {
  key: 'epic',
  label: 'Voter ID (EPIC)',
  sub: '85.6 × 54 mm'
}, {
  key: 'admit',
  label: 'Admit card',
  sub: 'Fit to page'
}, {
  key: 'custom',
  label: 'Custom',
  sub: 'Your dimensions'
}, {
  key: 'free',
  label: 'Free',
  sub: 'Fit to width'
}];
function CfCardType({
  go,
  setApp
}) {
  const {
    ScreenScaffold,
    CardArtwork
  } = DS;
  const [customOpen, setCustomOpen] = React.useState(false);
  const pick = t => {
    if (t.key === 'custom') {
      setCustomOpen(true);
      return;
    }
    setApp(a => ({
      ...a,
      cardType: t.key
    }));
    go('scan');
  };
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "Choose card type",
    height: FRAME_H,
    onBack: () => go('home')
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 12
    }
  }, CARD_TYPES.map(t => /*#__PURE__*/React.createElement("button", {
    key: t.key,
    className: "bv-home-tile",
    style: {
      flexDirection: 'column',
      alignItems: 'stretch',
      gap: 8,
      padding: 12
    },
    onClick: () => pick(t)
  }, /*#__PURE__*/React.createElement(CardArtwork, {
    type: t.key
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      textAlign: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'block',
      fontFamily: 'var(--font-display)',
      fontSize: 15,
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, t.label), /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'block',
      fontSize: 12,
      color: 'var(--text-muted)'
    }
  }, t.sub))))), customOpen ? /*#__PURE__*/React.createElement(CustomSizeDialog, {
    onCancel: () => setCustomOpen(false),
    onConfirm: (wMm, hMm) => {
      setCustomOpen(false);
      setApp(a => ({
        ...a,
        cardType: 'custom',
        customWmm: wMm,
        customHmm: hMm
      }));
      go('scan');
    }
  }) : null);
}

// --- Step 2: scan (ScanScreen.kt) ---
function ScanSlotTile({
  label,
  required,
  scanned,
  cardType,
  onScan
}) {
  const {
    CardArtwork
  } = DS;
  return /*#__PURE__*/React.createElement("div", {
    className: "bv-card",
    style: {
      padding: 12,
      display: 'flex',
      flexDirection: 'column',
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'baseline',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: 15,
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 11,
      color: 'var(--text-muted)'
    }
  }, required ? 'Required' : 'Optional')), scanned ? /*#__PURE__*/React.createElement(CardArtwork, {
    type: cardType
  }) : /*#__PURE__*/React.createElement("div", {
    style: {
      aspectRatio: '1.585',
      borderRadius: 8,
      border: '2px dashed var(--color-midnight-200)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: 'var(--text-muted)',
      fontSize: 12
    }
  }, "Not scanned yet"), /*#__PURE__*/React.createElement("button", {
    className: `bv-btn bv-btn--${scanned ? 'ghost' : 'primary'} bv-btn--block`,
    onClick: onScan
  }, scanned ? 'Retake' : `Scan ${label.toLowerCase()}`));
}
function CfScan({
  go,
  app,
  setApp
}) {
  const {
    ScreenScaffold
  } = DS;
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "Scan card",
    height: FRAME_H,
    bottomBar: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--primary bv-btn--block",
      disabled: !app.front,
      style: app.front ? undefined : {
        opacity: 0.4,
        cursor: 'default'
      },
      onClick: () => app.front && go('configure')
    }, "Next"), /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--ghost bv-btn--block",
      onClick: () => go('cardType')
    }, "Back"))
  }, /*#__PURE__*/React.createElement(HelpText, null, "Uses the on-device document scanner \u2014 images never leave your phone."), /*#__PURE__*/React.createElement(ScanSlotTile, {
    label: "Front",
    required: true,
    scanned: app.front,
    cardType: app.cardType,
    onScan: () => setApp(a => ({
      ...a,
      front: true
    }))
  }), /*#__PURE__*/React.createElement(ScanSlotTile, {
    label: "Back",
    scanned: app.back,
    cardType: app.cardType,
    onScan: () => setApp(a => ({
      ...a,
      back: true
    }))
  }));
}

// --- Step 3: configure (ConfigureScreen.kt) ---
function MiniArt({
  kind
}) {
  // Tiny abstract artworks for Purpose / Paper / Format tiles (ExportArtwork.kt equivalents).
  const a = 'currentColor';
  if (kind === 'print') return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 60 44",
    style: {
      height: '100%',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement("rect", {
    x: "10",
    y: "4",
    width: "40",
    height: "14",
    rx: "3",
    fill: a,
    opacity: ".45"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "6",
    y: "18",
    width: "48",
    height: "16",
    rx: "4",
    fill: a,
    opacity: ".8"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "16",
    y: "30",
    width: "28",
    height: "10",
    rx: "2",
    fill: "#fff",
    stroke: a,
    strokeOpacity: ".5"
  }));
  if (kind === 'upload') return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 60 44",
    style: {
      height: '100%',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M30 34 V12 M30 12 l-9 9 M30 12 l9 9",
    fill: "none",
    stroke: a,
    strokeWidth: "4",
    strokeLinecap: "round",
    opacity: ".8"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "10",
    y: "36",
    width: "40",
    height: "4",
    rx: "2",
    fill: a,
    opacity: ".45"
  }));
  if (kind === 'a4' || kind === 'a5') return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 60 44",
    style: {
      height: '100%',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement("rect", {
    x: kind === 'a4' ? 18 : 21,
    y: "4",
    width: kind === 'a4' ? 25 : 18,
    height: "36",
    rx: "2",
    fill: "#fff",
    stroke: a,
    strokeOpacity: ".7"
  }), /*#__PURE__*/React.createElement("rect", {
    x: kind === 'a4' ? 22 : 24,
    y: "10",
    width: kind === 'a4' ? 17 : 12,
    height: "3",
    rx: "1.5",
    fill: a,
    opacity: ".4"
  }), /*#__PURE__*/React.createElement("rect", {
    x: kind === 'a4' ? 22 : 24,
    y: "16",
    width: kind === 'a4' ? 13 : 9,
    height: "3",
    rx: "1.5",
    fill: a,
    opacity: ".4"
  }));
  if (kind === 'pdf') return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 60 44",
    style: {
      height: '100%',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement("rect", {
    x: "18",
    y: "4",
    width: "24",
    height: "36",
    rx: "3",
    fill: a,
    opacity: ".15"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "18",
    y: "4",
    width: "24",
    height: "36",
    rx: "3",
    fill: "none",
    stroke: a,
    strokeOpacity: ".6"
  }), /*#__PURE__*/React.createElement("text", {
    x: "30",
    y: "28",
    textAnchor: "middle",
    fontSize: "10",
    fontWeight: "700",
    fill: a
  }, "PDF"));
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 60 44",
    style: {
      height: '100%',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement("rect", {
    x: "12",
    y: "6",
    width: "36",
    height: "32",
    rx: "3",
    fill: a,
    opacity: ".15"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "22",
    cy: "16",
    r: "4",
    fill: a,
    opacity: ".6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 34 l12-12 8 8 6-6 8 10z",
    fill: a,
    opacity: ".5"
  }));
}
function CfConfigure({
  go,
  app,
  setApp
}) {
  const {
    ScreenScaffold,
    IllustratedTile,
    AppSwitch,
    AppTextField,
    OutputChip,
    SelectableCard
  } = DS;
  const [customOpen, setCustomOpen] = React.useState(false);
  const toggle = (setKey, val) => setApp(a => {
    const s = new Set(a[setKey]);
    s.has(val) ? s.delete(val) : s.add(val);
    return {
      ...a,
      [setKey]: s
    };
  });
  const chips = [];
  for (const m of app.modes) for (const p of m === 'Print' ? app.papers : [null]) for (const f of app.formats) {
    if (m === 'Upload' && f === 'PDF' && !app.papers.size) continue;
    chips.push([m, p, f].filter(Boolean).join(' · '));
  }
  const complete = app.modes.size && app.formats.size && (app.papers.size || !app.modes.has('Print'));
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "Configure output",
    height: FRAME_H,
    bottomBar: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--primary bv-btn--block",
      style: complete ? undefined : {
        opacity: 0.4,
        cursor: 'default'
      },
      onClick: () => complete && go('name')
    }, "Next"), /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--ghost bv-btn--block",
      onClick: () => go('scan')
    }, "Back"))
  }, /*#__PURE__*/React.createElement(SectionLabel, null, "Purpose"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(IllustratedTile, {
    label: "Print",
    subtitle: "Both sides, true size",
    selected: app.modes.has('Print'),
    onClick: () => toggle('modes', 'Print'),
    style: {
      flex: 1
    },
    artwork: /*#__PURE__*/React.createElement(MiniArt, {
      kind: "print"
    })
  }), /*#__PURE__*/React.createElement(IllustratedTile, {
    label: "Upload",
    subtitle: "Compressed to a cap",
    selected: app.modes.has('Upload'),
    onClick: () => toggle('modes', 'Upload'),
    style: {
      flex: 1
    },
    artwork: /*#__PURE__*/React.createElement(MiniArt, {
      kind: "upload"
    })
  })), /*#__PURE__*/React.createElement(SectionLabel, null, "Paper (up to 2)"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8
    }
  }, ['A4', 'A5'].map(p => /*#__PURE__*/React.createElement(IllustratedTile, {
    key: p,
    label: p,
    selected: app.papers.has(p),
    onClick: () => toggle('papers', p),
    style: {
      flex: 1
    },
    artwork: /*#__PURE__*/React.createElement(MiniArt, {
      kind: p.toLowerCase()
    })
  }))), /*#__PURE__*/React.createElement(SectionLabel, null, "Format"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(IllustratedTile, {
    label: "PDF",
    subtitle: "Vector, searchable",
    selected: app.formats.has('PDF'),
    onClick: () => toggle('formats', 'PDF'),
    style: {
      flex: 1
    },
    artwork: /*#__PURE__*/React.createElement(MiniArt, {
      kind: "pdf"
    })
  }), /*#__PURE__*/React.createElement(IllustratedTile, {
    label: "JPEG",
    subtitle: "Single flat image",
    selected: app.formats.has('JPEG'),
    onClick: () => toggle('formats', 'JPEG'),
    style: {
      flex: 1
    },
    artwork: /*#__PURE__*/React.createElement(MiniArt, {
      kind: "jpeg"
    })
  })), /*#__PURE__*/React.createElement(AppSwitch, {
    label: "Grayscale",
    checked: app.grayscale,
    onChange: v => setApp(a => ({
      ...a,
      grayscale: v
    }))
  }), /*#__PURE__*/React.createElement(AppSwitch, {
    label: "Trim rounded corners",
    checked: app.roundCorners,
    onChange: v => setApp(a => ({
      ...a,
      roundCorners: v
    }))
  }), /*#__PURE__*/React.createElement(HelpText, null, "Rounds the corners and removes off-colour corner spots from PVC cards like PAN / Aadhaar / Voter ID. Turn off for square-corner paper cards."), app.modes.has('Print') ? /*#__PURE__*/React.createElement(AppSwitch, {
    label: "Crop marks (print)",
    checked: app.cropMarks,
    onChange: v => setApp(a => ({
      ...a,
      cropMarks: v
    }))
  }) : null, app.formats.has('PDF') ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(AppSwitch, {
    label: "Searchable PDF text",
    checked: app.searchable,
    onChange: v => setApp(a => ({
      ...a,
      searchable: v
    }))
  }), /*#__PURE__*/React.createElement(HelpText, null, "Embeds the recognized text into the PDF as a hidden, selectable layer.")) : null, app.modes.has('Upload') ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(SectionLabel, null, "Max upload size"), /*#__PURE__*/React.createElement(AppTextField, {
    label: "Size cap in KB (blank = no cap)",
    value: app.sizeCap,
    onChange: v => setApp(a => ({
      ...a,
      sizeCap: v.replace(/\D/g, '')
    }))
  })) : null, chips.length ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(SectionLabel, null, "Files (", chips.length, ")"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: 8
    }
  }, chips.map(c => /*#__PURE__*/React.createElement(OutputChip, {
    key: c
  }, c)))) : null, /*#__PURE__*/React.createElement(SectionLabel, null, "Card size"), /*#__PURE__*/React.createElement(HelpText, null, "Detected: CR-80, landscape (ratio 1.59)"), /*#__PURE__*/React.createElement(HelpText, null, app.sizeOverride === 'Custom' && app.customWmm ? `Sizing: ${fmtNum(app.customWmm)} × ${fmtNum(app.customHmm)} mm (custom)` : 'Sizing: 85.6 × 54 mm (CR-80)'), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: 8
    }
  }, ['Automatic', 'Custom'].map(l => /*#__PURE__*/React.createElement(SelectableCard, {
    key: l,
    label: l,
    selected: app.sizeOverride === l,
    onClick: () => {
      if (l === 'Custom') {
        setCustomOpen(true);
      } else {
        setApp(a => ({
          ...a,
          sizeOverride: l
        }));
      }
    }
  }))), customOpen ? /*#__PURE__*/React.createElement(CustomSizeDialog, {
    initialWmm: app.customWmm || 85.6,
    initialHmm: app.customHmm || 54,
    onCancel: () => setCustomOpen(false),
    onConfirm: (wMm, hMm) => {
      setCustomOpen(false);
      setApp(a => ({
        ...a,
        sizeOverride: 'Custom',
        customWmm: wMm,
        customHmm: hMm
      }));
    }
  }) : null);
}

// --- Step 4: name (NameScreen.kt) ---
function CfName({
  go,
  app,
  setApp
}) {
  const {
    ScreenScaffold,
    AppTextField
  } = DS;
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "Name on file",
    height: FRAME_H,
    bottomBar: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--primary bv-btn--block",
      onClick: () => go('preview')
    }, "Next"), /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--ghost bv-btn--block",
      onClick: () => go('configure')
    }, "Back"))
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 14,
      lineHeight: 1.55,
      color: 'var(--color-ink)'
    }
  }, "Used only for the filename. Edit freely; nothing is auto-finalized."), /*#__PURE__*/React.createElement(AppTextField, {
    label: "Holder name (optional)",
    value: app.name,
    onChange: v => setApp(a => ({
      ...a,
      name: v
    }))
  }), /*#__PURE__*/React.createElement(HelpText, null, "Suggested from the scan \u2014 edit if it's not quite right."));
}

// --- Step 5: preview & export (PreviewScreen.kt) ---
function CfPreview({
  go,
  app,
  setApp,
  reset
}) {
  const {
    ScreenScaffold,
    CardArtwork,
    OutputChip
  } = DS;
  const [done, setDone] = React.useState(null);
  const chips = [];
  for (const m of app.modes) for (const p of m === 'Print' ? app.papers : [null]) for (const f of app.formats) {
    chips.push([m, p, f].filter(Boolean).join(' · '));
  }
  const gray = app.grayscale ? 'grayscale(1)' : 'none';
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "Preview & export",
    height: FRAME_H,
    bottomBar: done ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--primary bv-btn--block",
      onClick: () => {
        reset();
        go('cardType');
      }
    }, "New scan"), /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--ghost bv-btn--block",
      onClick: () => {
        reset();
        go('home');
      }
    }, "Home")) : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--primary bv-btn--block",
      onClick: () => setDone('saved')
    }, "Save"), /*#__PURE__*/React.createElement("button", {
      className: "bv-btn bv-btn--ghost bv-btn--block",
      onClick: () => setDone('shared')
    }, "Share"))
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      background: '#fff',
      border: '1px solid var(--border-subtle)',
      borderRadius: 12,
      padding: '28px 40px',
      display: 'flex',
      flexDirection: 'column',
      gap: 20,
      aspectRatio: '0.707',
      justifyContent: 'center',
      filter: gray
    }
  }, /*#__PURE__*/React.createElement(CardArtwork, {
    type: app.cardType,
    style: {
      width: '78%',
      margin: '0 auto'
    }
  }), app.back ? /*#__PURE__*/React.createElement(CardArtwork, {
    type: app.cardType,
    style: {
      width: '78%',
      margin: '0 auto'
    }
  }) : null), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: 8
    }
  }, chips.map(c => /*#__PURE__*/React.createElement(OutputChip, {
    key: c
  }, c))), /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--ghost",
    style: {
      alignSelf: 'flex-start'
    },
    onClick: () => go('configure')
  }, "Change output settings"), done ? /*#__PURE__*/React.createElement("div", {
    style: {
      borderRadius: 10,
      background: 'var(--accent-soft)',
      padding: '10px 14px',
      fontSize: 13,
      color: 'var(--color-teal-700)'
    }
  }, done === 'saved' ? `Saved ${chips.length} file(s) to Downloads/CardFit${app.name ? ` as "${app.name}…"` : ''}.` : 'Share sheet opened with the exported files.') : null);
}

// --- Tasks (TaskListScreen.kt) ---
function CfTasks({
  go
}) {
  const {
    ScreenScaffold
  } = DS;
  const [tasks, setTasks] = React.useState([{
    id: 1,
    name: 'Visa application — family',
    docs: 3
  }]);
  const [creating, setCreating] = React.useState(false);
  const [name, setName] = React.useState('');
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "Tasks",
    height: FRAME_H,
    onBack: () => go('home')
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 14,
      lineHeight: 1.55,
      color: 'var(--color-ink)'
    }
  }, "Group several people's documents into one application set, then export them together."), /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--primary bv-btn--block",
    onClick: () => {
      setCreating(true);
      setName('');
    }
  }, "New task"), tasks.length === 0 ? /*#__PURE__*/React.createElement(HelpText, null, "No tasks yet. Create one to get started.") : null, tasks.map(t => /*#__PURE__*/React.createElement("div", {
    key: t.id,
    className: "bv-home-tile",
    style: {
      cursor: 'pointer',
      width: '100%',
      boxSizing: 'border-box'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      minWidth: 0,
      display: 'flex',
      flexDirection: 'column',
      gap: 2
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: 16,
      fontWeight: 600,
      color: 'var(--text-heading)',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }
  }, t.name || '(untitled task)'), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 12,
      color: 'var(--text-muted)'
    }
  }, t.docs, " document(s)")), /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--ghost",
    style: {
      padding: '6px 14px',
      fontSize: 12,
      flexShrink: 0,
      whiteSpace: 'nowrap'
    },
    onClick: e => {
      e.stopPropagation();
      setTasks(ts => ts.filter(x => x.id !== t.id));
    }
  }, "Delete"))), creating ? /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      background: 'rgba(22,24,58,0.4)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 24,
      zIndex: 5
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      background: '#fff',
      borderRadius: 16,
      padding: 20,
      width: '100%',
      boxSizing: 'border-box',
      boxShadow: 'var(--shadow-float)',
      display: 'flex',
      flexDirection: 'column',
      gap: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: 18,
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, "New task"), /*#__PURE__*/React.createElement("div", {
    className: "bv-field"
  }, /*#__PURE__*/React.createElement("label", null, "Task name"), /*#__PURE__*/React.createElement("input", {
    value: name,
    onChange: e => setName(e.target.value),
    autoFocus: true
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8,
      justifyContent: 'flex-end'
    }
  }, /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--ghost",
    onClick: () => setCreating(false)
  }, "Cancel"), /*#__PURE__*/React.createElement("button", {
    className: "bv-btn bv-btn--primary",
    style: name.trim() ? undefined : {
      opacity: 0.4
    },
    onClick: () => {
      if (name.trim()) {
        setTasks(ts => [...ts, {
          id: Date.now(),
          name: name.trim(),
          docs: 0
        }]);
        setCreating(false);
      }
    }
  }, "Create")))) : null);
}

// --- About (About & open-source licenses) ---
function CfAbout({
  go
}) {
  const {
    ScreenScaffold
  } = DS;
  const licenses = [{
    name: 'Jetpack Compose & AndroidX',
    license: 'Apache-2.0'
  }, {
    name: 'ML Kit Document Scanner',
    license: 'Google Play services terms'
  }, {
    name: 'ML Kit Text Recognition',
    license: 'Google Play services terms'
  }, {
    name: 'Kotlin Coroutines',
    license: 'Apache-2.0'
  }];
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: "About",
    height: FRAME_H,
    onBack: () => go('home')
  }, /*#__PURE__*/React.createElement("div", {
    className: "bv-card",
    style: {
      padding: '24px 16px',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 10,
      textAlign: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontSize: 24,
      fontWeight: 600,
      color: 'var(--text-heading)'
    }
  }, "CardFit"), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 12,
      color: 'var(--text-muted)'
    }
  }, "Version 1.0 \xB7 fully offline"), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 13,
      lineHeight: 1.55,
      color: 'var(--color-ink)',
      maxWidth: 280
    }
  }, "Scan IDs and documents, prepare ID/passport photos, and export print-ready pages \u2014 all on your device.")), /*#__PURE__*/React.createElement(SectionLabel, null, "Privacy"), /*#__PURE__*/React.createElement("div", {
    className: "bv-card",
    style: {
      padding: 16,
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, ['Everything stays on your device.', 'No accounts, no tracking, no network access.', 'Files are saved only where you choose.'].map(line => /*#__PURE__*/React.createElement("span", {
    key: line,
    style: {
      display: 'flex',
      gap: 8,
      alignItems: 'baseline',
      fontSize: 13,
      color: 'var(--color-ink)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: 999,
      background: 'var(--color-teal-500)',
      flexShrink: 0,
      position: 'relative',
      top: -2
    }
  }), line))), /*#__PURE__*/React.createElement(SectionLabel, null, "Open-source licenses"), /*#__PURE__*/React.createElement("div", {
    className: "bv-card",
    style: {
      padding: '4px 16px'
    }
  }, licenses.map((l, i) => /*#__PURE__*/React.createElement("div", {
    key: l.name,
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      gap: 12,
      padding: '12px 0',
      borderTop: i ? '1px solid var(--border-subtle)' : 'none'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 13,
      color: 'var(--color-ink)'
    }
  }, l.name), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 12,
      color: 'var(--text-muted)',
      flexShrink: 0
    }
  }, l.license)))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 'auto',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 6,
      paddingTop: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 11,
      color: 'var(--text-muted)'
    }
  }, "Built by"), /*#__PURE__*/React.createElement("img", {
    src: "../../assets/brand/logo.svg",
    alt: "Bayaan Consultancy",
    style: {
      height: 32
    }
  })));
}

// --- Simple placeholder screens ---
function CfPlaceholder({
  go,
  title,
  body,
  backTo = 'home'
}) {
  const {
    ScreenScaffold
  } = DS;
  return /*#__PURE__*/React.createElement(ScreenScaffold, {
    title: title,
    height: FRAME_H,
    onBack: () => go(backTo)
  }, /*#__PURE__*/React.createElement(HelpText, null, body));
}
const FRAME_H = 720;
Object.assign(window, {
  CfHome,
  CfCardType,
  CfScan,
  CfConfigure,
  CfName,
  CfPreview,
  CfTasks,
  CfAbout,
  CfPlaceholder,
  FRAME_H
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cardfit-app/CardFitScreens.jsx", error: String((e && e.message) || e) }); }

__ds_ns.AppSwitch = __ds_scope.AppSwitch;

__ds_ns.AppTextField = __ds_scope.AppTextField;

__ds_ns.CardArtwork = __ds_scope.CardArtwork;

__ds_ns.HomeTile = __ds_scope.HomeTile;

__ds_ns.IllustratedTile = __ds_scope.IllustratedTile;

__ds_ns.OutputChip = __ds_scope.OutputChip;

__ds_ns.ScreenScaffold = __ds_scope.ScreenScaffold;

__ds_ns.SelectableCard = __ds_scope.SelectableCard;

__ds_ns.ArcDivider = __ds_scope.ArcDivider;

__ds_ns.ArcField = __ds_scope.ArcField;

__ds_ns.Logo = __ds_scope.Logo;

__ds_ns.VoiceArcsMark = __ds_scope.VoiceArcsMark;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Container = __ds_scope.Container;

__ds_ns.Eyebrow = __ds_scope.Eyebrow;

__ds_ns.Tag = __ds_scope.Tag;

__ds_ns.ValueBlock = __ds_scope.ValueBlock;

__ds_ns.CaseStudyCard = __ds_scope.CaseStudyCard;

__ds_ns.ServiceCard = __ds_scope.ServiceCard;

})();
