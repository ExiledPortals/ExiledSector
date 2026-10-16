import { MantineProvider } from '@mantine/core';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactNode } from 'react';
import { editorTheme } from '../theme.ts';

export function renderWithProviders(ui: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity } } });
  return render(
    <MantineProvider theme={editorTheme} env="test">
      <QueryClientProvider client={queryClient}>
        <ModalsProvider>
          <Notifications />
          {ui}
        </ModalsProvider>
      </QueryClientProvider>
    </MantineProvider>,
  );
}
