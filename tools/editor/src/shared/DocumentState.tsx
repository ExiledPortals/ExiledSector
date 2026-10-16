import { Alert, Button, Center, Loader, Stack } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import type { DataDocumentState } from './useDataDocument.ts';

interface DocumentStateProps<TModel> {
  state: DataDocumentState<TModel>;
  onRetry: () => void;
  children: (model: TModel) => ReactNode;
}

export function DocumentLoading() {
  return (
    <Center p="xl">
      <Loader />
    </Center>
  );
}

export function DocumentLoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <Stack p="md">
      <Alert color="red" icon={<IconAlertTriangle />} title="Could not load">
        {message}
      </Alert>
      <Button variant="default" w="fit-content" onClick={onRetry}>
        Retry
      </Button>
    </Stack>
  );
}

export function DocumentState<TModel>({ state, onRetry, children }: DocumentStateProps<TModel>) {
  if (state.status === 'loading') {
    return <DocumentLoading />;
  }
  if (state.status === 'error') {
    return <DocumentLoadError message={state.message} onRetry={onRetry} />;
  }
  return <>{children(state.model)}</>;
}
