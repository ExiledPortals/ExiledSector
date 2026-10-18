import { withLineEnding, type LineEnding } from '../../../shared/lineEndings.ts';

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
  rareNames: Record<string, RareNameWords>;
  rareNamesLineEnding: LineEnding;
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
      .map((hullItemId) => {
        const words = names[hullItemId];
        const lines = Object.keys(words).map((key) => {
          const value = words[key];
          const text = Array.isArray(value) ? `[${value.map((word) => JSON.stringify(word)).join(', ')}]` : JSON.stringify(value);
          return `\t\t${JSON.stringify(key)}: ${text}`;
        });
        return `\t${JSON.stringify(hullItemId)}: {\n${lines.join(',\n')}\n\t}`;
      })
      .join(',\n') +
    '\n}\n';
  return withLineEnding(text, lineEnding);
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
  for (const [hullItemId, words] of Object.entries(model.rareNames)) {
    if (!(words.first ?? []).length || !(words.second ?? []).length) {
      issues.push(`${hullItemId}: needs at least one first word and one second word`);
    }
  }
  return issues;
}
