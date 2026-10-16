import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const serverOrigin = 'http://localhost:8792';

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router'],
          mantine: ['@mantine/core', '@mantine/hooks', '@mantine/modals', '@mantine/notifications'],
          data: ['@tanstack/react-query', 'zustand', 'immer'],
        },
      },
    },
  },
  server: {
    port: 5173,
    watch: { usePolling: true, interval: 250 },
    proxy: {
      '/api': { target: serverOrigin, changeOrigin: true },
      '/files': { target: serverOrigin, changeOrigin: true },
    },
  },
});
