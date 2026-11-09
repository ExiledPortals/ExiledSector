import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { PNG } from 'pngjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { createEditorServer } from './app.ts';
import { hashBytes, writeTextFilesAtomic } from './dataFiles.ts';
import {
  BUILD_LOCK_FILE,
  builtClasspath,
  checkoutSnapshotsDir,
  copySnapshot,
  DescribeService,
  describeRequestWithStrings,
  latestSnapshot,
  removeOtherSnapshots,
} from './describe.ts';
import type { ProjectPaths } from './paths.ts';

const PORT = 8792;
const EDITOR_HEADERS = { 'x-skill-tree-editor': '1', host: `localhost:${PORT}` };
const TYPES_TEXT = '{\n\t"skillTypes": [\n\t\t{ "id": "a" }\n\t]\n}\n';
const TREE_TEXT = '{"nodes":[{"id":"n1"}]}\r\n';

let projectRoot: string;
let paths: ProjectPaths;
let app: FastifyInstance;

function writeProjectFile(relativePath: string, content: string | Buffer): string {
  const fullPath = path.join(projectRoot, relativePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content);
  return fullPath;
}

function writePng(relativePath: string, width: number, height: number, rgba: [number, number, number, number]): string {
  const image = new PNG({ width, height });
  for (let offset = 0; offset < image.data.length; offset += 4) {
    image.data.set(rgba, offset);
  }
  return writeProjectFile(relativePath, PNG.sync.write(image));
}

function writeVanillaPng(relativePath: string, rgba: [number, number, number, number]): void {
  const fullPath = path.join(paths.vanillaCoreDir, relativePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  const image = new PNG({ width: 2, height: 2 });
  for (let offset = 0; offset < image.data.length; offset += 4) {
    image.data.set(rgba, offset);
  }
  fs.writeFileSync(fullPath, PNG.sync.write(image));
}

function readPixel(fullPath: string, x: number, y: number): number[] {
  const image = PNG.sync.read(fs.readFileSync(fullPath));
  const offset = (y * image.width + x) * 4;
  return [...image.data.subarray(offset, offset + 4)];
}

beforeEach(() => {
  projectRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'editor-server-'));
  paths = { projectRoot, vanillaCoreDir: path.join(projectRoot, 'vanilla-core') };
  writeProjectFile('data/skilltrees/skill_types.json', Buffer.concat([Buffer.from([0xef, 0xbb, 0xbf]), Buffer.from(TYPES_TEXT)]));
  writeProjectFile('data/skilltrees/ship_skill_tree.json', TREE_TEXT);
  writeProjectFile('data/config/exiledSector/subroutines.csv', 'id,name\r\nx,X\r\n');
  writeProjectFile('tools/editor_vocabulary.json', '{"effects":[]}');
  app = createEditorServer({ paths, port: PORT, describe: new DescribeService(paths, { javaHome: null, maven: null }, Date.now, path.join(projectRoot, 'tooltip-code')) });
});

afterEach(async () => {
  await app.close();
  fs.rmSync(projectRoot, { recursive: true, force: true });
});

describe('editor request check', () => {
  it('refuses changes without the editor header, from another host or from a foreign origin', async () => {
    const payload = { files: {} };
    expect((await app.inject({ method: 'POST', url: '/api/files', payload, headers: { host: `localhost:${PORT}` } })).statusCode).toBe(403);
    expect((await app.inject({ method: 'POST', url: '/api/files', payload, headers: { ...EDITOR_HEADERS, host: 'evil:80' } })).statusCode).toBe(403);
    const foreign = await app.inject({ method: 'POST', url: '/api/files', payload, headers: { ...EDITOR_HEADERS, origin: 'http://evil.test' } });
    expect(foreign.statusCode).toBe(403);
    const allowed = await app.inject({ method: 'POST', url: '/api/files', payload, headers: { ...EDITOR_HEADERS, origin: 'http://LOCALHOST:5173' } });
    expect(allowed.statusCode).toBe(400);
  });
});

