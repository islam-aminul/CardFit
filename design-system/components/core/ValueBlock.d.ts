/** Principle/value block: a 40×4px accent bar over a heading and short body. */
export interface ValueBlockProps {
  title: string;
  body: string;
  /** Alternate teal/sage across a row. Default 'teal'. */
  tone?: 'teal' | 'sage';
  className?: string;
}
