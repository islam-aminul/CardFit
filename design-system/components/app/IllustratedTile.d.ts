/** Selection tile with a small illustration above the label (Purpose / Paper / Format pickers). */
export interface IllustratedTileProps {
  label: string;
  subtitle?: string;
  /** Small artwork node rendered in a 44px-tall slot. */
  artwork?: React.ReactNode;
  selected?: boolean;
  enabled?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}
