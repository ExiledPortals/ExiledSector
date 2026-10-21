import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

export interface Checkout {
  root: string;
  branch: string | null;
  main: boolean;
}

export type CheckoutLister = () => Checkout[];

const EDITOR_CODE_DIRS = ['src', 'server'];
const TEST_FILE = /\.test\.tsx?$/;

export function parseWorktreeList(porcelain: string): Checkout[] {
  const checkouts: Checkout[] = [];
  for (const block of porcelain.split(/\r?\n\r?\n/)) {
    const lines = block.split(/\r?\n/).filter((line) => line.length > 0);
    const worktreeLine = lines.find((line) => line.startsWith('worktree '));
    if (!worktreeLine || lines.some((line) => line === 'bare' || line.startsWith('prunable'))) {
      continue;
    }
    const branchLine = lines.find((line) => line.startsWith('branch '));
    checkouts.push({
      root: path.resolve(worktreeLine.slice('worktree '.length)),
      branch: branchLine ? branchLine.slice('branch '.length).replace(/^refs\/heads\//, '') : null,
      main: checkouts.length === 0,
    });
  }
  return checkouts;
}

export function gitCheckoutLister(repositoryDir: string): CheckoutLister {
  return () => {
    try {
      const porcelain = execFileSync('git', ['worktree', 'list', '--porcelain'], { cwd: repositoryDir, encoding: 'utf8', windowsHide: true });
      return parseWorktreeList(porcelain).filter((checkout) => fs.existsSync(checkout.root));
    } catch {
      return [{ root: path.resolve(repositoryDir), branch: null, main: true }];
    }
  };
}

export function sameRoot(first: string, second: string): boolean {
  return path.resolve(first).toLowerCase() === path.resolve(second).toLowerCase();
}

function listCodeFiles(directory: string, files: string[]): void {
  if (!fs.existsSync(directory)) {
    return;
  }
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      if (entry.name !== 'test') {
        listCodeFiles(fullPath, files);
      }
    } else if (/\.tsx?$/.test(entry.name) && !TEST_FILE.test(entry.name)) {
      files.push(fullPath);
    }
  }
}

export function editorCodeFingerprint(editorDir: string): string {
  const hash = crypto.createHash('sha1');
  for (const codeDir of EDITOR_CODE_DIRS) {
    const files: string[] = [];
    listCodeFiles(path.join(editorDir, codeDir), files);
    for (const file of files.sort((first, second) => first.localeCompare(second))) {
      hash.update(path.relative(editorDir, file).split(path.sep).join('/'));
      hash.update('\0');
      hash.update(fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n'));
      hash.update('\0');
    }
  }
  return hash.digest('hex');
}
