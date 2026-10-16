import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { PNG } from 'pngjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { createEditorServer } from './app.ts';
import { hashBytes, writeTextFilesAtomic } from './dataFiles.ts';
import { DescribeService, describeRequestWithStrings } from './describe.ts';
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
  writeProjectFile('data/config/exiledSector/socketables.csv', 'id,kind\r\nx,subroutine\r\n');
  writeProjectFile('tools/editor_vocabulary.json', '{"effects":[]}');
  app = createEditorServer({ paths, port: PORT, describe: new DescribeService(paths, { javaHome: null, maven: null }) });
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
    expect((await app.inject({ method: 'GET', url: '/api/files?names=salvage:../escape.csv' })).statusCode).toBe(404);
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
      payload: { files: { socketables: { content: 'name,kind\n', baseHash: 'x' } } },
    });
    expect(refused.statusCode).toBe(400);
    expect(refused.json().message).toContain('must start with the id column');
  });

  it('saves the English strings only when every value is text and none contains a hash', async () => {
    const original = '{\r\n    "stat.armor": "armor"\r\n}\r\n';
    writeProjectFile('data/strings/exiledSector/en.json', original);
    const loaded = (await app.inject({ method: 'GET', url: '/api/files?names=strings-en' })).json().files['strings-en'];
    expect(loaded.content).toBe(original);
    const save = (content: string) =>
      app.inject({ method: 'POST', url: '/api/files', headers: EDITOR_HEADERS, payload: { files: { 'strings-en': { content, baseHash: loaded.hash } } } });

    expect((await save('{"stat.armor": "armour #1"}')).json().message).toContain("contains '#'");
    expect((await save('{"stat.armor": 3}')).json().message).toContain('not text');
    const reworded = original.replace('"armor"', '"plating"');
    expect((await save(reworded)).statusCode).toBe(200);
    expect(fs.readFileSync(path.join(projectRoot, 'data/strings/exiledSector/en.json'), 'utf8')).toBe(reworded);
  });

  it('lists salvage files with the default first', async () => {
    writeProjectFile('data/config/exiledSector/compat/salvage/zzz.csv', 'site,chances\n');
    writeProjectFile('data/config/exiledSector/compat/salvage/aaa.csv', 'site,chances\n');
    expect((await app.inject({ method: 'GET', url: '/api/salvage-files' })).json()).toEqual([
      'socketable_salvage.csv',
      'compat/salvage/aaa.csv',
      'compat/salvage/zzz.csv',
    ]);
  });
});

describe('reserved node ids', () => {
  it('lists retired ids from node_replacements.csv and released ids, read-only', async () => {
    writeProjectFile('data/config/exiledSector/node_replacements.csv', 'old,new\r\nflux_8,socket_2\r\n\r\nflux_15,socket_1\r\n');
    writeProjectFile('src/test/resources/released_skill_nodes.json', '{\n  "nodes": ["flux_1", "flux_2"],\n  "roots": []\n}\n');

    const response = await app.inject({ method: 'GET', url: '/api/reserved-node-ids' });

    expect(response.json()).toEqual({ ok: true, replaced: ['flux_8', 'flux_15'], released: ['flux_1', 'flux_2'] });
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
    expect((await app.inject({ method: 'GET', url: '/api/images/all' })).json()).toEqual([
      'graphics/backgrounds/static_images/s.png',
      'graphics/icons/b.png',
      'graphics/icons/cargo/v.png',
    ]);
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
});

describe('describe', () => {
  it('reports a clear failure when Java or Maven is missing', async () => {
    const service = new DescribeService(paths, { javaHome: null, maven: null });
    service.startBuild();
    const result = await service.describe('{}');
    expect(result).toEqual({ body: { pending: false, message: expect.stringContaining('JDK 17 and Maven') } });
  });

  it('sends the saved English strings with a request that has none, and keeps strings a request brings', () => {
    expect(JSON.parse(describeRequestWithStrings(paths, { typeId: 'a' }))).toEqual({ typeId: 'a' });
    writeProjectFile('data/strings/exiledSector/en.json', '{"stat.armor": "armor"}');
    expect(JSON.parse(describeRequestWithStrings(paths, { typeId: 'a' }))).toEqual({ typeId: 'a', strings: { 'stat.armor': 'armor' } });
    const ownStrings = { typeId: 'a', strings: { 'stat.armor': 'plating' } };
    expect(JSON.parse(describeRequestWithStrings(paths, ownStrings))).toEqual(ownStrings);
  });
});
