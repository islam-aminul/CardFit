/** Bayaan pill button (site Button.js). */
export interface ButtonProps {
  /** 'primary' midnight | 'ghost' outlined | 'teal' accent | 'sage' | 'light' (on dark). Default 'primary'. */
  variant?: 'primary' | 'ghost' | 'teal' | 'sage' | 'light';
  /** Full-width (app bottom bars). Default false. */
  block?: boolean;
  /** Renders an <a> when set. */
  href?: string;
  children?: React.ReactNode;
  className?: string;
  onClick?: () => void;
  disabled?: boolean;
}
