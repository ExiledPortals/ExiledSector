import { Center, Stack, Text, Title } from '@mantine/core';

export function PagePlaceholder({ title, description }: { title: string; description: string }) {
  return (
    <Center h="100%">
      <Stack align="center" gap="xs">
        <Title order={2}>{title}</Title>
        <Text c="dimmed">{description}</Text>
      </Stack>
    </Center>
  );
}
