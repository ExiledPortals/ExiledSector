import { compareText } from '../../../shared/arrays.ts';
import { withLineEnding, type LineEnding } from '../../../shared/lineEndings.ts';
import { csvCell, emptyCsvTable, parseCsvTable, serializeCsvTable, setCsvCell, type CsvTable } from '../../../shared/csv.ts';
import { isTrueText, type SocketableDefinition } from './definitions.ts';

export interface AffixRow {
  cells: string[];
  effect: string;
  prefix: string;
  suffix: string;
}

export type RareNameStyle = 'codename' | 'product';

export const RARE_NAME_STYLES: { value: RareNameStyle; label: string }[] = [
  { value: 'codename', label: 'Codename - "First Second"' },
  { value: 'product', label: 'Product - "First Second", sometimes a compound first word and a model' },
];

export type RareNameWords = Record<string, unknown> & {
  style?: string;
  first?: string[];
  second?: string[];
  models?: string[];
};

export type RareNameWordList = 'first' | 'second' | 'models';

export const RARE_NAME_WORD_LISTS: { key: RareNameWordList; label: string; productOnly: boolean }[] = [
  { key: 'first', label: 'First words', productOnly: false },
  { key: 'second', label: 'Second words', productOnly: false },
  { key: 'models', label: 'Models', productOnly: true },
];

export function newRareNameWords(): RareNameWords {
  return { style: 'codename', first: [], second: [] };
}

export interface NamesModel {
  affixTable: CsvTable;
  affixes: AffixRow[];
  rareNames: Record<string, RareNameWords>;
  rareNamesLineEnding: LineEnding;
  usedEffects: Record<string, string[]>;
}

export function parseAffixesCsv(text: string): { table: CsvTable; rows: AffixRow[] } {
  const table = parseCsvTable(text);
  return {
    table,
    rows: table.rows.map((cells) => ({
      cells,
      effect: csvCell(table, cells, 'effect').trim(),
      prefix: csvCell(table, cells, 'prefix').trim(),
      suffix: csvCell(table, cells, 'suffix').trim(),
    })),
  };
}

export function serializeAffixesCsv(table: CsvTable, rows: AffixRow[]): string {
  const out = emptyCsvTable(table);
  for (const row of rows) {
    const cells = row.cells.slice();
    setCsvCell(out, cells, 'effect', row.effect);
    setCsvCell(out, cells, 'prefix', row.prefix);
    setCsvCell(out, cells, 'suffix', row.suffix);
    out.rows.push(cells);
  }
  return serializeCsvTable(out);
}

export function wordList(text: string): string[] {
  return String(text ?? '')
    .split(/[\n,]/)
    .map((word) => word.trim())
    .filter(Boolean);
}

export function serializeRareNames(names: Record<string, RareNameWords>, lineEnding: LineEnding = '\n'): string {
  const text =
    '{\n' +
    Object.keys(names)
      .map((socketableId) => {
        const words = names[socketableId];
        const lines = Object.keys(words).map((key) => {
          const value = words[key];
          const text = Array.isArray(value) ? `[${value.map((word) => JSON.stringify(word)).join(', ')}]` : JSON.stringify(value);
          return `\t\t${JSON.stringify(key)}: ${text}`;
        });
        return `\t${JSON.stringify(socketableId)}: {\n${lines.join(',\n')}\n\t}`;
      })
      .join(',\n') +
    '\n}\n';
  return withLineEnding(text, lineEnding);
}

export function usedEffectsOf(definitions: SocketableDefinition[]): Record<string, string[]> {
  const used: Record<string, string[]> = {};
  for (const definition of definitions) {
    if (isTrueText(definition.unique)) {
      continue;
    }
    for (const entry of [...definition.prefixes, ...definition.suffixes]) {
      (used[entry.effect] ??= []).push(definition.id);
    }
  }
  return used;
}

export function missingEffects(model: Pick<NamesModel, 'affixes' | 'usedEffects'>): string[] {
  return Object.keys(model.usedEffects)
    .filter((effect) => !model.affixes.some((row) => row.effect === effect))
    .sort(compareText);
}

export function sampleNames(words: RareNameWords, random: () => number = Math.random): string[] {
  const first = words.first ?? [];
  const second = words.second ?? [];
  const models = words.models ?? [];
  if (first.length === 0 || second.length === 0) {
    return [];
  }
  const pick = (list: string[]) => list[Math.floor(random() * list.length)];
  const samples: string[] = [];
  for (let sample = 0; sample < 5; sample++) {
    let name = pick(first);
    if (words.style === 'product' && first.length > 1 && random() < 0.3) {
      const firstWord = name;
      name += pick(first.filter((word) => word !== firstWord));
    }
    name += ' ' + pick(second);
    if (words.style === 'product' && models.length > 0 && random() < 0.4) {
      name += ' ' + pick(models);
    }
    samples.push(name);
  }
  return samples;
}

export function validateNames(model: NamesModel): string[] {
  const issues: string[] = [];
  const seen = new Set<string>();
  model.affixes.forEach((row, index) => {
    const label = row.effect || `row ${index + 1}`;
    if (!row.effect) {
      issues.push(`${label}: pick an effect`);
    }
    if (seen.has(row.effect)) {
      issues.push(`${label}: listed more than once`);
    }
    seen.add(row.effect);
    if (model.usedEffects[row.effect] && (!row.prefix.trim() || !row.suffix.trim())) {
      issues.push(`${label}: used by socketables, so it needs both a prefix and a suffix`);
    }
  });
  for (const effect of missingEffects(model)) {
    issues.push(`${effect}: used by ${model.usedEffects[effect].join(', ')} but has no names`);
  }
  for (const [socketableId, words] of Object.entries(model.rareNames)) {
    if (!(words.first ?? []).length || !(words.second ?? []).length) {
      issues.push(`${socketableId}: needs at least one first word and one second word`);
    }
  }
  return issues;
}
