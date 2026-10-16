import { Button, Group, Stack, TextInput } from '@mantine/core';
import { useState } from 'react';

interface TextPromptProps {
  label: string;
  onSubmit: (value: string) => void;
}

export function TextPrompt({ label, onSubmit }: TextPromptProps) {
  const [value, setValue] = useState('');
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(value.trim());
      }}
    >
      <Stack>
        <TextInput label={label} value={value} onChange={(event) => setValue(event.currentTarget.value)} data-autofocus />
        <Group justify="flex-end">
          <Button type="submit" disabled={!value.trim()}>
            Add
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
