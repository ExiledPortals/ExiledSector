import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..', '..');
const POOL_FOLDER = 'art/atlas_unused';
const CIRCULAR_POOL_FOLDER = 'art/atlas_unused_circular';
const ATLAS_ICON_FOLDER = 'graphics/exiledSector/tree/atlas';
const SOURCE_FOLDERS = ['graphics/exiledSector/tree', 'graphics/exiledSector/items'];
const SKIPPED_FOLDERS = new Set([ATLAS_ICON_FOLDER, 'graphics/exiledSector/items/anchors/crests', 'graphics/exiledSector/items/doctrine_archives/emblems']);
const SKIPPED_FILES = new Set(['graphics/exiledSector/items/doctrine_archives/doctrine_archive_frame.png']);
const ATLAS_TYPES_FILE = 'data/skilltrees/atlas_tree_types.json';

const relative = (fullPath) => path.relative(projectRoot, fullPath).split(path.sep).join('/');

function pngsUnder(folder, found = []) {
  const fullFolder = path.join(projectRoot, folder);
  if (!fs.existsSync(fullFolder)) {
    return found;
  }
  for (const entry of fs.readdirSync(fullFolder, { withFileTypes: true })) {
    const entryPath = `${folder}/${entry.name}`;
    if (entry.isDirectory()) {
      if (!SKIPPED_FOLDERS.has(entryPath)) {
        pngsUnder(entryPath, found);
      }
    } else if (entry.name.toLowerCase().endsWith('.png') && !SKIPPED_FILES.has(entryPath)) {
      found.push(entryPath);
    }
  }
  return found;
}

function namesIn(folder) {
  const fullFolder = path.join(projectRoot, folder);
  return fs.existsSync(fullFolder) ? fs.readdirSync(fullFolder).map((name) => name.toLowerCase()) : [];
}

function contentHash(relativePath) {
  return createHash('sha256').update(fs.readFileSync(path.join(projectRoot, relativePath))).digest('hex');
}

function hashesIn(folder) {
  return namesIn(folder).map((name) => contentHash(`${folder}/${name}`));
}

function atlasTypeIcons() {
  const text = fs.readFileSync(path.join(projectRoot, ATLAS_TYPES_FILE), 'utf8');
  return new Set([...text.matchAll(/"icon"\s*:\s*"([^"]+)"/g)].map((match) => match[1]));
}

const sources = SOURCE_FOLDERS.flatMap((folder) => pngsUnder(folder));
const nameCounts = new Map();
for (const source of sources) {
  const name = path.posix.basename(source).toLowerCase();
  nameCounts.set(name, (nameCounts.get(name) ?? 0) + 1);
}
const usedInAtlas = atlasTypeIcons();
const taken = new Set([...namesIn(POOL_FOLDER), ...namesIn(CIRCULAR_POOL_FOLDER), ...namesIn(ATLAS_ICON_FOLDER)]);
const pooledContent = new Set([POOL_FOLDER, CIRCULAR_POOL_FOLDER, ATLAS_ICON_FOLDER].flatMap(hashesIn));
fs.mkdirSync(path.join(projectRoot, POOL_FOLDER), { recursive: true });
fs.mkdirSync(path.join(projectRoot, CIRCULAR_POOL_FOLDER), { recursive: true });
let copied = 0;
for (const source of sources) {
  if (usedInAtlas.has(source)) {
    continue;
  }
  const baseName = path.posix.basename(source);
  const poolName = nameCounts.get(baseName.toLowerCase()) > 1 ? `${path.posix.basename(path.posix.dirname(source))}_${baseName}` : baseName;
  if (taken.has(poolName.toLowerCase()) || pooledContent.has(contentHash(source))) {
    continue;
  }
  fs.copyFileSync(path.join(projectRoot, source), path.join(projectRoot, POOL_FOLDER, poolName));
  taken.add(poolName.toLowerCase());
  copied++;
}
console.log(`Copied ${copied} of ${sources.length} images into ${relative(path.join(projectRoot, POOL_FOLDER))}`);
