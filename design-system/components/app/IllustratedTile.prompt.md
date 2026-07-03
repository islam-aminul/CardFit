Illustrated multi-select tile used in Configure's Purpose / Paper / Format rows (equal-width in a flex row).

```jsx
<IllustratedTile label="Print" subtitle="Both sides, true size" selected artwork={<PrinterArt />} />
<IllustratedTile label="A4" artwork={<PaperArt ratio={0.707} />} />
```

Disabled tiles (paper cap reached) render at 40% opacity.
