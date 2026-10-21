import fastifyStatic from '@fastify/static';
import Fastify, { type FastifyInstance } from 'fastify';
import fs from 'node:fs';
import path from 'node:path';
import { gitCheckoutLister, sameRoot, type CheckoutLister } from './checkouts.ts';
import { DescribeService, describeRequestWithStrings } from './describe.ts';
import { CHECKOUT_HEADER, registerCheckoutRoutes, requestedCheckout } from './routes/checkouts.ts';
import { registerDataRoutes } from './routes/data.ts';
import { registerFileRoutes } from './routes/files.ts';
import { registerImageRoutes } from './routes/images.ts';
import { isEditorRequest } from './security.ts';
import type { ProjectPaths } from './paths.ts';

export interface EditorServerOptions {
  paths: ProjectPaths;
  port: number;
  clientDir?: string;
  describe?: DescribeService;
  listCheckouts?: CheckoutLister;
  editorDir?: string;
}

export const BODY_LIMIT_BYTES = 32 * 1024 * 1024;

export function createEditorServer(options: EditorServerOptions): FastifyInstance {
  const app = Fastify({ bodyLimit: BODY_LIMIT_BYTES });
  const describe = options.describe ?? new DescribeService(options.paths);

  app.addHook('onRequest', async (request, reply) => {
    if (request.method !== 'GET' && !isEditorRequest(request.headers, options.port)) {
      return reply.code(403).send({ ok: false, message: 'Rejected: only the skill tree editor page may change files.' });
    }
    const pageCheckout = requestedCheckout(request.headers[CHECKOUT_HEADER]);
    if (request.method !== 'GET' && request.url !== '/api/checkout' && pageCheckout && !sameRoot(pageCheckout, options.paths.projectRoot)) {
      return reply.code(412).send({
        ok: false,
        message: `Rejected: the editor switched to ${options.paths.projectRoot} in another tab. Reload this page to edit that checkout.`,
      });
    }
  });

  registerCheckoutRoutes(app, {
    paths: options.paths,
    describe,
    listCheckouts: options.listCheckouts ?? gitCheckoutLister(options.paths.projectRoot),
    editorDir: options.editorDir ?? path.resolve(import.meta.dirname, '..'),
  });
  registerDataRoutes(app, options.paths);
  registerImageRoutes(app, options.paths);
  registerFileRoutes(app, options.paths);

  app.post('/api/describe', async (request, reply) => {
    const result = await describe.describe(describeRequestWithStrings(options.paths, request.body));
    reply.header('content-type', 'application/json; charset=utf-8');
    return 'raw' in result ? result.raw : result.body;
  });

  app.addHook('onClose', async () => describe.stopHelper());

  const clientDir = options.clientDir;
  if (clientDir && fs.existsSync(path.join(clientDir, 'index.html'))) {
    app.register(fastifyStatic, { root: clientDir, wildcard: false });
    app.setNotFoundHandler((request, reply) => {
      if (request.method === 'GET' && !request.url.startsWith('/api/') && !request.url.startsWith('/files/')) {
        return reply.sendFile('index.html');
      }
      return reply.code(404).send({ ok: false, message: 'Not found.' });
    });
  }

  return app;
}
