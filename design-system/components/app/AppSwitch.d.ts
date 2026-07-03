/** Toggle row: label left, teal switch right. Omit label for a bare switch. */
export interface AppSwitchProps {
  label?: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  enabled?: boolean;
}
