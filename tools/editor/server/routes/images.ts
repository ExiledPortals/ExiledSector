import type { FastifyInstance, FastifyReply } from 'fastify';
import fs from 'node:fs';
import path from 'node:path';
import { hueShiftImage, makeCircularImage, rotateImage, transformPng, type RgbaImage } from '../imageOps.ts';
import { existingProjectFile, existingVanillaFile, resolveProjectPath, toProjectRelative, type ProjectPaths } from '../paths.ts';

const STATIC_IMAGES_DIR = 'graphics/backgrounds/static_images';
const VANILLA_CARGO_ICONS_DIR = 'graphics/icons/cargo';
const CIRCULAR_OUTPUT_DIR = 'graphics/unused/circular';
const PROJECT_IMAGES_DIR = 'graphics/';
const IMAGE_EXTENSION = '.png';
const VANILLA_READ_ONLY_MESSAGE = 'Vanilla images are read-only.';

type CopyBody = { path?: string; suffix?: string; overwrite?: boolean } & Record<string, unknown>;

type NumberParameters = Record<string, number>;

interface ImageCopyOperation<TParameters extends NumberParameters> {
  destinationFolder: (sourceFolder: string) => string;
  parameterDefaults: TParameters;
  transform: (image: RgbaImage, parameters: TParameters) => RgbaImage;
}

interface ImageSource {
  fullPath: string;
  relativePath: string;
}

