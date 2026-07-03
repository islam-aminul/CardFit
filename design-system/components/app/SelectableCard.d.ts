/** Small tappable selection card (text-only). Selected = teal 2px border + teal tint. */
export interface SelectableCardProps {
  label: string;
  selected?: boolean;
  /** Default true. */
  enabled?: boolean;
  onClick?: () => void;
}