describe('data files', () => {
  it('loads files with the byte order mark stripped and hashes the raw bytes', async () => {
    const response = await app.inject({ method: 'GET', url: '/api/files?names=types,tree' });
    const body = response.json();
    expect(body.ok).toBe(true);
    expect(body.files.types.content).toBe(TYPES_TEXT);
    expect(body.files.types.hash).toBe(hashBytes(fs.readFileSync(path.join(projectRoot, 'data/skilltrees/skill_types.json'))));
    expect(body.files.tree.content).toBe(TREE_TEXT);
  });

  it('reports unknown or missing files', async () => {
    expect((await app.inject({ method: 'GET', url: '/api/files?names=nope' })).statusCode).toBe(404);
    expect((await app.inject({ method: 'GET', url: '/api/files?names=commodities' })).statusCode).toBe(404);
  });

  it('saves exactly what was sent, without a byte order mark, and returns the new hashes', async () => {
    const loaded = (await app.inject({ method: 'GET', url: '/api/files?names=types,tree' })).json().files;
    const newTree = '{"nodes":[{"id":"n2"}]}\r\n';
    const response = await app.inject({
      method: 'POST',
      url: '/api/files',
      headers: EDITOR_HEADERS,
      payload: { files: { types: { content: TYPES_TEXT, baseHash: loaded.types.hash }, tree: { content: newTree, baseHash: loaded.tree.hash } } },
    });
    expect(response.statusCode).toBe(200);
    const typesBytes = fs.readFileSync(path.join(projectRoot, 'data/skilltrees/skill_types.json'));
    expect(typesBytes.toString('utf8')).toBe(TYPES_TEXT);
    expect(fs.readFileSync(path.join(projectRoot, 'data/skilltrees/ship_skill_tree.json'), 'utf8')).toBe(newTree);
    expect(response.json().hashes.types).toBe(hashBytes(typesBytes));
    expect(fs.readdirSync(path.join(projectRoot, 'data/skilltrees')).filter((name) => name.endsWith('.tmp'))).toEqual([]);
  });

  it('puts back the files it already replaced when a later file cannot be replaced', () => {
    const firstPath = writeProjectFile('data/first.txt', 'first before');
    const blockedPath = path.join(projectRoot, 'data/blocked');
    fs.mkdirSync(path.join(blockedPath, 'inside'), { recursive: true });
    const newPath = path.join(projectRoot, 'data/new.txt');

    expect(() => writeTextFilesAtomic([
      { filePath: firstPath, content: 'first after' },
      { filePath: newPath, content: 'new after' },
      { filePath: blockedPath, content: 'blocked after' },
    ])).toThrow();

    expect(fs.readFileSync(firstPath, 'utf8')).toBe('first before');
    expect(fs.existsSync(newPath)).toBe(false);
    expect(fs.readdirSync(path.join(projectRoot, 'data')).filter((name) => name.endsWith('.tmp'))).toEqual([]);
  });

  it('writes nothing when any file changed on disk or fails its check', async () => {
    const loaded = (await app.inject({ method: 'GET', url: '/api/files?names=types,tree' })).json().files;
    fs.writeFileSync(path.join(projectRoot, 'data/skilltrees/ship_skill_tree.json'), '{"nodes":[{"id":"other"}]}');
    const conflict = await app.inject({
      method: 'POST',
      url: '/api/files',
      headers: EDITOR_HEADERS,
      payload: { files: { types: { content: TYPES_TEXT.replace('"a"', '"b"'), baseHash: loaded.types.hash }, tree: { content: TREE_TEXT, baseHash: loaded.tree.hash } } },
    });
    expect(conflict.statusCode).toBe(409);
    expect(conflict.json().changed).toEqual(['data/skilltrees/ship_skill_tree.json']);
    expect(fs.readFileSync(path.join(projectRoot, 'data/skilltrees/skill_types.json'), 'utf8')).toContain('"a"');

    const refused = await app.inject({
      method: 'POST',
      url: '/api/files',
      headers: EDITOR_HEADERS,
      payload: { files: { subroutines: { content: 'name,kind\n', baseHash: 'x' } } },
    });
    expect(refused.statusCode).toBe(400);
    expect(refused.json().message).toContain('must start with the id column');
  });

  it('loads and saves the atlas tree files, refusing ones without types or nodes', async () => {
    const atlasTypes = '{\n\t"skillTypes": [\n\t\t{ "id": "atlas_root", "tier": "ROOT" }\n\t]\n}';
    const atlasTree = '{\r\n\t"volumes": [],\r\n\t"nodes": [{ "id": "atlas_root" }]\r\n}';
    writeProjectFile('data/skilltrees/atlas_tree_types.json', atlasTypes);
    writeProjectFile('data/skilltrees/atlas_tree.json', atlasTree);
    const loaded = (await app.inject({ method: 'GET', url: '/api/files?names=atlas-types,atlas-tree' })).json().files;
    expect(loaded['atlas-types'].content).toBe(atlasTypes);
    expect(loaded['atlas-tree'].content).toBe(atlasTree);
    const save = (files: Record<string, string>) =>
      app.inject({
        method: 'POST',
        url: '/api/files',
        headers: EDITOR_HEADERS,
        payload: { files: Object.fromEntries(Object.entries(files).map(([name, content]) => [name, { content, baseHash: loaded[name].hash }])) },
      });

    expect((await save({ 'atlas-tree': '{"volumes":[],"nodes":[]}' })).json().message).toContain('data/skilltrees/atlas_tree.json was refused because its nodes array is empty');
    expect((await save({ 'atlas-types': '{"types":[]}' })).json().message).toContain('it has no skillTypes array');
    const retagged = atlasTree.replace('"volumes": []', '"volumes": ["crafting"]');
    expect((await save({ 'atlas-tree': retagged })).statusCode).toBe(200);
    expect(fs.readFileSync(path.join(projectRoot, 'data/skilltrees/atlas_tree.json'), 'utf8')).toBe(retagged);
    expect(fs.readFileSync(path.join(projectRoot, 'data/skilltrees/ship_skill_tree.json'), 'utf8')).toBe(TREE_TEXT);
  });

  it('saves an English catalogue only when every value is text and none contains a hash', async () => {
    const original = '{\r\n    "stat.armor": "armor"\r\n}\r\n';
    writeProjectFile('data/strings/exiledSector/core/en.json', original);
    const loaded = (await app.inject({ method: 'GET', url: '/api/files?names=strings-en:core' })).json().files['strings-en:core'];
    expect(loaded.content).toBe(original);
    const save = (content: string) =>
      app.inject({ method: 'POST', url: '/api/files', headers: EDITOR_HEADERS, payload: { files: { 'strings-en:core': { content, baseHash: loaded.hash } } } });

    expect((await save('{"stat.armor": "armour #1"}')).json().message).toContain("contains '#'");
    expect((await save('{"stat.armor": 3}')).json().message).toContain('not text');
    const reworded = original.replace('"armor"', '"plating"');
    expect((await save(reworded)).statusCode).toBe(200);
    expect(fs.readFileSync(path.join(projectRoot, 'data/strings/exiledSector/core/en.json'), 'utf8')).toBe(reworded);
  });
});

