/** Abstract card-type illustration (SVG port of the app's Compose CardArtwork). No real ID marks. */
export interface CardArtworkProps {
  /** 'pan' | 'aadhaar' | 'epic' (ID cards) | 'admit' (portrait page) | 'custom' (dashed + arrows) | 'free' (dashed + blocks). */
  type?: 'pan' | 'aadhaar' | 'epic' | 'admit' | 'custom' | 'free';
  style?: React.CSSProperties;
  className?: string;
}
