import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromColumns, parseCsvTable, readCsvRow, serializeCsvTable, splitList, type CsvColumn, type CsvTable } from '../../../shared/csv.ts';
import type { ItemModifier } from './modifiers.ts';

export const DOCTRINE_TYPE = 'doctrine';
export const DOCTRINE_SUBROUTINE_KIND = 'DOCTRINE';
export const DOCTRINE_ARCHIVE_RESOURCE = 'doctrine-archives';
export const DOCTRINE_ARCHIVE_FILE_NAME = 'doctrine_archives.csv';
export const REWRITE_BEHAVIOUR = 'REWRITE';
export const AUGMENT_BEHAVIOUR = 'AUGMENT';
export const DOCTRINE_POOL_TIERS = { notables: 'NOTABLE', keystones: 'KEYSTONE', smallNodes: 'SMALL' } as const;
export type DoctrinePoolKey = keyof typeof DOCTRINE_POOL_TIERS;
const LIST_JOINER = '; ';
const WHOLE_NUMBER = /^\d{1,4}$/;
const CHANCE = /^[0-9]+([.][0-9]+)?$/;
const BOOLEAN_TEXT = ['', 'true', 'false'];
const MODIFIER_ENTRY = /^@([^:]+)(?::([^:]*))?$/;

export interface DoctrineArchive {
  cells: string[];
  id: string;
  behaviour: string;
  radius: string;
  faction: string;
  notables: string;
  keystones: string;
  smallNodes: string;
  modifiers: string;
  minLines: string;
  maxLines: string;
  infestation: string;
  dropFactions: string;
  dropChance: string;
  guaranteedDrop: string;
  loadedId?: string;
}

const DOCTRINE_ARCHIVE_COLUMNS: readonly CsvColumn<DoctrineArchive>[] = [
  { column: 'id', field: 'id' },
  { column: 'behaviour', field: 'behaviour' },
  { column: 'radius', field: 'radius' },
  { column: 'faction', field: 'faction' },
  { column: 'notables', field: 'notables' },
  { column: 'keystones', field: 'keystones' },
  { column: 'small_nodes', field: 'smallNodes' },
  { column: 'modifiers', field: 'modifiers' },
  { column: 'min_lines', field: 'minLines' },
  { column: 'max_lines', field: 'maxLines' },
  { column: 'infestation', field: 'infestation' },
  { column: 'drop_factions', field: 'dropFactions' },
  { column: 'drop_chance', field: 'dropChance' },
  { column: 'guaranteed_drop', field: 'guaranteedDrop' },
];

export interface DoctrineArchivesModel {
  table: CsvTable;
  archives: DoctrineArchive[];
}

export interface DoctrineTypeFacts {
  tier: string;
  optional: boolean;
}

export function parseDoctrineArchivesCsv(text: string): DoctrineArchivesModel {
  const table = parseCsvTable(text);
  const archives = table.rows.map((cells): DoctrineArchive => {
    const archive = { cells, ...readCsvRow(table, cells, DOCTRINE_ARCHIVE_COLUMNS) } as DoctrineArchive;
    return { ...archive, loadedId: archive.id };
  });
  return { table, archives };
}

export function serializeDoctrineArchivesCsv(model: DoctrineArchivesModel): string {
  return serializeCsvTable(csvTableFromColumns(model.table, model.archives, DOCTRINE_ARCHIVE_COLUMNS));
}

export function joinEntries(entries: readonly string[]): string {
  return entries.join(LIST_JOINER);
}

export function doctrineTypeFacts(typesText: string): Record<string, DoctrineTypeFacts> {
  const types = (JSON.parse(typesText) as { skillTypes?: { id?: string; tier?: string; options?: unknown[] }[] }).skillTypes ?? [];
  const facts: Record<string, DoctrineTypeFacts> = {};
  for (const type of types) {
    facts[String(type.id ?? '')] = {
      tier: String(type.tier ?? 'SMALL'),
      optional: Array.isArray(type.options) && type.options.length > 0,
    };
  }
  return facts;
}

export function replacementCandidates(typeFacts: Readonly<Record<string, DoctrineTypeFacts>>, tier: string): string[] {
  return Object.entries(typeFacts)
    .filter(([, facts]) => facts.tier === tier && !facts.optional)
    .map(([typeId]) => typeId)
    .sort((first, second) => first.localeCompare(second));
}

