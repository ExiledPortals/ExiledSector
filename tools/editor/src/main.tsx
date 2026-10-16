import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App.tsx';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('The page has no #root element.');
}

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
