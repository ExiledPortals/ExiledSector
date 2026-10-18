import { useEffect, useState } from 'react';
import { fractionToPercentText, percentToFractionText } from './decimalText.ts';
import { DecimalInput } from './DecimalInput.tsx';

interface PercentInputProps {
  fraction: string;
  onFraction: (fraction: string) => void;
  label: string;
  inputWidth?: number;
}

export function PercentInput({ fraction, onFraction, label, inputWidth = 78 }: PercentInputProps) {
  const [text, setText] = useState(() => fractionToPercentText(fraction));
  useEffect(() => {
    setText((current) => (percentToFractionText(current) === fraction ? current : fractionToPercentText(fraction)));
  }, [fraction]);
  return (
    <DecimalInput
      w={inputWidth}
      size="xs"
      aria-label={label}
      value={text}
      styles={{ input: { textAlign: 'right' } }}
      onChange={(nextText) => {
        setText(nextText);
        onFraction(percentToFractionText(nextText));
      }}
    />
  );
}
