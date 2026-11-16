import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..', '..');
const mapFile = path.join(import.meta.dirname, 'graphics_renames.csv');
const referencesOnly = process.argv.includes('--references-only');
const DELETED = 'DELETE';
const SKIPPED_FOLDERS = new Set(['.git', 'node_modules', 'dist', 'coverage', 'target', 'jars', 'libs', 'graphics', 'art', '.idea', '.claude']);
const TEXT_EXTENSIONS = new Set(['.java', '.json', '.csv', '.ts', '.tsx', '.js', '.mjs', '.md', '.bbcode', '.faction', '.txt', '.ps1', '.gitignore', '.xml', '.properties']);

function readRenames() {
  const lines = fs.readFileSync(mapFile, 'utf8').split(/\r?\n/).slice(1).filter((line) => line.trim());
  return lines.map((line) => {
    const [from, to] = line.split(',');
    return { from, to, isFile: /\.[a-z]+$/i.test(from) };
  });
}

function git(...args) {
  execFileSync('git', args, { cwd: projectRoot, stdio: 'inherit' });
}

function moveFiles(renames) {
  const foldersFirst = [...renames.filter(({ isFile }) => !isFile), ...renames.filter(({ isFile }) => isFile)];
  for (const { from, to, isFile } of foldersFirst) {
    const source = path.join(projectRoot, from);
    const movableFolder = !isFile && from.endsWith('/') && fs.existsSync(source) && fs.readdirSync(source).length > 0;
    if (!(isFile ? fs.existsSync(source) : movableFolder)) {
      continue;
    }
    if (to === DELETED) {
      git('rm', '-q', from);
      continue;
    }
    fs.mkdirSync(path.dirname(path.join(projectRoot, to)), { recursive: true });
    git('mv', from.replace(/\/$/, ''), to.replace(/\/$/, ''));
  }
}

function textFiles(directory, found = []) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      if (!SKIPPED_FOLDERS.has(entry.name)) {
        textFiles(fullPath, found);
      }
    } else if (TEXT_EXTENSIONS.has(path.extname(entry.name).toLowerCase()) || entry.name === '.gitignore') {
      found.push(fullPath);
    }
  }
  return found;
}

function referenceRewrites(renames) {
  const kept = renames.filter(({ to }) => to !== DELETED);
  const byLongestSource = (first, second) => second.from.length - first.from.length;
  return [...kept.filter(({ isFile }) => isFile).sort(byLongestSource), ...kept.filter(({ isFile }) => !isFile).sort(byLongestSource)];
}

function rewriteReferences(renames) {
  const rewrites = referenceRewrites(renames);
  const changed = [];
  for (const file of textFiles(projectRoot)) {
    if (path.resolve(file) === path.resolve(mapFile)) {
      continue;
    }
    const original = fs.readFileSync(file, 'utf8');
    let text = original;
    for (const { from, to } of rewrites) {
      if (text.includes(from)) {
        text = text.split(from).join(to);
      }
    }
    if (text !== original) {
      fs.writeFileSync(file, text, 'utf8');
      changed.push(path.relative(projectRoot, file).split(path.sep).join('/'));
    }
  }
  return changed;
}

function removeEmptyFolders(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      removeEmptyFolders(path.join(directory, entry.name));
    }
  }
  if (fs.readdirSync(directory).length === 0) {
    fs.rmdirSync(directory);
  }
}

const renames = readRenames();
if (!referencesOnly) {
  moveFiles(renames);
  removeEmptyFolders(path.join(projectRoot, 'graphics'));
}
const changed = rewriteReferences(renames);
console.log(`Rewrote image references in ${changed.length} files:`);
for (const file of changed) {
  console.log(`  ${file}`);
}
