import path from 'node:path';
import { defineConfig } from 'vitest/config';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..');

export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: [['lcov', { projectRoot: repositoryRoot }], 'text-summary'],
      reportsDirectory: 'coverage',
      include: ['server/**/*.ts', 'src/**/*.{ts,tsx}'],
      exclude: ['**/*.test.ts', '**/*.test.tsx', 'src/test/**'],
    },
    projects: [
      {
        extends: true,
        test: { name: 'node', include: ['server/**/*.test.ts', 'src/**/*.test.ts'], environment: 'node' },
      },
      {
        extends: true,
        test: { name: 'dom', include: ['src/**/*.test.tsx'], environment: 'jsdom', setupFiles: ['src/test/domSetup.ts'], testTimeout: 30000 },
      },
    ],
  },
});
