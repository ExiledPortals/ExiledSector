import { createHash, randomUUID } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { resolveSalvagePath, toProjectRelative, type ProjectPaths } from './paths.ts';
import { stringCatalogueFile } from './stringCatalogues.ts';

export interface DataFile {
  content: string;
  hash: string;
}

export interface DataResource {
  filePath: string;
  check: (text: string) => string | null;
}

const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);

export function hashBytes(bytes: Buffer): string {
  return createHash('sha256').update(bytes).digest('hex');
}

export function fileHash(filePath: string): string {
  return fs.existsSync(filePath) ? hashBytes(fs.readFileSync(filePath)) : '';
}

export function readDataFile(filePath: string): DataFile {
  const bytes = fs.readFileSync(filePath);
  let content = bytes.toString('utf8');
  if (content.startsWith(BYTE_ORDER_MARK)) {
    content = content.slice(1);
  }
  return { content, hash: hashBytes(bytes) };
}

function parseJson(text: string): { parsed: unknown } | null {
  try {
    return { parsed: JSON.parse(text) };
  } catch {
    return null;
  }
}

function checkJsonArrayField(text: string, field: string): string | null {
  if (!text.trim()) {
    return 'it is empty';
  }
  const result = parseJson(text);
  if (!result) {
    return 'it did not parse as JSON';
  }
  const parsed = result.parsed;
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed) || !(field in parsed)) {
    return `it has no ${field} array`;
  }
  const values = (parsed as Record<string, unknown>)[field];
  if (values === null || typeof values === 'string' || !Array.isArray(values) || values.length === 0) {
    return `its ${field} array is empty`;
  }
  return null;
}

function checkJsonObject(text: string): string | null {
  if (!text.trim()) {
    return 'it is empty';
  }
  const result = parseJson(text);
  if (!result) {
    return 'it did not parse as JSON';
  }
  const parsed = result.parsed;
  return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? null : 'it is not a JSON object';
}

function checkStringCatalogue(text: string): string | null {
  const objectRefusal = checkJsonObject(text);
  if (objectRefusal) {
    return objectRefusal;
  }
  if (text.includes('#')) {
    return "it contains '#', which Starsector's JSON loader treats as a comment";
  }
  const values = Object.values(JSON.parse(text) as Record<string, unknown>);
  return values.every((value) => typeof value === 'string') ? null : 'some of its values are not text';
}

function checkCsvHeader(text: string, prefix: string, columns: string): string | null {
  return text.startsWith(prefix) ? null : `it must start with ${columns}`;
}

