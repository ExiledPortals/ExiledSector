import type { FastifyInstance, FastifyReply } from 'fastify';
import fs from 'node:fs';
import path from 'node:path';
import { hueShiftImage, makeCircularImage } from '../imageOps.ts';
import { resolveProjectPath, toProjectRelative, type ProjectPaths } from '../paths.ts';

const STATIC_IMAGES_DIR = 'graphics/backgrounds/static_images';
const VANILLA_CARGO_ICONS_DIR = 'graphics/icons/cargo';
const CIRCULAR_OUTPUT_DIR = 'graphics/unused/circular';

interface CopyBody {
  path?: string;
  suffix?: string;
  hueShift?: unknown;
  overwrite?: boolean;
}

function listPngs(directory: string, recursive: boolean): string[] {
  if (!fs.existsSync(directory)) {
    return [];
  }
  const found: string[] = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory() && recursive) {
      found.push(...listPngs(fullPath, true));
    } else if (entry.isFile() && entry.name.toLowerCase().endsWith('.png')) {
      found.push(fullPath);
    }
  }
  return found;
}

function compareIgnoringCase(first: string, second: string): number {
  return first.localeCompare(second, undefined, { sensitivity: 'base' });
}

function sourceImage(paths: ProjectPaths, relativePath: string | undefined): string | null {
  const fullPath = resolveProjectPath(paths, relativePath);
  return fullPath && fs.existsSync(fullPath) && fs.statSync(fullPath).isFile() ? fullPath : null;
}

function validSuffix(suffix: string | undefined): string | null {
  const trimmed = (suffix ?? '').trim();
  return trimmed && !/[\\/:]/.test(trimmed) ? trimmed : null;
}

function refuseExisting(paths: ProjectPaths, destination: string, overwrite: boolean | undefined, reply: FastifyReply): boolean {
  if (overwrite !== true && fs.existsSync(destination)) {
    const relative = toProjectRelative(paths, destination);
    reply.code(409).send({ ok: false, exists: true, path: relative, message: `${relative} already exists.` });
    return true;
  }
  return false;
}

export function registerImageRoutes(app: FastifyInstance, paths: ProjectPaths): void {
  app.get('/api/images/static', async () =>
    listPngs(path.join(paths.projectRoot, STATIC_IMAGES_DIR), false)
      .map((fullPath) => `${STATIC_IMAGES_DIR}/${path.basename(fullPath)}`)
      .sort(compareIgnoringCase),
  );

  app.get('/api/images/all', async () => {
    const projectImages = listPngs(path.join(paths.projectRoot, 'graphics'), true)
      .map((fullPath) => toProjectRelative(paths, fullPath))
      .sort(compareIgnoringCase);
    const vanillaIcons = listPngs(path.join(paths.vanillaCoreDir, VANILLA_CARGO_ICONS_DIR), false)
      .map((fullPath) => path.basename(fullPath))
      .sort(compareIgnoringCase)
      .map((name) => `${VANILLA_CARGO_ICONS_DIR}/${name}`);
    return [...projectImages, ...vanillaIcons];
  });

  app.post<{ Body: CopyBody }>('/api/images/colorshift', async (request, reply) => {
    const source = sourceImage(paths, request.body?.path);
    if (!source) {
      return reply.code(400).send({ ok: false, message: 'Source image not found.' });
    }
    const suffix = validSuffix(request.body?.suffix);
    if (!suffix) {
      return reply.code(400).send({ ok: false, message: 'Suffix is required and cannot contain path separators.' });
    }
    const destination = path.join(path.dirname(source), `${path.parse(source).name}_${suffix}.png`);
    if (refuseExisting(paths, destination, request.body?.overwrite, reply)) {
      return reply;
    }
    const hueShift = Number(request.body?.hueShift);
    hueShiftImage(source, destination, Number.isFinite(hueShift) ? hueShift : 0);
    return { ok: true, path: toProjectRelative(paths, destination) };
  });

  app.post<{ Body: CopyBody }>('/api/images/circular', async (request, reply) => {
    const source = sourceImage(paths, request.body?.path);
    if (!source) {
      return reply.code(400).send({ ok: false, message: 'Source image not found.' });
    }
    const suffix = validSuffix(request.body?.suffix);
    if (!suffix) {
      return reply.code(400).send({ ok: false, message: 'Suffix is required and cannot contain path separators.' });
    }
    const outputDir = path.join(paths.projectRoot, CIRCULAR_OUTPUT_DIR);
    const destination = path.join(outputDir, `${path.parse(source).name}_${suffix}.png`);
    if (refuseExisting(paths, destination, request.body?.overwrite, reply)) {
      return reply;
    }
    fs.mkdirSync(outputDir, { recursive: true });
    makeCircularImage(source, destination);
    return { ok: true, path: toProjectRelative(paths, destination) };
  });

  app.post<{ Body: { from?: string; to?: string } }>('/api/images/move', async (request, reply) => {
    const source = sourceImage(paths, request.body?.from);
    const destination = resolveProjectPath(paths, request.body?.to);
    if (!source || !destination) {
      return reply.code(400).send({ ok: false, message: 'Source image not found.' });
    }
    if (fs.existsSync(destination)) {
      return reply.code(400).send({ ok: false, message: `${toProjectRelative(paths, destination)} already exists.` });
    }
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.renameSync(source, destination);
    return { ok: true, path: toProjectRelative(paths, destination) };
  });

  app.post<{ Body: { path?: string } }>('/api/images/delete', async (request, reply) => {
    const target = sourceImage(paths, request.body?.path);
    const relative = target ? toProjectRelative(paths, target) : '';
    if (!target || !relative.startsWith('graphics/') || !relative.toLowerCase().endsWith('.png')) {
      return reply.code(400).send({ ok: false, message: 'Image not found.' });
    }
    fs.rmSync(target);
    return { ok: true };
  });
}
