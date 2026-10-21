import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import type { FastifyInstance } from 'fastify';
import { vi } from 'vitest';
import { createEditorServer, type EditorServerOptions } from '../../server/app.ts';
import { DescribeService } from '../../server/describe.ts';

const PORT = 8792;
const REPOSITORY_ROOT = path.resolve(import.meta.dirname, '..', '..', '..', '..');

export interface TestServer {
  projectRoot: string;
  app: FastifyInstance;
  readProjectFile: (relativePath: string) => string;
  close: () => Promise<void>;
}

export function startTestServer(copiedFiles: string[], options: Pick<EditorServerOptions, 'listCheckouts' | 'editorDir'> = {}): TestServer {
  const projectRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'editor-dom-'));
  for (const relativePath of copiedFiles) {
    const target = path.join(projectRoot, relativePath);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.copyFileSync(path.join(REPOSITORY_ROOT, relativePath), target);
  }
  const paths = { projectRoot, vanillaCoreDir: path.join(projectRoot, 'vanilla-core') };
  const app = createEditorServer({ ...options, paths, port: PORT, describe: new DescribeService(paths, { javaHome: null, maven: null }) });
  vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.pathname + input.search : input.url;
    const response = await app.inject({
      method: (init?.method ?? 'GET') as 'GET' | 'POST',
      url,
      payload: typeof init?.body === 'string' ? init.body : undefined,
      headers: { ...(init?.headers as Record<string, string> | undefined), host: `localhost:${PORT}` },
    });
    return new Response(response.statusCode === 204 ? null : response.body, {
      status: response.statusCode,
      headers: { 'content-type': String(response.headers['content-type'] ?? 'application/json') },
    });
  });
  return {
    projectRoot,
    app,
    readProjectFile: (relativePath) => fs.readFileSync(path.join(projectRoot, relativePath), 'utf8'),
    close: async () => {
      vi.unstubAllGlobals();
      await app.close();
      fs.rmSync(projectRoot, { recursive: true, force: true });
    },
  };
}

export function readRepositoryFile(relativePath: string): string {
  return fs.readFileSync(path.join(REPOSITORY_ROOT, relativePath), 'utf8');
}
