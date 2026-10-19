import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import type { ItemModifier } from './modifiers.ts';

export const DOCTRINE_TYPE = 'doctrine';
export const DOCTRINE_SUBROUTINE_KIND = 'DOCTRINE';
export const DOCTRINE_ARCHIVE_RESOURCE = 'doctrine-archives';
export const DOCTRINE_ARCHIVE_FILE_NAME = 'doctrine_archives.csv';
export const TRANSFORM_BEHAVIOUR = 'TRANSFORM';
export const AUGMENT_BEHAVIOUR = 'AUGMENT';
export const DOCTRINE_ARCHIVE_COLUMNS = ['id', 'behaviour', 'radius', 'notables', 'keystones', 'small_nodes', 'modifiers', 'min_lines', 'max_lines'] as const;
export const DOCTRINE_POOL_TIERS = { notables: 'NOTABLE', keystones: 'KEYSTONE', smallNodes: 'SMALL' } as const;
export type DoctrinePoolKey = keyof typeof DOCTRINE_POOL_TIERS;
const ENTRY_SEPARATOR = ';';
const LIST_JOINER = '; ';
const WHOLE_NUMBER = /^\d{1,4}$/;
const MODIFIER_ENTRY = /^@([^:]+)(?::([^:]*))?$/;

export interface DoctrineArchive {
  cells: string[];
  id: string;
  behaviour: string;
  radius: string;
  notables: string;
  keystones: string;
  smallNodes: string;
  modifiers: string;
  minLines: string;
  maxLines: string;
  loadedId?: string;
}

export interface DoctrineArchivesModel {
  table: CsvTable;
  archives: DoctrineArchive[];
}

export interface DoctrineTypeFacts {
  tier: string;
  optional: boolean;
  itemCost: boolean;
}

export function parseDoctrineArchivesCsv(text: string): DoctrineArchivesModel {
  const table = parseCsvTable(text);
  const archives = table.rows.map((cells): DoctrineArchive => {
    const columns = trimmedCsvCells(table, cells, DOCTRINE_ARCHIVE_COLUMNS);
    return {
      cells,
      id: columns.id,
      behaviour: columns.behaviour,
      radius: columns.radius,
      notables: columns.notables,
      keystones: columns.keystones,
      smallNodes: columns.small_nodes,
      modifiers: columns.modifiers,
      minLines: columns.min_lines,
      maxLines: columns.max_lines,
      loadedId: columns.id,
    };
  });
  return { table, archives };
}

function archiveColumnValues(archive: DoctrineArchive): Record<(typeof DOCTRINE_ARCHIVE_COLUMNS)[number], string> {
  return {
    id: archive.id,
    behaviour: archive.behaviour,
    radius: archive.radius,
    notables: archive.notables,
    keystones: archive.keystones,
    small_nodes: archive.smallNodes,
    modifiers: archive.modifiers,
    min_lines: archive.minLines,
    max_lines: archive.maxLines,
  };
}

export function serializeDoctrineArchivesCsv(model: DoctrineArchivesModel): string {
  return serializeCsvTable(csvTableFromRows(model.table, model.archives, archiveColumnValues));
}

export function listEntries(text: string): string[] {
  return text
    .split(ENTRY_SEPARATOR)
    .map((entry) => entry.trim())
    .filter((entry) => entry !== '');
}

export function joinEntries(entries: readonly string[]): string {
  return entries.join(LIST_JOINER);
}

export function doctrineTypeFacts(typesText: string): Record<string, DoctrineTypeFacts> {
  const types = (JSON.parse(typesText) as { skillTypes?: { id?: string; tier?: string; optionalOptions?: unknown[]; itemCost?: unknown }[] }).skillTypes ?? [];
  const facts: Record<string, DoctrineTypeFacts> = {};
  for (const type of types) {
    facts[String(type.id ?? '')] = {
      tier: String(type.tier ?? 'SMALL'),
      optional: Array.isArray(type.optionalOptions) && type.optionalOptions.length > 0,
      itemCost: type.itemCost !== undefined && type.itemCost !== null,
    };
  }
  return facts;
}

export function replacementCandidates(typeFacts: Readonly<Record<string, DoctrineTypeFacts>>, tier: string): string[] {
  return Object.entries(typeFacts)
    .filter(([, facts]) => facts.tier === tier && !facts.optional && !facts.itemCost)
    .map(([typeId]) => typeId)
    .sort((first, second) => first.localeCompare(second));
}

export function doctrineModifierIds(modifiers: readonly ItemModifier[]): string[] {
  return modifiers.filter((modifier) => modifier.itemTypes.includes(DOCTRINE_TYPE) && modifier.slot === 'prefix').map((modifier) => modifier.id);
}

export function newDoctrineArchive(archives: DoctrineArchive[], doctrineSubroutineIds: readonly string[], vocabulary: Vocabulary): DoctrineArchive {
  const covered = new Set(archives.map((archive) => archive.id));
  const id = doctrineSubroutineIds.find((candidate) => !covered.has(candidate)) ?? '';
  return {
    cells: [],
    id,
    behaviour: vocabulary.doctrineBehaviours[0] ?? TRANSFORM_BEHAVIOUR,
    radius: vocabulary.radiusSizes[1] ?? vocabulary.radiusSizes[0] ?? 'medium',
    notables: '',
    keystones: '',
    smallNodes: '',
    modifiers: '',
    minLines: '1',
    maxLines: '1',
  };
}

export function markDoctrinesSaved(model: Pick<DoctrineArchivesModel, 'archives'>, saved: Pick<DoctrineArchivesModel, 'archives'>): void {
  for (const archive of model.archives) {
    const savedArchive = saved.archives.find((candidate) =>
      archive.loadedId ? candidate.loadedId === archive.loadedId : !candidate.loadedId && candidate.id === archive.id,
    );
    if (savedArchive) {
      archive.loadedId = savedArchive.id;
    }
  }
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
    for (const typeId of listEntries(archive[key])) {
      const facts = Object.hasOwn(typeFacts, typeId) ? typeFacts[typeId] : undefined;
      if (!facts) {
        issues.push(`${label}: ${typeId} is not a skill type`);
      } else if (facts.tier !== tier) {
        issues.push(`${label}: ${typeId} is listed as a ${tier} replacement, but it is a ${facts.tier} node`);
      } else if (facts.optional || facts.itemCost) {
        issues.push(`${label}: ${typeId} is optional or has an item cost, so it can't be a replacement`);
      }
    }
  }
  return issues;
}

function modifierIssues(label: string, archive: DoctrineArchive, modifiers: readonly ItemModifier[]): string[] {
  const issues: string[] = [];
  const seen = new Set<string>();
  for (const entry of listEntries(archive.modifiers)) {
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
    const listedTypes = listEntries(archive.notables).length + listEntries(archive.keystones).length + listEntries(archive.smallNodes).length;
    issues.push(...poolIssues(label, archive, typeFacts));
    if (archive.behaviour === TRANSFORM_BEHAVIOUR) {
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
      if (listEntries(archive.modifiers).length === 0) {
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
  for (const entry of listEntries(archive.modifiers)) {
    const match = MODIFIER_ENTRY.exec(entry);
    if (match) {
      references.push(match[2] === undefined ? { modifierId: match[1].trim() } : { modifierId: match[1].trim(), weight: match[2] });
    }
  }
  return references;
}

export function renameDoctrineModifier(archives: DoctrineArchive[], fromId: string, toId: string): number {
  let renamed = 0;
  for (const archive of archives) {
    const entries = listEntries(archive.modifiers);
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