describe('reserved node ids', () => {
  it('lists replaced ids from node_replacements.csv and released and retired ids, read-only', async () => {
    writeProjectFile('data/config/exiledSector/node_replacements.csv', 'old,new\r\nflux_8,socket_2\r\n\r\nflux_15,socket_1\r\n');
    writeProjectFile('exiledsector-skill-tree/src/test/resources/released_skill_nodes.json', '{\n  "nodes": ["flux_1", "flux_2"],\n  "roots": [],\n  "retired": ["old_flux_3"]\n}\n');

    const response = await app.inject({ method: 'GET', url: '/api/reserved-node-ids' });

    expect(response.json()).toEqual({ ok: true, replaced: ['flux_8', 'flux_15'], released: ['flux_1', 'flux_2', 'old_flux_3'] });
    const save = await app.inject({
      method: 'POST',
      url: '/api/files',
      headers: EDITOR_HEADERS,
      payload: { files: { 'node-replacements': { content: 'old,new\r\n', baseHash: null } } },
    });
    expect(save.statusCode).toBe(400);
  });

  it('returns empty lists when the files are missing', async () => {
    expect((await app.inject({ method: 'GET', url: '/api/reserved-node-ids' })).json()).toEqual({ ok: true, replaced: [], released: [] });
  });
});

