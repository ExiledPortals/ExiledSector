import { lineEndingOf, type LineEnding } from '../../../shared/lineEndings.ts';

export interface StringEntry {
  key: string;
  value: string;
  savedValue: string;
  lineIndex: number;
}

export interface StringsModel {
  lines: string[];
  lineEnding: LineEnding;
  entries: StringEntry[];
}

export type TemplateSegment = { kind: 'text'; text: string; style: string | null } | { kind: 'placeholder'; name: string; style: string | null };

export interface TemplateAnalysis {
  segments: TemplateSegment[];
  placeholders: string[];
  balancedTags: boolean;
  unknownTags: string[];
}

export interface StringIssues {
  errors: string[];
  warnings: string[];
}

export const TEMPLATE_STYLES = ['good', 'bad', 'hl', 'hullmod', 'node'] as const;

const ENTRY_LINE = /^(\s*"(?:[^"\\]|\\.)*"\s*:\s*)("(?:[^"\\]|\\.)*")(,?\s*)$/;
const IDENTIFIER = /^[A-Za-z][A-Za-z0-9_]*$/;
const TAG_LIKE = /^\/?[A-Za-z]+$/;
const UNSPACED_SU = /[0-9}]su(?![A-Za-z])/;

export function parseStrings(text: string): StringsModel {
  const lineEnding = lineEndingOf(text);
  const lines = text.split(lineEnding);
  const entries: StringEntry[] = [];
  lines.forEach((line, lineIndex) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed === '{' || trimmed === '}') {
      return;
    }
    const match = ENTRY_LINE.exec(line);
    if (!match) {
      throw new Error(`line ${lineIndex + 1} is not a single "key": "text" entry`);
    }
    const key = JSON.parse(match[1].trim().slice(0, -1)) as string;
    const value = JSON.parse(match[2]) as string;
    entries.push({ key, value, savedValue: value, lineIndex });
  });
  const parsedCount = Object.keys(JSON.parse(text) as Record<string, unknown>).length;
  if (parsedCount !== entries.length) {
    throw new Error(`found ${entries.length} entry lines but the file has ${parsedCount} keys`);
  }
  return { lines, lineEnding, entries };
}

export function serializeStrings(model: StringsModel): string {
  const lines = [...model.lines];
  for (const entry of model.entries) {
    if (entry.value === entry.savedValue) {
      continue;
    }
    const match = ENTRY_LINE.exec(lines[entry.lineIndex]);
    if (match) {
      lines[entry.lineIndex] = match[1] + JSON.stringify(entry.value) + match[3];
    }
  }
  return lines.join(model.lineEnding);
}

export function markStringsSaved(model: StringsModel, saved: StringsModel): void {
  model.lines = serializeStrings(saved).split(saved.lineEnding);
  const savedValues = new Map(saved.entries.map((entry) => [entry.key, entry.value]));
  for (const entry of model.entries) {
    entry.savedValue = savedValues.get(entry.key) ?? entry.savedValue;
  }
}

export function currentStrings(model: StringsModel): Record<string, string> {
  return Object.fromEntries(model.entries.map((entry) => [entry.key, entry.value]));
}

export function editedEntries(model: StringsModel): StringEntry[] {
  return model.entries.filter((entry) => entry.value !== entry.savedValue);
}

export function keyGroup(key: string): string {
  const dot = key.indexOf('.');
  return dot < 0 ? key : key.slice(0, dot);
}

function tokenAt(raw: string, index: number): { segment: TemplateSegment | 'open' | 'close'; style?: string; end: number } | null {
  const opener = raw[index];
  if (opener !== '{' && opener !== '<') {
    return null;
  }
  const end = raw.indexOf(opener === '{' ? '}' : '>', index + 1);
  if (end < 0) {
    return null;
  }
  const inner = raw.slice(index + 1, end);
  if (opener === '{') {
    return IDENTIFIER.test(inner) ? { segment: { kind: 'placeholder', name: inner, style: null }, end } : null;
  }
  const closing = inner.startsWith('/');
  const style = closing ? inner.slice(1) : inner;
  if (!(TEMPLATE_STYLES as readonly string[]).includes(style)) {
    return null;
  }
  return { segment: closing ? 'close' : 'open', style, end };
}

