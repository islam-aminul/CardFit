/** Small uppercase tracked label above a section heading. */
export interface EyebrowProps {
  /** 'teal' default | 'sage' supporting second voice | 'light' teal-300 on dark. */
  tone?: 'teal' | 'sage' | 'light';
  children?: React.ReactNode;
  className?: string;
}
