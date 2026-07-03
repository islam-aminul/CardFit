/** Flagship case-study card: midnight visual panel beside label, status pill, title, body, tags. */
export interface CaseStudyCardProps {
  href?: string;
  /** Small sage uppercase category, e.g. "Android app". */
  label: string;
  /** Optional teal status pill, e.g. "Coming soon to Google Play". */
  status?: string;
  title: string;
  body: string;
  tags?: string[];
  /** Visual for the midnight panel (e.g. the CardFit SVG at assets/cardfit-visual.svg). */
  visual?: React.ReactNode;
  className?: string;
}
