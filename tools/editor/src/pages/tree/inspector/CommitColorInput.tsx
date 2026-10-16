import { ColorInput, type ColorInputProps } from '@mantine/core';
import { useEffect, useState } from 'react';

interface CommitColorInputProps extends Omit<ColorInputProps, 'value' | 'onChange' | 'onChangeEnd'> {
  value: string;
  onCommit: (colour: string) => void;
}

export function CommitColorInput({ value, onCommit, ...inputProps }: CommitColorInputProps) {
  const [colour, setColour] = useState(value);
  useEffect(() => setColour(value), [value]);
  return (
    <ColorInput
      format="hex"
      value={colour}
      onChange={setColour}
      onChangeEnd={(next) => {
        if (/^#[0-9a-fA-F]{6}$/.test(next) && next !== value) {
          onCommit(next);
        }
      }}
      onBlur={() => {
        if (/^#[0-9a-fA-F]{6}$/.test(colour) && colour !== value) {
          onCommit(colour);
        }
      }}
      {...inputProps}
    />
  );
}
