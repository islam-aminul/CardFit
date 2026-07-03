/** Home-screen flow tile: circular icon, title + subtitle, trailing chevron. */
export interface HomeTileProps {
  title: string;
  subtitle: string;
  /** Icon node (Material Symbols span). */
  icon?: React.ReactNode;
  /** Circle background. Default teal accent-soft. */
  iconBg?: string;
  iconColor?: string;
  onClick?: () => void;
}