describe('images', () => {
  it('lists project and vanilla images and serves them without escaping the roots', async () => {
    writePng('graphics/icons/b.png', 2, 2, [1, 2, 3, 255]);
    writePng('graphics/backgrounds/static_images/s.png', 2, 2, [1, 2, 3, 255]);
    fs.mkdirSync(path.join(paths.vanillaCoreDir, 'graphics/icons/cargo'), { recursive: true });
    const image = new PNG({ width: 1, height: 1 });
    fs.writeFileSync(path.join(paths.vanillaCoreDir, 'graphics/icons/cargo/v.png'), PNG.sync.write(image));
    expect((await app.inject({ method: 'GET', url: '/api/images/static' })).json()).toEqual(['graphics/backgrounds/static_images/s.png']);
    expect((await app.inject({ method: 'GET', url: '/api/images/all' })).json()).toEqual({
      projectImages: ['graphics/backgrounds/static_images/s.png', 'graphics/icons/b.png'],
      vanillaImages: ['graphics/icons/cargo/v.png'],
    });
    expect((await app.inject({ method: 'GET', url: '/files/graphics/icons/cargo/v.png' })).statusCode).toBe(200);
    expect((await app.inject({ method: 'GET', url: '/files/graphics/icons/b.png' })).headers['content-type']).toBe('image/png');
    expect((await app.inject({ method: 'GET', url: '/files/data/skilltrees/skill_types.json' })).statusCode).toBe(404);
    expect((await app.inject({ method: 'GET', url: '/files/..%2F..%2Fsecret.png' })).statusCode).toBe(404);
  });

  it('makes colour-shifted and circular copies and refuses to overwrite unless asked', async () => {
    writePng('graphics/icons/red.png', 8, 8, [255, 0, 0, 255]);
    const payload = { path: 'graphics/icons/red.png', suffix: 'green', hueShift: 120 };
    const shifted = await app.inject({ method: 'POST', url: '/api/images/colorshift', headers: EDITOR_HEADERS, payload });
    expect(shifted.json()).toEqual({ ok: true, path: 'graphics/icons/red_green.png' });
    expect(readPixel(path.join(projectRoot, 'graphics/icons/red_green.png'), 3, 3)).toEqual([0, 255, 0, 255]);
    expect((await app.inject({ method: 'POST', url: '/api/images/colorshift', headers: EDITOR_HEADERS, payload })).statusCode).toBe(409);
    const overwritten = await app.inject({ method: 'POST', url: '/api/images/colorshift', headers: EDITOR_HEADERS, payload: { ...payload, overwrite: true } });
    expect(overwritten.statusCode).toBe(200);
    const badSuffix = await app.inject({ method: 'POST', url: '/api/images/colorshift', headers: EDITOR_HEADERS, payload: { ...payload, suffix: 'a/b' } });
    expect(badSuffix.statusCode).toBe(400);

    const circular = await app.inject({ method: 'POST', url: '/api/images/circular', headers: EDITOR_HEADERS, payload: { path: 'graphics/icons/red.png', suffix: 'round' } });
    expect(circular.json().path).toBe('graphics/unused/circular/red_round.png');
    const circularPath = path.join(projectRoot, 'graphics/unused/circular/red_round.png');
    expect(readPixel(circularPath, 0, 0)[3]).toBe(0);
    expect(readPixel(circularPath, 4, 4)).toEqual([255, 0, 0, 255]);
  });

  it('makes rotated copies next to the original', async () => {
    writePng('graphics/icons/wide.png', 4, 2, [0, 0, 255, 255]);
    const rotated = await app.inject({ method: 'POST', url: '/api/images/rotate', headers: EDITOR_HEADERS, payload: { path: 'graphics/icons/wide.png', suffix: 'turned', degrees: 90 } });
    expect(rotated.json()).toEqual({ ok: true, path: 'graphics/icons/wide_turned.png' });
    const image = PNG.sync.read(fs.readFileSync(path.join(projectRoot, 'graphics/icons/wide_turned.png')));
    expect([image.width, image.height]).toEqual([2, 4]);
    expect((await app.inject({ method: 'POST', url: '/api/images/rotate', headers: EDITOR_HEADERS, payload: { path: 'graphics/icons/wide.png', suffix: 'turned', degrees: 90 } })).statusCode).toBe(409);
  });

  it('copies listed vanilla icons into the mod but never deletes, moves or overrides them', async () => {
    writeVanillaPng('graphics/icons/cargo/red.png', [255, 0, 0, 255]);
    writeVanillaPng('graphics/icons/cargo/red_taken.png', [0, 255, 0, 255]);
    writeVanillaPng('graphics/icons/cargo/shadowed.png', [0, 0, 255, 255]);
    writePng('graphics/icons/cargo/shadowed.png', 2, 2, [9, 9, 9, 255]);
    writeVanillaPng('graphics/ships/unlisted.png', [255, 0, 0, 255]);
    const listing = (await app.inject({ method: 'GET', url: '/api/images/all' })).json();
    expect([...listing.vanillaImages].sort()).toEqual(['graphics/icons/cargo/red.png', 'graphics/icons/cargo/red_taken.png']);
    expect(listing.projectImages).toEqual(['graphics/icons/cargo/shadowed.png']);

    const copy = (payload: Record<string, unknown>) => app.inject({ method: 'POST', url: '/api/images/colorshift', headers: EDITOR_HEADERS, payload });
    const shifted = await copy({ path: 'graphics/icons/cargo/red.png', suffix: 'green', hueShift: 120 });
    expect(shifted.json()).toEqual({ ok: true, path: 'graphics/icons/cargo/red_green.png' });
    expect(readPixel(path.join(projectRoot, 'graphics/icons/cargo/red_green.png'), 0, 0)).toEqual([0, 255, 0, 255]);
    const shadowed = await copy({ path: 'graphics/icons/cargo/shadowed.png', suffix: 'copy', hueShift: 0 });
    expect(readPixel(path.join(projectRoot, shadowed.json().path), 0, 0)).toEqual([9, 9, 9, 255]);
    const overriding = await copy({ path: 'graphics/icons/cargo/red.png', suffix: 'taken', hueShift: 0, overwrite: true });
    expect(overriding.statusCode).toBe(400);
    expect(overriding.json().message).toBe('graphics/icons/cargo/red_taken.png is a vanilla image; a copy there would replace it game-wide.');
    expect(fs.existsSync(path.join(projectRoot, 'graphics/icons/cargo/red_taken.png'))).toBe(false);
    expect((await copy({ path: 'graphics/ships/unlisted.png', suffix: 'copy' })).statusCode).toBe(400);

    const deleted = await app.inject({ method: 'POST', url: '/api/images/delete', headers: EDITOR_HEADERS, payload: { path: 'graphics/icons/cargo/red.png' } });
    expect(deleted.json()).toEqual({ ok: false, message: 'Vanilla images are read-only.' });
    const moved = await app.inject({ method: 'POST', url: '/api/images/move', headers: EDITOR_HEADERS, payload: { from: 'graphics/icons/cargo/red.png', to: 'graphics/icons/red.png' } });
    expect(moved.json()).toEqual({ ok: false, message: 'Vanilla images are read-only.' });
    expect(fs.existsSync(path.join(paths.vanillaCoreDir, 'graphics/icons/cargo/red.png'))).toBe(true);
  });

  it('only copies png images under graphics', async () => {
    writePng('data/config/outside.png', 2, 2, [255, 0, 0, 255]);
    writeProjectFile('graphics/icons/notes.txt', 'not an image');
    for (const url of ['/api/images/colorshift', '/api/images/circular', '/api/images/rotate']) {
      for (const source of ['data/config/outside.png', 'graphics/icons/notes.txt', 'graphics/../data/config/outside.png', '../outside.png']) {
        const refused = await app.inject({ method: 'POST', url, headers: EDITOR_HEADERS, payload: { path: source, suffix: 'copy' } });
        expect(refused.statusCode).toBe(400);
        expect(refused.json().message).toBe('Source image not found.');
      }
    }
    expect(fs.existsSync(path.join(projectRoot, 'data/config/outside_copy.png'))).toBe(false);
    expect(fs.existsSync(path.join(projectRoot, 'graphics/unused/circular'))).toBe(false);
  });

  it('moves images and only deletes pngs under graphics', async () => {
    writePng('graphics/unused/a.png', 1, 1, [0, 0, 0, 255]);
    writePng('graphics/icons/taken.png', 1, 1, [0, 0, 0, 255]);
    const moved = await app.inject({ method: 'POST', url: '/api/images/move', headers: EDITOR_HEADERS, payload: { from: 'graphics/unused/a.png', to: 'graphics/icons/a.png' } });
    expect(moved.json()).toEqual({ ok: true, path: 'graphics/icons/a.png' });
    const clash = await app.inject({ method: 'POST', url: '/api/images/move', headers: EDITOR_HEADERS, payload: { from: 'graphics/icons/a.png', to: 'graphics/icons/taken.png' } });
    expect(clash.statusCode).toBe(400);
    const notImage = await app.inject({ method: 'POST', url: '/api/images/delete', headers: EDITOR_HEADERS, payload: { path: 'data/skilltrees/skill_types.json' } });
    expect(notImage.statusCode).toBe(400);
    const deleted = await app.inject({ method: 'POST', url: '/api/images/delete', headers: EDITOR_HEADERS, payload: { path: 'graphics/icons/a.png' } });
    expect(deleted.statusCode).toBe(200);
    expect(fs.existsSync(path.join(projectRoot, 'graphics/icons/a.png'))).toBe(false);
  });

  it('only moves png images under graphics to png paths under graphics', async () => {
    writePng('graphics/icons/b.png', 1, 1, [0, 0, 0, 255]);
    writePng('data/config/outside.png', 1, 1, [0, 0, 0, 255]);
    const move = (from: string, to: string) => app.inject({ method: 'POST', url: '/api/images/move', headers: EDITOR_HEADERS, payload: { from, to } });

    const fromOutside = await move('data/config/outside.png', 'graphics/icons/outside.png');
    expect(fromOutside.statusCode).toBe(400);
    expect(fromOutside.json().message).toBe('Source image not found.');
    const notImage = await move('data/skilltrees/skill_types.json', 'graphics/icons/types.png');
    expect(notImage.statusCode).toBe(400);
    for (const destination of ['data/config/b.png', 'graphics/icons/b.json', 'graphics/../data/b.png', '../b.png']) {
      const refused = await move('graphics/icons/b.png', destination);
      expect(refused.statusCode).toBe(400);
      expect(refused.json().message).toBe('Images can only be moved to a .png path under graphics/.');
    }
    expect(fs.existsSync(path.join(projectRoot, 'graphics/icons/b.png'))).toBe(true);
    expect(fs.existsSync(path.join(projectRoot, 'data/config/outside.png'))).toBe(true);
    expect(fs.existsSync(path.join(projectRoot, 'data/skilltrees/skill_types.json'))).toBe(true);
  });
});

