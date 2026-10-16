import { Textarea, TextInput, type TextareaProps, type TextInputProps } from '@mantine/core';
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';

function useCommittedText(value: string, onCommit: (text: string) => void) {
  const [text, setText] = useState(value);
  const textRef = useRef(text);
  const valueRef = useRef(value);
  const commitRef = useRef(onCommit);
  textRef.current = text;
  commitRef.current = onCommit;

  useEffect(() => {
    valueRef.current = value;
    setText(value);
  }, [value]);

  useEffect(
    () => () => {
      if (textRef.current !== valueRef.current) {
        commitRef.current(textRef.current);
      }
    },
    [],
  );

  const commit = () => {
    if (textRef.current !== valueRef.current) {
      const committed = textRef.current;
      textRef.current = valueRef.current;
      setText(valueRef.current);
      commitRef.current(committed);
    }
  };
  return { text, setText, commit };
}

interface CommitTextInputProps extends Omit<TextInputProps, 'value' | 'onChange'> {
  value: string;
  onCommit: (text: string) => void;
}

export function CommitTextInput({ value, onCommit, ...inputProps }: CommitTextInputProps) {
  const { text, setText, commit } = useCommittedText(value, onCommit);
  return (
    <TextInput
      value={text}
      onChange={(event) => setText(event.currentTarget.value)}
      onBlur={commit}
      onKeyDown={(event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
          commit();
        }
      }}
      {...inputProps}
    />
  );
}

interface CommitNumberInputProps extends Omit<TextInputProps, 'value' | 'onChange'> {
  value: number | null | undefined;
  onCommit: (value: number) => void;
}

export function CommitNumberInput({ value, onCommit, ...inputProps }: CommitNumberInputProps) {
  const shown = value === null || value === undefined || Number.isNaN(value) ? '' : String(value);
  return <CommitTextInput inputMode="decimal" value={shown} onCommit={(text) => onCommit(Number.parseFloat(text))} {...inputProps} />;
}

interface CommitTextareaProps extends Omit<TextareaProps, 'value' | 'onChange'> {
  value: string;
  onCommit: (text: string) => void;
}

export function CommitTextarea({ value, onCommit, ...textareaProps }: CommitTextareaProps) {
  const { text, setText, commit } = useCommittedText(value, onCommit);
  return <Textarea value={text} onChange={(event) => setText(event.currentTarget.value)} onBlur={commit} autosize minRows={2} {...textareaProps} />;
}