export interface ImageListing {
  projectImages: string[];
  vanillaImages: string[];
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

function finiteNumber(value: unknown, fallback: number): number {
  const parsed = Number(value);
  return value !== null && value !== '' && Number.isFinite(parsed) ? parsed : fallback;
}

function readParameters<TParameters extends NumberParameters>(body: CopyBody, defaults: TParameters): TParameters {
  return Object.fromEntries(Object.entries(defaults).map(([name, fallback]) => [name, finiteNumber(body[name], fallback)])) as TParameters;
}

function relativeTo(rootDir: string, fullPath: string): string {
  return path.relative(rootDir, fullPath).split(path.sep).join('/');
}

function isGraphicsPng(relativePath: string): boolean {
  return relativePath.startsWith(PROJECT_IMAGES_DIR) && relativePath.toLowerCase().endsWith(IMAGE_EXTENSION);
}

function existingProjectImage(paths: ProjectPaths, relativePath: string | undefined): string | null {
  const fullPath = existingProjectFile(paths, relativePath);
  return fullPath && isGraphicsPng(toProjectRelative(paths, fullPath)) ? fullPath : null;
}

function existingVanillaImage(paths: ProjectPaths, relativePath: string | undefined): string | null {
  const fullPath = existingVanillaFile(paths, relativePath);
  if (!fullPath) {
    return null;
  }
  const vanillaRelative = relativeTo(paths.vanillaCoreDir, fullPath);
  return path.posix.dirname(vanillaRelative) === VANILLA_CARGO_ICONS_DIR && isGraphicsPng(vanillaRelative) ? fullPath : null;
}

function copySource(paths: ProjectPaths, relativePath: string | undefined): ImageSource | null {
  const projectImage = existingProjectImage(paths, relativePath);
  if (projectImage) {
    return { fullPath: projectImage, relativePath: toProjectRelative(paths, projectImage) };
  }
  const vanillaImage = existingVanillaImage(paths, relativePath);
  return vanillaImage ? { fullPath: vanillaImage, relativePath: relativeTo(paths.vanillaCoreDir, vanillaImage) } : null;
}

function projectImageDestination(paths: ProjectPaths, relativePath: string | undefined): string | null {
  const fullPath = resolveProjectPath(paths, relativePath);
  return fullPath && isGraphicsPng(toProjectRelative(paths, fullPath)) ? fullPath : null;
}

function validSuffix(suffix: string | undefined): string | null {
  const trimmed = (suffix ?? '').trim();
  return trimmed && !/[\\/:]/.test(trimmed) ? trimmed : null;
}

function refuseExisting(paths: ProjectPaths, destination: string, overwrite: boolean | undefined, reply: FastifyReply): boolean {
  const relative = toProjectRelative(paths, destination);
  if (existingVanillaFile(paths, relative)) {
    reply.code(400).send({ ok: false, message: `${relative} is a vanilla image; a copy there would replace it game-wide.` });
    return true;
  }
  if (overwrite !== true && fs.existsSync(destination)) {
    reply.code(409).send({ ok: false, exists: true, path: relative, message: `${relative} already exists.` });
    return true;
  }
  return false;
}

function sendMissingProjectImage(paths: ProjectPaths, relativePath: string | undefined, notFoundMessage: string, reply: FastifyReply): FastifyReply {
  return reply.code(400).send({ ok: false, message: existingVanillaImage(paths, relativePath) ? VANILLA_READ_ONLY_MESSAGE : notFoundMessage });
}

function registerImageCopy<TParameters extends NumberParameters>(app: FastifyInstance, paths: ProjectPaths, url: string, operation: ImageCopyOperation<TParameters>): void {
  app.post<{ Body: CopyBody }>(url, async (request, reply) => {
    const body = request.body ?? {};
    const source = copySource(paths, body.path);
    if (!source) {
      return reply.code(400).send({ ok: false, message: 'Source image not found.' });
    }
    const suffix = validSuffix(body.suffix);
    if (!suffix) {
      return reply.code(400).send({ ok: false, message: 'Suffix is required and cannot contain path separators.' });
    }
    const destinationDir = path.join(paths.projectRoot, operation.destinationFolder(path.posix.dirname(source.relativePath)));
    const destination = path.join(destinationDir, `${path.parse(source.fullPath).name}_${suffix}${IMAGE_EXTENSION}`);
    if (refuseExisting(paths, destination, body.overwrite, reply)) {
      return reply;
    }
    const parameters = readParameters(body, operation.parameterDefaults);
    fs.mkdirSync(destinationDir, { recursive: true });
    transformPng(source.fullPath, destination, (image) => operation.transform(image, parameters));
    return { ok: true, path: toProjectRelative(paths, destination) };
  });
}

function listImages(paths: ProjectPaths): ImageListing {
  const projectImages = listPngs(path.join(paths.projectRoot, 'graphics'), true)
    .map((fullPath) => toProjectRelative(paths, fullPath))
    .sort(compareIgnoringCase);
  const projectImageKeys = new Set(projectImages.map((relativePath) => relativePath.toLowerCase()));
  const vanillaImages = listPngs(path.join(paths.vanillaCoreDir, VANILLA_CARGO_ICONS_DIR), false)
    .map((fullPath) => `${VANILLA_CARGO_ICONS_DIR}/${path.basename(fullPath)}`)
    .filter((relativePath) => !projectImageKeys.has(relativePath.toLowerCase()))
    .sort(compareIgnoringCase);
  return { projectImages, vanillaImages };
}

export function registerImageRoutes(app: FastifyInstance, paths: ProjectPaths): void {
  app.get('/api/images/static', async () =>
    listPngs(path.join(paths.projectRoot, STATIC_IMAGES_DIR), false)
      .map((fullPath) => `${STATIC_IMAGES_DIR}/${path.basename(fullPath)}`)
      .sort(compareIgnoringCase),
  );

  app.get('/api/images/all', async () => listImages(paths));

  registerImageCopy(app, paths, '/api/images/colorshift', {
    destinationFolder: (sourceFolder) => sourceFolder,
    parameterDefaults: { hueShift: 0 },
    transform: (image, { hueShift }) => hueShiftImage(image, hueShift),
  });

  registerImageCopy(app, paths, '/api/images/circular', {
    destinationFolder: () => CIRCULAR_OUTPUT_DIR,
    parameterDefaults: {},
    transform: (image) => makeCircularImage(image),
  });

  registerImageCopy(app, paths, '/api/images/rotate', {
    destinationFolder: (sourceFolder) => sourceFolder,
    parameterDefaults: { degrees: 0 },
    transform: (image, { degrees }) => rotateImage(image, degrees),
  });

  app.post<{ Body: { from?: string; to?: string } }>('/api/images/move', async (request, reply) => {
    const source = existingProjectImage(paths, request.body?.from);
    if (!source) {
      return sendMissingProjectImage(paths, request.body?.from, 'Source image not found.', reply);
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
      return sendMissingProjectImage(paths, request.body?.path, 'Image not found.', reply);
    }
    fs.rmSync(target);
    return { ok: true };
  });
}
