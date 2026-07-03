/** Decorative background field of concentric voice arcs, used behind heroes and CTA panels. Absolutely positioned; parent needs position:relative + overflow:hidden. */
export interface ArcFieldProps {
  /** 'light' for paper surfaces, 'dark' for midnight surfaces. Default 'light'. */
  tone?: 'light' | 'dark';
  /** Focal-point x in the 1040×560 viewBox. Default 880. */
  cx?: number;
  /** Focal-point y in the 1040×560 viewBox. Default 300. */
  cy?: number;
  className?: string;
  style?: React.CSSProperties;
}
