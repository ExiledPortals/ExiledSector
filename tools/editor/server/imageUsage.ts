import fs from 'node:fs';
import path from 'node:path';
import { ATLAS_TREE_DATA_PREFIX } from '../src/shared/imageFolders.ts';
import type { ProjectPaths } from './paths.ts';
import { parseStarsectorJson } from './starsectorJson.ts';

export interface ImageUse {
  file: string;
  detail: string;
  atlas: boolean;
}

export type ImageUsage = Record<string, ImageUse[]>;

const DATA_EXTENSIONS = new Set(['.json', '.csv', '.faction']);
const SKIPPED_FOLDERS = new Set(['node_modules', 'target', '.git']);
const TYPE_FILES = new Set(['data/skilltrees/skill_types.json', 'data/skilltrees/atlas_tree_types.json']);
const IMAGE_REFERENCE = /\b(?:graphics|art)\/[A-Za-z0-9_\-./]+/g;
const IMAGE_FILE = /\.(png|jpg)$/i;
const PREFIX_ENDING = /[/_]$/;

interface ReferenceTarget {
  reference: string;
  prefix: boolean;
}

interface TypeIconEntry {
  id?: unknown;
  name?: unknown;
  icon?: unknown;
}

function sourceFiles(directory: string, keep: (fileName: string) => boolean, found: string[] = []): string[] {
  if (!fs.existsSync(directory)) {
    return found;
  }
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      if (!SKIPPED_FOLDERS.has(entry.name)) {
        sourceFiles(fullPath, keep, found);
      }
    } else if (keep(entry.name)) {
      found.push(fullPath);
    }
  }
  return found;
}

function scannedFiles(paths: ProjectPaths): string[] {
  const dataFiles = sourceFiles(path.join(paths.projectRoot, 'data'), (name) => DATA_EXTENSIONS.has(path.extname(name).toLowerCase()));
  const moduleFolders = fs.existsSync(paths.projectRoot)
    ? fs.readdirSync(paths.projectRoot).filter((name) => name.startsWith('exiledsector-')).map((name) => path.join(paths.projectRoot, name, 'src', 'main', 'java'))
    : [];
  const javaFiles = moduleFolders.flatMap((folder) => sourceFiles(folder, (name) => name.endsWith('.java')));
  return [...dataFiles, ...javaFiles];
}

function referencesIn(line: string, java: boolean): ReferenceTarget[] {
  const targets: ReferenceTarget[] = [];
  for (const match of line.matchAll(IMAGE_REFERENCE)) {
    const reference = match[0];
    if (IMAGE_FILE.test(reference)) {
      targets.push({ reference, prefix: false });
    } else if (java && PREFIX_ENDING.test(reference) && line.charAt((match.index ?? 0) + reference.length) === '"') {
      targets.push({ reference, prefix: true });
    }
  }
  return targets;
}

function lineDetail(relativeFile: string, line: string): string {
  if (relativeFile.endsWith('.csv')) {
    return line.split(',')[0].replace(/^"/, '').trim();
  }
  if (relativeFile.endsWith('.java')) {
    return path.basename(relativeFile, '.java');
  }
  return '';
}

function typeIconUses(relativeFile: string, text: string): { icon: string; use: ImageUse }[] {
  const parsed = parseStarsectorJson(text) as { skillTypes?: TypeIconEntry[] };
  const atlas = relativeFile.startsWith(ATLAS_TREE_DATA_PREFIX);
  return (parsed.skillTypes ?? [])
    .filter((type) => typeof type.icon === 'string' && type.icon)
    .map((type) => ({
      icon: String(type.icon),
      use: { file: relativeFile, detail: `${String(type.name ?? type.id)} (${String(type.id)})`, atlas },
    }));
}

function addUse(usage: ImageUsage, imagePath: string, use: ImageUse): void {
  const uses = usage[imagePath] ?? (usage[imagePath] = []);
  if (!uses.some((existing) => existing.file === use.file && existing.detail === use.detail)) {
    uses.push(use);
  }
}

export function scanImageUsage(paths: ProjectPaths, knownImages: string[]): ImageUsage {
  const usage: ImageUsage = {};
  const known = new Map(knownImages.map((imagePath) => [imagePath.toLowerCase(), imagePath]));
  const canonical = (reference: string) => known.get(reference.toLowerCase()) ?? reference;
  for (const fullPath of scannedFiles(paths)) {
    const relativeFile = path.relative(paths.projectRoot, fullPath).split(path.sep).join('/');
    const text = fs.readFileSync(fullPath, 'utf8');
    if (TYPE_FILES.has(relativeFile)) {
      for (const { icon, use } of typeIconUses(relativeFile, text)) {
        addUse(usage, canonical(icon), use);
      }
      continue;
    }
    const java = relativeFile.endsWith('.java');
    const atlas = relativeFile.startsWith(ATLAS_TREE_DATA_PREFIX);
    for (const line of text.split(/\r?\n/)) {
      for (const { reference, prefix } of referencesIn(line, java)) {
        const use = { file: relativeFile, detail: lineDetail(relativeFile, line), atlas };
        if (prefix) {
          knownImages.filter((imagePath) => imagePath.startsWith(reference)).forEach((imagePath) => addUse(usage, imagePath, use));
        } else {
          addUse(usage, canonical(reference), use);
        }
      }
    }
  }
  return usage;
}
