import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { createEditorServer } from './app.ts';
import { editorCodeFingerprint, parseWorktreeList, type Checkout } from './checkouts.ts';
import { DescribeService, libsDir } from './describe.ts';
import type { ProjectPaths } from './paths.ts';

const PORT = 8792;
const EDITOR_HEADERS = { 'x-skill-tree-editor': '1', host: `localhost:${PORT}` };

let tempDir: string;
let mainRoot: string;
let branchRoot: string;
let paths: ProjectPaths;
let checkouts: Checkout[];
let app: FastifyInstance;

function writeFile(root: string, relativePath: string, content: string): void {
  const fullPath = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content);
}

function makeCheckout(name: string, treeText: string): string {
  const root = path.join(tempDir, name);
  writeFile(root, 'data/skilltrees/ship_skill_tree.json', treeText);
  writeFile(root, 'tools/editor/src/App.tsx', 'export const app = 1;\n');
  return root;
}

beforeEach(() => {
  tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'editor-checkouts-'));
  mainRoot = makeCheckout('ExiledSector', '{"nodes":[{"id":"main"}]}');
  branchRoot = makeCheckout('ExiledSector-parallel-work', '{"nodes":[{"id":"branch"}]}');
  writeFile(branchRoot, 'tools/editor/src/App.tsx', 'export const app = 2;\n');
  paths = { projectRoot: mainRoot, vanillaCoreDir: path.join(tempDir, 'vanilla-core') };
  checkouts = [
    { root: mainRoot, branch: 'main', main: true },
    { root: branchRoot, branch: 'parallel-work', main: false },
  ];
  app = createEditorServer({
    paths,
    port: PORT,
    describe: new DescribeService(paths, { javaHome: null, maven: null }),
    listCheckouts: () => checkouts,
    editorDir: path.join(mainRoot, 'tools', 'editor'),
  });
});

afterEach(async () => {
  await app.close();
  fs.rmSync(tempDir, { recursive: true, force: true });
});

async function treeNodeId(): Promise<string> {
  const body = (await app.inject({ method: 'GET', url: '/api/files?names=tree' })).json();
  return JSON.parse(body.files.tree.content).nodes[0].id;
}

describe('worktree list', () => {
  it('reads branches and detached heads, marks the first as main and skips bare or prunable entries', () => {
    const porcelain = [
      'worktree E:/Dev/ExiledSector\nHEAD abc\nbranch refs/heads/main',
      'worktree E:/Dev/ExiledSector-parallel-work\nHEAD def\nbranch refs/heads/feature/x',
      'worktree E:/Dev/ExiledSector/.claude/worktrees/probe\nHEAD 123\ndetached',
      'worktree E:/Gone\nHEAD 456\nbranch refs/heads/gone\nprunable gitdir file points to non-existent location',
      '',
    ].join('\n\n');

    expect(parseWorktreeList(porcelain)).toEqual([
      { root: path.resolve('E:/Dev/ExiledSector'), branch: 'main', main: true },
      { root: path.resolve('E:/Dev/ExiledSector-parallel-work'), branch: 'feature/x', main: false },
      { root: path.resolve('E:/Dev/ExiledSector/.claude/worktrees/probe'), branch: null, main: false },
    ]);
  });
});

describe('editor code fingerprint', () => {
  it('ignores line endings and tests but not code changes', () => {
    const editorDir = path.join(mainRoot, 'tools', 'editor');
    const before = editorCodeFingerprint(editorDir);

    writeFile(mainRoot, 'tools/editor/src/App.tsx', 'export const app = 1;\r\n');
    writeFile(mainRoot, 'tools/editor/src/App.test.tsx', 'test');
    writeFile(mainRoot, 'tools/editor/src/test/helper.ts', 'helper');
    expect(editorCodeFingerprint(editorDir)).toBe(before);

    writeFile(mainRoot, 'tools/editor/server/new.ts', 'export {};');
    expect(editorCodeFingerprint(editorDir)).not.toBe(before);
  });
});

describe('checkout switching', () => {
  it('lists the checkouts with the current one and whether their editor code matches', async () => {
    const body = (await app.inject({ method: 'GET', url: '/api/checkouts' })).json();

    expect(body.current).toBe(mainRoot);
    expect(body.checkouts).toEqual([
      { root: mainRoot, branch: 'main', main: true, name: 'ExiledSector', current: true, editorMatches: true },
      { root: branchRoot, branch: 'parallel-work', main: false, name: 'ExiledSector-parallel-work', current: false, editorMatches: false },
    ]);
  });

  it('reads and writes the chosen checkout after a switch', async () => {
    expect(await treeNodeId()).toBe('main');

    const response = await app.inject({ method: 'POST', url: '/api/checkout', payload: { root: branchRoot.toUpperCase() }, headers: EDITOR_HEADERS });

    expect(response.json()).toEqual({ ok: true, current: branchRoot });
    expect(paths.projectRoot).toBe(branchRoot);
    expect(await treeNodeId()).toBe('branch');
  });

  it('refuses folders that are not listed checkouts or have no skill tree data', async () => {
    const outside = await app.inject({ method: 'POST', url: '/api/checkout', payload: { root: tempDir }, headers: EDITOR_HEADERS });
    expect(outside.statusCode).toBe(400);

    fs.rmSync(path.join(branchRoot, 'data'), { recursive: true });
    const empty = await app.inject({ method: 'POST', url: '/api/checkout', payload: { root: branchRoot }, headers: EDITOR_HEADERS });
    expect(empty.statusCode).toBe(400);
    expect(paths.projectRoot).toBe(mainRoot);
  });

  it('refuses changes from a page that loaded another checkout, but still lets it switch', async () => {
    await app.inject({ method: 'POST', url: '/api/checkout', payload: { root: branchRoot }, headers: EDITOR_HEADERS });
    const staleHeaders = { ...EDITOR_HEADERS, 'x-editor-checkout': encodeURIComponent(mainRoot) };

    const save = await app.inject({ method: 'POST', url: '/api/files', payload: { files: {} }, headers: staleHeaders });
    expect(save.statusCode).toBe(412);
    expect(save.json().message).toContain(branchRoot);

    const switchBack = await app.inject({ method: 'POST', url: '/api/checkout', payload: { root: mainRoot }, headers: staleHeaders });
    expect(switchBack.json().ok).toBe(true);
    const currentHeaders = { ...EDITOR_HEADERS, 'x-editor-checkout': encodeURIComponent(mainRoot) };
    expect((await app.inject({ method: 'POST', url: '/api/files', payload: { files: {} }, headers: currentHeaders })).statusCode).toBe(400);
  });

  it('builds against the main checkout libs when a worktree has none', () => {
    writeFile(mainRoot, 'libs/starfarer.api.jar', '');
    const worktreePaths = { ...paths, projectRoot: path.join(mainRoot, '.claude', 'worktrees', 'probe'), mainCheckoutRoot: mainRoot };

    expect(libsDir(worktreePaths)).toBe(path.join(mainRoot, 'libs'));
    writeFile(worktreePaths.projectRoot, 'libs/starfarer.api.jar', '');
    expect(libsDir(worktreePaths)).toBe(path.join(worktreePaths.projectRoot, 'libs'));
  });
});
