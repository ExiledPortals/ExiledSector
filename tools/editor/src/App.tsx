import { MantineProvider } from '@mantine/core';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { EditorLayout } from './layout/EditorLayout.tsx';
import { ImagesPage } from './pages/images/ImagesPage.tsx';
import { SocketablesPage } from './pages/socketables/SocketablesPage.tsx';
import { TreePage } from './pages/tree/TreePage.tsx';
import { editorTheme } from './theme.ts';

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false, retry: false, staleTime: Infinity } },
});

export function App() {
  return (
    <MantineProvider theme={editorTheme} defaultColorScheme="dark">
      <QueryClientProvider client={queryClient}>
        <ModalsProvider>
          <Notifications position="bottom-right" />
          <BrowserRouter>
            <Routes>
              <Route element={<EditorLayout />}>
                <Route index element={<Navigate to="/tree" replace />} />
                <Route path="/tree" element={<TreePage />} />
                <Route path="/images" element={<ImagesPage />} />
                <Route path="/socketables" element={<SocketablesPage />} />
                <Route path="*" element={<Navigate to="/tree" replace />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </ModalsProvider>
      </QueryClientProvider>
    </MantineProvider>
  );
}
