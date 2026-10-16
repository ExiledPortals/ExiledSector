import { MantineProvider } from '@mantine/core';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router';
import { EditorLayout } from './layout/EditorLayout.tsx';
import { ImagesPage } from './pages/images/ImagesPage.tsx';
import { SocketablesPage } from './pages/socketables/SocketablesPage.tsx';
import { TextPage } from './pages/text/TextPage.tsx';
import { TreePage } from './pages/tree/TreePage.tsx';
import { editorTheme } from './theme.ts';

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false, retry: false, staleTime: Infinity } },
});

const router = createBrowserRouter([
  {
    element: <EditorLayout />,
    children: [
      { index: true, element: <Navigate to="/tree" replace /> },
      { path: '/tree', element: <TreePage /> },
      { path: '/images', element: <ImagesPage /> },
      { path: '/socketables', element: <SocketablesPage /> },
      { path: '/text', element: <TextPage /> },
      { path: '*', element: <Navigate to="/tree" replace /> },
    ],
  },
]);

export function App() {
  return (
    <MantineProvider theme={editorTheme} defaultColorScheme="dark">
      <QueryClientProvider client={queryClient}>
        <ModalsProvider>
          <Notifications position="bottom-right" />
          <RouterProvider router={router} />
        </ModalsProvider>
      </QueryClientProvider>
    </MantineProvider>
  );
}