export function newDoctrineArchive(archives: DoctrineArchive[], doctrineSubroutineIds: readonly string[], vocabulary: Vocabulary): DoctrineArchive {
  const covered = new Set(archives.map((archive) => archive.id));
  const id = doctrineSubroutineIds.find((candidate) => !covered.has(candidate)) ?? '';
  return {
    cells: [],
    id,
    behaviour: vocabulary.doctrineBehaviours[0] ?? REWRITE_BEHAVIOUR,
    radius: vocabulary.radiusSizes[1] ?? vocabulary.radiusSizes[0] ?? 'medium',
    faction: '',
    notables: '',
    keystones: '',
    smallNodes: '',
    modifiers: '',
    minLines: '1',
    maxLines: '1',
    infestation: '',
    dropFactions: '',
    dropChance: '',
    guaranteedDrop: '',
  };
}

function lineCount(text: string): number | null {
  if (text === '') {
    return 1;
  }
  return WHOLE_NUMBER.test(text) ? Number.parseInt(text, 10) : null;
}

function poolIssues(label: string, archive: DoctrineArchive, typeFacts: Readonly<Record<string, DoctrineTypeFacts>>): string[] {
  const issues: string[] = [];
  for (const [key, tier] of Object.entries(DOCTRINE_POOL_TIERS) as [DoctrinePoolKey, string][]) {
    for (const typeId of splitList(archive[key])) {
      const facts = Object.hasOwn(typeFacts, typeId) ? typeFacts[typeId] : undefined;
      if (!facts) {
        issues.push(`${label}: ${typeId} is not a skill type`);
      } else if (facts.tier !== tier) {
        issues.push(`${label}: ${typeId} is listed as a ${tier} replacement, but it is a ${facts.tier} node`);
      } else if (facts.optional) {
        issues.push(`${label}: ${typeId} is optional, so it can't be a replacement`);
      }
    }
  }
  return issues;
}

function dropIssues(label: string, archive: DoctrineArchive, vocabulary: Vocabulary): string[] {
  const issues: string[] = [];
  if (archive.infestation !== '' && !vocabulary.doctrineInfestations.includes(archive.infestation)) {
    issues.push(`${label}: infestation must be blank or one of ${vocabulary.doctrineInfestations.join(', ')}`);
  }
  if (archive.dropChance !== '' && !CHANCE.test(archive.dropChance)) {
    issues.push(`${label}: drop chance must be a number of 0 or more`);
  }
  if (!BOOLEAN_TEXT.includes(archive.guaranteedDrop.toLowerCase())) {
    issues.push(`${label}: guaranteed drop must be true, false or blank`);
  }
  if (splitList(archive.dropFactions).length === 0 && (archive.dropChance !== '' || archive.guaranteedDrop.toLowerCase() === 'true')) {
    issues.push(`${label}: a fleet drop needs at least one drop faction`);
  }
  return issues;
}

function modifierIssues(label: string, archive: DoctrineArchive, modifiers: readonly ItemModifier[]): string[] {
  const issues: string[] = [];
  const seen = new Set<string>();
  for (const entry of splitList(archive.modifiers)) {
    const match = MODIFIER_ENTRY.exec(entry);
    if (!match) {
      issues.push(`${label}: ${entry} is not @ID or @ID:weight`);
      continue;
    }
    const modifierId = match[1].trim();
    const weight = match[2];
    if (weight !== undefined && !(Number.parseFloat(weight) > 0)) {
      issues.push(`${label}: ${entry} needs a weight above zero`);
    }
    if (seen.has(modifierId)) {
      issues.push(`${label}: ${modifierId} is listed more than once`);
    }
    seen.add(modifierId);
    const modifier = modifiers.find((candidate) => candidate.id === modifierId);
    if (!modifier) {
      issues.push(`${label}: ${modifierId} is not in item_modifiers.csv`);
    } else if (!modifier.itemTypes.includes(DOCTRINE_TYPE)) {
      issues.push(`${label}: ${modifierId} does not list ${DOCTRINE_TYPE} in its item types`);
    } else if (modifier.slot !== 'prefix') {
      issues.push(`${label}: ${modifierId} must be a prefix to roll on a doctrine`);
    }
  }
  return issues;
}