describe('describe', () => {
  const NO_TOOLCHAIN = { javaHome: null, maven: null };

  function snapshotsDir(): string {
    return path.join(projectRoot, 'tooltip-code');
  }

  function writeClasspathEntries(): string[] {
    const classes = path.join(projectRoot, 'exiledsector-mod', 'target', 'classes');
    fs.mkdirSync(path.join(classes, 'exiledsector'), { recursive: true });
    fs.writeFileSync(path.join(classes, 'exiledsector', 'A.class'), 'compiled');
    const ownJar = writeProjectFile('libs/starfarer.api.jar', 'jar');
    const repositoryJar = writeProjectFile('.m2/repository/org/json/json.jar', 'json');
    return [classes, ownJar, repositoryJar];
  }

  it('reports a clear failure when Java or Maven is missing', async () => {
    const service = new DescribeService(paths, NO_TOOLCHAIN, Date.now, snapshotsDir());
    service.start();
    expect(service.status()).toMatchObject({ build: 'failed', copiedAtMs: null });
    const result = await service.describe('{}');
    expect(result).toEqual({ body: { error: expect.stringContaining('JDK 17 and Maven') } });
  });

  it('waits for another Maven build of the mod instead of starting a second one in the same target folder', async () => {
    writeProjectFile(BUILD_LOCK_FILE, '1234');
    const service = new DescribeService(paths, { javaHome: 'fake-jdk', maven: 'fake-mvn.cmd' }, Date.now, snapshotsDir());
    try {
      expect(service.refresh().build).toBe('waiting');
      expect(service.refresh().build).toBe('waiting');
      const result = await service.describe('{}');
      expect(result).toEqual({ body: { pending: true, message: expect.stringContaining('another Maven build') } });
    } finally {
      service.close();
      fs.rmSync(path.join(paths.projectRoot, BUILD_LOCK_FILE), { force: true });
    }
  });

  it('copies the compiled code so later builds and cleans of the checkout cannot break it', () => {
    const entries = writeClasspathEntries();
    const checkoutDir = checkoutSnapshotsDir(snapshotsDir(), projectRoot);
    const snapshot = copySnapshot(checkoutDir, entries, 1000);
    fs.rmSync(path.join(projectRoot, 'exiledsector-mod'), { recursive: true });
    fs.rmSync(path.join(projectRoot, 'libs'), { recursive: true });
    expect(snapshot.classpath[2]).toBe(entries[2]);
    expect(fs.readFileSync(path.join(snapshot.classpath[0], 'exiledsector', 'A.class'), 'utf8')).toBe('compiled');
    expect(fs.readFileSync(snapshot.classpath[1], 'utf8')).toBe('jar');
    expect(latestSnapshot(checkoutDir)).toEqual(snapshot);
  });

  it('uses the newest complete copy and clears the others once a newer one is in use', () => {
    const entries = writeClasspathEntries();
    const checkoutDir = checkoutSnapshotsDir(snapshotsDir(), projectRoot);
    const older = copySnapshot(checkoutDir, entries, 1000);
    const newer = copySnapshot(checkoutDir, entries, 2000);
    const broken = copySnapshot(checkoutDir, entries, 3000);
    fs.rmSync(broken.classpath[1]);
    expect(latestSnapshot(checkoutDir)?.directory).toBe(newer.directory);
    removeOtherSnapshots(checkoutDir, newer.directory);
    expect(fs.existsSync(older.directory)).toBe(false);
    expect(fs.existsSync(broken.directory)).toBe(false);
    expect(fs.existsSync(newer.directory)).toBe(true);
  });

  it('keeps the copies of different checkouts apart', () => {
    const copiesDir = path.join(projectRoot, 'copies');
    const mainCopies = checkoutSnapshotsDir(copiesDir, path.join(projectRoot, 'ExiledSector'));
    expect(mainCopies).not.toBe(checkoutSnapshotsDir(copiesDir, path.join(projectRoot, 'ExiledSector-parallel-work')));
    expect(mainCopies).toBe(checkoutSnapshotsDir(copiesDir, path.join(projectRoot, 'exiledsector')));
  });

  it('starts from an existing copy without rebuilding', () => {
    copySnapshot(checkoutSnapshotsDir(snapshotsDir(), projectRoot), writeClasspathEntries(), 1234);
    const service = new DescribeService(paths, NO_TOOLCHAIN, Date.now, snapshotsDir());
    service.start();
    expect(service.status()).toEqual({ build: 'idle', message: '', copiedAtMs: 1234, codeRevision: 0 });
  });

  it('only finds the compiled classpath once the helper module and every dependency exist', () => {
    expect(builtClasspath(projectRoot)).toBeNull();
    const [classes, ownJar] = writeClasspathEntries();
    const testClasses = path.join(projectRoot, 'exiledsector-mod', 'target', 'test-classes');
    fs.mkdirSync(testClasses, { recursive: true });
    writeProjectFile('exiledsector-mod/target/editor-describe.classpath', [ownJar, path.join(projectRoot, 'missing.jar')].join(path.delimiter));
    expect(builtClasspath(projectRoot)).toBeNull();
    writeProjectFile('exiledsector-mod/target/editor-describe.classpath', `${ownJar}\n`);
    expect(builtClasspath(projectRoot)).toEqual([classes, testClasses, ownJar]);
  });

  it('reports the tooltip code status and only lets the editor page ask for a refresh', async () => {
    const status = await app.inject({ method: 'GET', url: '/api/describe/status' });
    expect(status.json()).toEqual({ ok: true, build: 'idle', message: '', copiedAtMs: null, codeRevision: 0 });
    const outside = await app.inject({ method: 'POST', url: '/api/describe/refresh', payload: {} });
    expect(outside.statusCode).toBe(403);
    const refreshed = await app.inject({ method: 'POST', url: '/api/describe/refresh', headers: EDITOR_HEADERS, payload: {} });
    expect(refreshed.json()).toMatchObject({ ok: true, build: 'failed', message: expect.stringContaining('JDK 17 and Maven') });
  });

  it('sends every saved English catalogue with a request that has none, and keeps strings a request brings', () => {
    expect(JSON.parse(describeRequestWithStrings(paths, { typeId: 'a' }))).toEqual({ typeId: 'a' });
    writeProjectFile('data/strings/exiledSector/core/en.json', '{"stat.armor": "armor"}');
    writeProjectFile('data/strings/exiledSector/starCharts/en.json', '{"chart.name": "chart"}');
    expect(JSON.parse(describeRequestWithStrings(paths, { typeId: 'a' }))).toEqual({
      typeId: 'a',
      strings: { 'stat.armor': 'armor', 'chart.name': 'chart' },
    });
    const ownStrings = { typeId: 'a', strings: { 'stat.armor': 'plating' } };
    expect(JSON.parse(describeRequestWithStrings(paths, ownStrings))).toEqual(ownStrings);
  });
});
