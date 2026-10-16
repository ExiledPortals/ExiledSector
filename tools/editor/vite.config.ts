import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const serverOrigin = 'http://localhost:8792';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: serverOrigin, changeOrigin: true },
      '/files': { target: serverOrigin, changeOrigin: true },
    },
  },
});