export function resolveDataResource(paths: ProjectPaths, name: string): DataResource | null {
  const configDir = path.join(paths.projectRoot, 'data', 'config', 'exiledSector');
  const skillTreeDir = path.join(paths.projectRoot, 'data', 'skilltrees');
  switch (name) {
    case 'types':
      return { filePath: path.join(skillTreeDir, 'skill_types.json'), check: (text) => checkJsonArrayField(text, 'skillTypes') };
    case 'tree':
      return { filePath: path.join(skillTreeDir, 'ship_skill_tree.json'), check: (text) => checkJsonArrayField(text, 'nodes') };
    case 'node-radii':
      return { filePath: path.join(configDir, 'node_radii.json'), check: checkJsonObject };
    case 'subroutines':
      return { filePath: path.join(configDir, 'subroutines.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'hull-items':
      return { filePath: path.join(configDir, 'hull_items.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'item-modifiers':
      return { filePath: path.join(configDir, 'item_modifiers.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'item-names':
      return { filePath: path.join(configDir, 'item_names.json'), check: checkJsonObject };
    case 'item-crafting':
      return { filePath: path.join(configDir, 'item_crafting.csv'), check: (text) => checkCsvHeader(text, 'item,', 'the item column') };
    case 'hull-item-implicits':
      return { filePath: path.join(configDir, 'hull_item_implicits.csv'), check: (text) => checkCsvHeader(text, 'section,', 'the section column') };
    case 'subroutine-corruption-implicits':
      return {
        filePath: path.join(configDir, 'subroutine_corruption_implicits.csv'),
        check: (text) => checkCsvHeader(text, 'implicit', 'the implicit column'),
      };
    case 'hull-item-corruption-implicits':
      return {
        filePath: path.join(configDir, 'hull_item_corruption_implicits.csv'),
        check: (text) => checkCsvHeader(text, 'implicit', 'the implicit column'),
      };
    case 'star-charts':
      return { filePath: path.join(configDir, 'star_charts.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'wormhole-anchors':
      return { filePath: path.join(configDir, 'wormhole_anchors.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'doctrine-archives':
      return { filePath: path.join(configDir, 'doctrine_archives.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'star-chart-modifiers':
      return { filePath: path.join(configDir, 'star_chart_modifiers.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'star-chart-implicits':
      return {
        filePath: path.join(configDir, 'star_chart_implicits.csv'),
        check: (text) => checkCsvHeader(text, 'system_type,', 'the system_type column'),
      };
    case 'star-chart-names':
      return { filePath: path.join(configDir, 'star_chart_names.json'), check: checkJsonObject };
    case 'star-chart-tiers':
      return { filePath: path.join(configDir, 'star_chart_tiers.csv'), check: (text) => checkCsvHeader(text, 'tier,', 'the tier column') };
    case 'chart-pois':
      return { filePath: path.join(configDir, 'chart_poi.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'chart-factions':
      return { filePath: path.join(configDir, 'chart_factions.csv'), check: (text) => checkCsvHeader(text, 'faction,', 'the faction column') };
    case 'chart-blocked-abilities':
      return {
        filePath: path.join(configDir, 'chart_blocked_abilities.csv'),
        check: (text) => checkCsvHeader(text, 'ability_id,', 'the ability_id column'),
      };
    case 'chart-drops':
      return { filePath: path.join(configDir, 'chart_drops.csv'), check: (text) => checkCsvHeader(text, 'source,', 'the source column') };
    case 'chart-colony-items':
      return { filePath: path.join(configDir, 'chart_colony_items.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'chart-expansions':
      return { filePath: path.join(configDir, 'chart_expansions.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'chart-loot':
      return { filePath: path.join(configDir, 'chart_loot.csv'), check: (text) => checkCsvHeader(text, 'id,', 'the id column') };
    case 'commodities':
      return {
        filePath: path.join(paths.projectRoot, 'data', 'campaign', 'commodities.csv'),
        check: (text) => checkCsvHeader(text, 'name,id,', 'the name and id columns'),
      };
    case 'special-items':
      return {
        filePath: path.join(paths.projectRoot, 'data', 'campaign', 'special_items.csv'),
        check: (text) => checkCsvHeader(text, 'name,id,', 'the name and id columns'),
      };
    default:
      break;
  }
  const stringCatalogue = stringCatalogueFile(name);
  if (stringCatalogue) {
    return { filePath: path.join(paths.projectRoot, ...stringCatalogue.split('/')), check: checkStringCatalogue };
  }
  if (name.startsWith('salvage:')) {
    const salvagePath = resolveSalvagePath(paths, name.slice('salvage:'.length));
    if (salvagePath) {
      return { filePath: salvagePath, check: (text) => checkCsvHeader(text, 'site,', 'the site column') };
    }
  }
  return null;
}

export interface FileWrite {
  filePath: string;
  content: string;
}

interface ReplacedFile {
  targetPath: string;
  originalBytes: Buffer | null;
}

function restoreReplacedFiles(replaced: ReplacedFile[]): string[] {
  const unrestored: string[] = [];
  for (const file of [...replaced].reverse()) {
    try {
      if (file.originalBytes === null) {
        fs.rmSync(file.targetPath, { force: true });
      } else {
        fs.writeFileSync(file.targetPath, file.originalBytes);
      }
    } catch {
      unrestored.push(file.targetPath);
    }
  }
  return unrestored;
}

export function writeTextFilesAtomic(writes: FileWrite[]): void {
  const staged: { tempPath: string; targetPath: string }[] = [];
  const replaced: ReplacedFile[] = [];
  try {
    for (const write of writes) {
      const targetPath = path.resolve(write.filePath);
      const tempPath = path.join(path.dirname(targetPath), `.${path.basename(targetPath)}.${randomUUID().replace(/-/g, '')}.tmp`);
      staged.push({ tempPath, targetPath });
      fs.writeFileSync(tempPath, write.content, { encoding: 'utf8' });
    }
    for (const file of staged) {
      const originalBytes = fs.existsSync(file.targetPath) ? fs.readFileSync(file.targetPath) : null;
      fs.renameSync(file.tempPath, file.targetPath);
      replaced.push({ targetPath: file.targetPath, originalBytes });
    }
  } catch (error) {
    const unrestored = restoreReplacedFiles(replaced);
    if (unrestored.length > 0) {
      throw new Error(`${String(error)}; these files could not be put back: ${unrestored.join(', ')}`, { cause: error });
    }
    throw error;
  } finally {
    for (const file of staged) {
      if (fs.existsSync(file.tempPath)) {
        fs.rmSync(file.tempPath, { force: true });
      }
    }
  }
}

export type SaveOutcome =
  | { status: 200; body: { ok: true; message: string; hashes: Record<string, string> } }
  | { status: 400; body: { ok: false; message: string } }
  | { status: 409; body: { ok: false; conflict: true; changed: string[]; message: string } };

export function saveDataFiles(paths: ProjectPaths, files: Record<string, { content?: unknown; baseHash?: unknown }>): SaveOutcome {
  const names = Object.keys(files ?? {});
  if (names.length === 0) {
    return { status: 400, body: { ok: false, message: 'Nothing was written - the request named no files.' } };
  }
  const problems: string[] = [];
  const changed: string[] = [];
  const writes: (FileWrite & { name: string })[] = [];
  for (const name of names) {
    const resource = resolveDataResource(paths, name);
    if (!resource) {
      problems.push(`unknown data file ${name}`);
      continue;
    }
    const label = toProjectRelative(paths, resource.filePath);
    const entry = files[name];
    const content = typeof entry?.content === 'string' ? entry.content : '';
    const refusal = resource.check(content);
    if (refusal) {
      problems.push(`${label} was refused because ${refusal}`);
      continue;
    }
    if (typeof entry?.baseHash !== 'string' || entry.baseHash !== fileHash(resource.filePath)) {
      changed.push(label);
      continue;
    }
    writes.push({ name, filePath: resource.filePath, content });
  }
  if (problems.length > 0) {
    return { status: 400, body: { ok: false, message: `Nothing was written - ${problems.join('; ')}.` } };
  }
  if (changed.length > 0) {
    const verb = changed.length === 1 ? 'it' : 'them';
    return {
      status: 409,
      body: {
        ok: false,
        conflict: true,
        changed,
        message: `Nothing was written - ${changed.join(', ')} changed on disk since the editor loaded ${verb}.`,
      },
    };
  }
  writeTextFilesAtomic(writes);
  const hashes: Record<string, string> = {};
  for (const write of writes) {
    hashes[write.name] = fileHash(write.filePath);
  }
  return { status: 200, body: { ok: true, message: 'Saved.', hashes } };
}