export function analyseTemplate(raw: string): TemplateAnalysis {
  const segments: TemplateSegment[] = [];
  const placeholders: string[] = [];
  const unknownTags: string[] = [];
  let literal = '';
  let openStyle: string | null = null;
  let renderStyle: string | null = null;
  let depth = 0;
  let balancedTags = true;
  const flush = () => {
    if (literal) {
      segments.push({ kind: 'text', text: literal, style: renderStyle });
      literal = '';
    }
  };
  let index = 0;
  while (index < raw.length) {
    const token = tokenAt(raw, index);
    if (!token) {
      if (raw[index] === '<') {
        const end = raw.indexOf('>', index + 1);
        const inner = end < 0 ? '' : raw.slice(index + 1, end);
        if (TAG_LIKE.test(inner)) {
          unknownTags.push(`<${inner}>`);
        }
      }
      literal += raw[index];
      index++;
      continue;
    }
    flush();
    if (token.segment === 'open') {
      if (openStyle !== null) {
        balancedTags = false;
      }
      openStyle = token.style ?? null;
      if (depth === 0) {
        renderStyle = token.style ?? null;
      }
      depth++;
    } else if (token.segment === 'close') {
      if (openStyle !== token.style) {
        balancedTags = false;
      }
      openStyle = null;
      if (depth > 0) {
        depth--;
      }
      if (depth === 0) {
        renderStyle = null;
      }
    } else if (token.segment.kind === 'placeholder') {
      if (!placeholders.includes(token.segment.name)) {
        placeholders.push(token.segment.name);
      }
      segments.push({ ...token.segment, style: renderStyle });
    }
    index = token.end + 1;
  }
  flush();
  return { segments, placeholders, balancedTags: balancedTags && openStyle === null, unknownTags };
}

function pluralSiblingKey(key: string): string | null {
  return key.endsWith('.one') ? `${key.slice(0, -'.one'.length)}.other` : null;
}

export function stringIssues(entry: StringEntry, model: StringsModel): StringIssues {
  const errors: string[] = [];
  const warnings: string[] = [];
  const { value, savedValue, key } = entry;
  if (!value.trim()) {
    errors.push('The text is empty.');
  }
  const analysis = analyseTemplate(value);
  const saved = analyseTemplate(savedValue);
  if (!analysis.balancedTags) {
    errors.push('Highlight tags are unbalanced or nested; every <tag> needs its own </tag> before the next one opens.');
  }
  const siblingKey = pluralSiblingKey(key);
  const sibling = siblingKey ? model.entries.find((other) => other.key === siblingKey) : undefined;
  const allowed = new Set([...saved.placeholders, ...(sibling ? analyseTemplate(sibling.savedValue).placeholders : [])]);
  const added = analysis.placeholders.filter((name) => !allowed.has(name));
  if (added.length > 0) {
    errors.push(`The game never fills ${added.map((name) => `{${name}}`).join(', ')}, so it would show literally.`);
  }
  const dropped = saved.placeholders.filter((name) => !analysis.placeholders.includes(name));
  if (dropped.length > 0) {
    warnings.push(`No longer shows ${dropped.map((name) => `{${name}}`).join(', ')}.`);
  }
  if (analysis.unknownTags.length > 0) {
    warnings.push(`${analysis.unknownTags.join(', ')} is not a highlight tag and will show as text. Tags: ${TEMPLATE_STYLES.join(', ')}.`);
  }
  if (value.includes('#')) {
    errors.push("'#' starts a comment in Starsector's JSON loader.");
  }
  if (key.startsWith('settings.') && value.includes('%')) {
    errors.push("LunaLib runs settings text through String.format, so it must not contain '%'.");
  }
  if (UNSPACED_SU.test(value)) {
    errors.push("Write a space before 'su', as vanilla does ('1000 su').");
  }
  return { errors, warnings };
}

export function validateStrings(model: StringsModel): string[] {
  return editedEntries(model).flatMap((entry) => stringIssues(entry, model).errors.map((error) => `${entry.key}: ${error}`));
}
