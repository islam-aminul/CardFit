/** Single-line text input: small uppercase label, 12px-radius field, teal focus ring, optional helper. */
export interface AppTextFieldProps {
  label?: string;
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  /** Helper text under the field. */
  help?: string;
  type?: string;
  style?: React.CSSProperties;
}
