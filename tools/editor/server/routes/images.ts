import type { FastifyInstance, FastifyReply } from 'fastify';
import fs from 'node:fs';
import path from 'node:path';
import { hueShiftImage, makeCircularImage } from '../imageOps.ts';
import { existingProjectFile, resolveProjectPath, toProjectRelative, type ProjectPaths } from '../paths.ts';

const STATIC_IMAGES_DIR = 'graphics/backgrounds/static_images';
const VANILLA_CARGO_ICONS_DIR = 'graphics/icons/cargo';
const CIRCULAR_OUTPUT_DIR = 'graphics/unused/circular';
const PROJECT_IMAGES_DIR = 'graphics/';
const IMAGE_EXTENSION = '.png';

interface CopyBody {
  path?: string;
  suffix?: string;
  hueShift?: unknown;
  overwrite?: boolean;
}

interface ImageCopyOperation {
  destinationDir: (source: string) => string;
  writeCopy: (source: string, destination: string, body: CopyBody) => void;
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
    } else if (entry.isFile() && entry.name.toLowerCase().endsWith(IMAGE_EXTENSION)) {
      found.push(fullPath);
    }
  }
  return found;
}

function compareIgnoringCase(first: string, second: string): number {
  return first.localeCompare(second, undefined, { sensitivity: 'base' });
}

function isProjectImage(paths: ProjectPaths, fullPath: string): boolean {
  const relative = toProjectRelative(paths, fullPath);
  return relative.startsWith(PROJECT_IMAGES_DIR) && relative.toLowerCase().endsWith(IMAGE_EXTENSION);
}

function existingProjectImage(paths: ProjectPaths, relativePath: string | undefined): string | null {
  const fullPath = existingProjectFile(paths, relativePath);
  return fullPath && isProjectImage(paths, fullPath) ? fullPath : null;
}

function projectImageDestination(paths: ProjectPaths, relativePath: string | undefined): string | null {
  const fullPath = resolveProjectPath(paths, relativePath);
  return fullPath && isProjectImage(paths, fullPath) ? fullPath : null;
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

function registerImageCopy(app: FastifyInstance, paths: ProjectPaths, url: string, operation: ImageCopyOperation): void {
  app.post<{ Body: CopyBody }>(url, async (request, reply) => {
    const source = existingProjectImage(paths, request.body?.path);
    if (!source) {
      return reply.code(400).send({ ok: false, message: 'Source image not found.' });
    }
    const suffix = validSuffix(request.body?.suffix);
    if (!suffix) {
      return reply.code(400).send({ ok: false, message: 'Suffix is required and cannot contain path separators.' });
    }
    const destinationDir = operation.destinationDir(source);
    const destination = path.join(destinationDir, `${path.parse(source).name}_${suffix}${IMAGE_EXTENSION}`);
    if (refuseExisting(paths, destination, request.body?.overwrite, reply)) {
      return reply;
    }
    fs.mkdirSync(destinationDir, { recursive: true });
    operation.writeCopy(source, destination, request.body ?? {});
    return { ok: true, path: toProjectRelative(paths, destination) };
  });
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

  registerImageCopy(app, paths, '/api/images/colorshift', {
    destinationDir: (source) => path.dirname(source),
    writeCopy: (source, destination, body) => {
      const hueShift = Number(body.hueShift);
      hueShiftImage(source, destination, Number.isFinite(hueShift) ? hueShift : 0);
    },
  });

  registerImageCopy(app, paths, '/api/images/circular', {
    destinationDir: () => path.join(paths.projectRoot, CIRCULAR_OUTPUT_DIR),
    writeCopy: (source, destination) => makeCircularImage(source, destination),
  });

  app.post<{ Body: { from?: string; to?: string } }>('/api/images/move', async (request, reply) => {
    const source = existingProjectImage(paths, request.body?.from);
    if (!source) {
      return reply.code(400).send({ ok: false, message: 'Source image not found.' });
    }
    const destination = projectImageDestination(paths, request.body?.to);
    if (!destination) {
      return reply.code(400).send({ ok: false, message: `Images can only be moved to a ${IMAGE_EXTENSION} path under ${PROJECT_IMAGES_DIR}.` });
    }
    if (fs.existsSync(destination)) {
      return reply.code(400).send({ ok: false, message: `${toProjectRelative(paths, destination)} already exists.` });
    }
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.renameSync(source, destination);
    return { ok: true, path: toProjectRelative(paths, destination) };
  });

  app.post<{ Body: { path?: string } }>('/api/images/delete', async (request, reply) => {
    const target = existingProjectImage(paths, request.body?.path);
    if (!target) {
      return reply.code(400).send({ ok: false, message: 'Image not found.' });
    }
    fs.rmSync(target);
    return { ok: true };
  });
}
