Decorative concentric-arc background ("the signal carried outward") for heroes and CTA panels — never for content areas.

```jsx
<section style={{ position: 'relative', overflow: 'hidden' }}>
  <ArcField tone="light" cx={900} cy={300} />
  <div style={{ position: 'relative' }}>…content…</div>
</section>
```

`tone="dark"` on midnight-900 panels (arcs fade teal → sage → faint indigo). Position the focal dot off to one side (site uses cx 880–900 on heroes, cx 120 on CTAs).
