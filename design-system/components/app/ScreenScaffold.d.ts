/** CardFit step-screen scaffold: title bar, scrollable 16px content column with 12px gaps, pinned bottom actions. */
export interface ScreenScaffoldProps {
  title: string;
  /** Convenience: renders a full-width ghost "Back" button in the bottom bar. */
  onBack?: () => void;
  /** Custom pinned bottom-bar content (overrides onBack). */
  bottomBar?: React.ReactNode;
  /** Frame height in px. Default 640. */
  height?: number;
  children?: React.ReactNode;
  className?: string;
}
