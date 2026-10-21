import fs from 'node:fs';
import path from 'node:path';
import type { FastifyInstance } from 'fastify';
import { editorCodeFingerprint, sameRoot, type Checkout, type CheckoutLister } from '../checkouts.ts';
import type { DescribeService } from '../describe.ts';
import type { ProjectPaths } from '../paths.ts';

export const CHECKOUT_HEADER = 'x-editor-checkout';

export interface CheckoutRouteOptions {
  paths: ProjectPaths;
  describe: DescribeService;
  listCheckouts: CheckoutLister;
  editorDir: string;
}

interface CheckoutView extends Checkout {
  name: string;
  current: boolean;
  editorMatches: boolean;
}

export function requestedCheckout(headerValue: string | string[] | undefined): string | null {
  if (typeof headerValue !== 'string' || headerValue.length === 0) {
    return null;
  }
  try {
    return decodeURIComponent(headerValue);
  } catch {
    return headerValue;
  }
}

export function registerCheckoutRoutes(app: FastifyInstance, options: CheckoutRouteOptions): void {
  const { paths, describe, listCheckouts, editorDir } = options;
  let runningFingerprint: string | null = null;

  const checkoutViews = (): CheckoutView[] => {
    runningFingerprint ??= editorCodeFingerprint(editorDir);
    return listCheckouts().map((checkout) => ({
      ...checkout,
      name: path.basename(checkout.root),
      current: sameRoot(checkout.root, paths.projectRoot),
      editorMatches: editorCodeFingerprint(path.join(checkout.root, 'tools', 'editor')) === runningFingerprint,
    }));
  };

  app.get('/api/checkouts', async () => ({ ok: true, current: paths.projectRoot, checkouts: checkoutViews() }));

  app.post<{ Body: { root?: string } }>('/api/checkout', async (request, reply) => {
    const wanted = request.body?.root;
    const target = typeof wanted === 'string' ? listCheckouts().find((checkout) => sameRoot(checkout.root, wanted)) : undefined;
    if (!target) {
      return reply.code(400).send({ ok: false, message: 'That folder is not a checkout of this repository.' });
    }
    if (!fs.existsSync(path.join(target.root, 'data', 'skilltrees'))) {
      return reply.code(400).send({ ok: false, message: `${target.root} has no data/skilltrees folder.` });
    }
    if (!sameRoot(target.root, paths.projectRoot)) {
      paths.projectRoot = target.root;
      describe.restartForProject();
    }
    return { ok: true, current: paths.projectRoot };
  });
}