export function validateDoctrineArchives(
  archives: DoctrineArchive[],
  vocabulary: Vocabulary,
  doctrineSubroutineIds: readonly string[],
  typeFacts: Readonly<Record<string, DoctrineTypeFacts>>,
  modifiers: readonly ItemModifier[],
): string[] {
  const issues: string[] = [];
  const seenIds = new Set<string>();
  archives.forEach((archive, index) => {
    const label = archive.id || `row ${index + 1}`;
    if (seenIds.has(archive.id)) {
      issues.push(`${label}: id is used more than once`);
    }
    seenIds.add(archive.id);
    if (!doctrineSubroutineIds.includes(archive.id)) {
      issues.push(`${label}: id must be a ${DOCTRINE_SUBROUTINE_KIND} subroutine in subroutines.csv`);
    }
    if (!vocabulary.doctrineBehaviours.includes(archive.behaviour)) {
      issues.push(`${label}: behaviour must be one of ${vocabulary.doctrineBehaviours.join(', ')}`);
    }
    if (!vocabulary.radiusSizes.includes(archive.radius)) {
      issues.push(`${label}: radius must be one of ${vocabulary.radiusSizes.join(', ')}`);
    }
    const minLines = lineCount(archive.minLines);
    const maxLines = lineCount(archive.maxLines);
    if (minLines === null || maxLines === null || minLines < 1 || maxLines < minLines) {
      issues.push(`${label}: min lines must be 1 or more and max lines at least min lines`);
    }
    const listedTypes = splitList(archive.notables).length + splitList(archive.keystones).length + splitList(archive.smallNodes).length;
    issues.push(...poolIssues(label, archive, typeFacts));
    issues.push(...dropIssues(label, archive, vocabulary));
    if (archive.behaviour === REWRITE_BEHAVIOUR) {
      if (listedTypes === 0) {
        issues.push(`${label}: a transforming doctrine needs at least one replacement notable, keystone or small node`);
      }
      if (archive.modifiers !== '') {
        issues.push(`${label}: a transforming doctrine lists no modifiers`);
      }
    } else if (archive.behaviour === AUGMENT_BEHAVIOUR) {
      if (listedTypes > 0) {
        issues.push(`${label}: an augmenting doctrine lists no replacement nodes`);
      }
      if (splitList(archive.modifiers).length === 0) {
        issues.push(`${label}: an augmenting doctrine needs at least one modifier`);
      }
      issues.push(...modifierIssues(label, archive, modifiers));
    }
  });
  return issues;
}

export interface DoctrineModifierReference {
  modifierId: string;
  weight?: string;
}

export function doctrineModifierReferences(archive: Pick<DoctrineArchive, 'modifiers'>): DoctrineModifierReference[] {
  const references: DoctrineModifierReference[] = [];
  for (const entry of splitList(archive.modifiers)) {
    const match = MODIFIER_ENTRY.exec(entry);
    if (match) {
      references.push(match[2] === undefined ? { modifierId: match[1].trim() } : { modifierId: match[1].trim(), weight: match[2] });
    }
  }
  return references;
}

export function formatDoctrineModifiers(references: readonly DoctrineModifierReference[]): string {
  return joinEntries(references.map((reference) => (reference.weight === undefined || reference.weight === '' ? `@${reference.modifierId}` : `@${reference.modifierId}:${reference.weight}`)));
}

export function renameDoctrineModifier(archives: DoctrineArchive[], fromId: string, toId: string): number {
  let renamed = 0;
  for (const archive of archives) {
    const entries = splitList(archive.modifiers);
    let changed = false;
    const renamedEntries = entries.map((entry) => {
      const match = MODIFIER_ENTRY.exec(entry);
      if (!match || match[1].trim() !== fromId) {
        return entry;
      }
      changed = true;
      renamed++;
      return match[2] === undefined ? `@${toId}` : `@${toId}:${match[2]}`;
    });
    if (changed) {
      archive.modifiers = joinEntries(renamedEntries);
    }
  }
  return renamed;
}

export function renameDoctrineArchive(archives: DoctrineArchive[], fromId: string, toId: string): number {
  let renamed = 0;
  for (const archive of archives) {
    if (archive.id === fromId) {
      archive.id = toId;
      renamed++;
    }
  }
  return renamed;
}

export function doctrineReferenceIssues(archives: readonly DoctrineArchive[], catalogue: Map<string, ItemModifier>): string[] {
  const issues: string[] = [];
  for (const archive of archives) {
    for (const reference of doctrineModifierReferences(archive)) {
      const modifier = catalogue.get(reference.modifierId);
      if (!modifier) {
        issues.push(`${archive.id} in ${DOCTRINE_ARCHIVE_FILE_NAME} lists ${reference.modifierId}, which is not a modifier`);
      } else if (modifier.slot !== 'prefix') {
        issues.push(`${archive.id} in ${DOCTRINE_ARCHIVE_FILE_NAME} lists ${modifier.id}, but doctrine lines are prefixes and it is a ${modifier.slot}`);
      } else if (!modifier.itemTypes.includes(DOCTRINE_TYPE)) {
        issues.push(`${archive.id} in ${DOCTRINE_ARCHIVE_FILE_NAME} lists ${modifier.id}, which does not list ${DOCTRINE_TYPE} in its item types`);
      }
    }
  }
  return issues;
}

export function uncoveredDoctrines(archives: DoctrineArchive[], doctrineSubroutineIds: readonly string[]): string[] {
  const covered = new Set(archives.map((archive) => archive.id));
  return doctrineSubroutineIds.filter((doctrineId) => !covered.has(doctrineId));
}
