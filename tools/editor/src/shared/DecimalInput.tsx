import { TextInput, type TextInputProps } from '@mantine/core';

interface DecimalInputProps extends Omit<TextInputProps, 'value' | 'onChange'> {
  value: string;
  onChange: (text: string) => void;
}

const ALLOWED_DECIMAL_TEXT = /^[-+]?(?:\d+(?:\.\d*)?|\.\d*)?(?:[eE][-+]?\d*)?$/;

export function DecimalInput({ value, onChange, ...inputProps }: DecimalInputProps) {
  return (
    <TextInput
      inputMode="decimal"
      value={value}
      onChange={(event) => {
        const text = event.currentTarget.value.trim();
        if (ALLOWED_DECIMAL_TEXT.test(text)) {
          onChange(text);
        }
      }}
      {...inputProps}
    />
  );
}
