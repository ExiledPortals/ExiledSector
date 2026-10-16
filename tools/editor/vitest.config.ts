import path from 'node:path';
import { defineConfig } from 'vitest/config';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..');

export default defineConfig({
  test: {
    include: ['server/**/*.test.ts', 'src/**/*.test.ts'],
    environment: 'node',
    coverage: {
      provider: 'v8',
      reporter: [['lcov', { projectRoot: repositoryRoot }], 'text-summary'],
      reportsDirectory: 'coverage',
      include: ['server/**/*.ts', 'src/**/*.{ts,tsx}'],
      exclude: ['**/*.test.ts'],
    },
  },
});
