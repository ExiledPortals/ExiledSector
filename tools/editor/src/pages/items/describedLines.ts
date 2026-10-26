import { useQuery } from '@tanstack/react-query';
import { fetchModifierLineDescriptions, stringsRevision, type DescribedModifierLines, type ModifierLineRequest } from '../../api/describe.ts';
import { isNumberText } from '../../shared/decimalText.ts';
import { HULL_ITEM_TYPE } from './model/definitions.ts';
import type { ItemFamily } from './model/itemFamily.ts';
import { lineSummary, parseModifierLine, STAR_CHART_TYPE, type ItemModifier, type ModifierLine } from './model/modifiers.ts';

const PENDING_RETRY_MS = 1500;
const FAILED_RETRY_MS = 5000;
const MARKUP_TAG = /<\/?(good|bad|hl|hullmod|node)>/g;
const describedMarkup = new Map<string, string | null>();
let cacheGeneration = 0;

export interface DescribedLine {
  line: ModifierLine;
  itemType: string;
}

export interface LineDescriptions {
  markupOf: (line: ModifierLine, itemType: string) => string;
  note: string | null;
  version: string;
}

function lineRequest(line: ModifierLine, itemType: string): ModifierLineRequest | null {
  const typed: ModifierLineRequest = { effect: line.effect, itemType };
  if (line.tierBands) {
    const readable = line.tierBands.length > 0 && line.tierBands.every((band) => band.unparsedText === undefined && isNumberText(band.tier) && isNumberText(band.min) && (band.max === '' || isNumberText(band.max)));
    return readable ? { ...typed, tierBands: line.tierBands.map((band) => ({ tier: Number(band.tier), min: Number(band.min), max: Number(band.max === '' ? band.min : band.max) })) } : null;
  }
  if (line.hullValues) {
    return line.hullValues.every(isNumberText) ? { ...typed, hullValues: line.hullValues.map(Number) } : null;
  }
  return isNumberText(line.min) && isNumberText(line.max) ? { ...typed, min: Number(line.min), max: Number(line.max) } : null;
}

function lineKey(line: ModifierLine, itemType: string): string {
  const bands = (line.tierBands ?? []).map((band) => band.unparsedText ?? `t${band.tier}=${band.min}-${band.max}`).join('/');
  return `${stringsRevision()}|${itemType}|${line.effect}|${line.min}|${line.max}|${(line.hullValues ?? []).join('/')}|${bands}`;
}

function descriptionNote(described: DescribedModifierLines | undefined): string | null {
  if (!described || described.lines) {
    return null;
  }
  if (described.pending) {
    return described.message || 'Preparing in-game descriptions...';
  }
  return `${described.error ?? described.message ?? 'In-game descriptions are unavailable'} Retrying in a few seconds; until then lines show their effect ids.`;
}

export function plainText(markup: string): string {
  return markup.replace(MARKUP_TAG, '');
}

export function modifierLinesOf(modifiers: readonly ItemModifier[], itemTypeOf: (modifier: ItemModifier) => string): DescribedLine[] {
  return modifiers.flatMap((modifier) => modifier.lines.map((line) => ({ line, itemType: itemTypeOf(modifier) })));
}

export function displayItemType(modifier: ItemModifier, family: ItemFamily): string {
  return family === 'chart' ? STAR_CHART_TYPE : (modifier.itemTypes[0] ?? HULL_ITEM_TYPE);
}

export function inlineLine(inlineText: string): ModifierLine {
  return parseModifierLine(inlineText);
}

export function useLineDescriptions(lines: readonly DescribedLine[]): LineDescriptions {
  const missing = new Map<string, ModifierLineRequest>();
  for (const { line, itemType } of lines) {
    const key = lineKey(line, itemType);
    if (!describedMarkup.has(key) && !missing.has(key)) {
      const request = lineRequest(line, itemType);
      if (request) {
        missing.set(key, request);
      }
    }
  }
  const keys = [...missing.keys()];
  const query = useQuery({
    queryKey: ['describedLines', keys],
    queryFn: async () => {
      const generation = cacheGeneration;
      const described = await fetchModifierLineDescriptions([...missing.values()]);
      const describedLines = described.lines;
      if (describedLines && generation === cacheGeneration) {
        keys.forEach((key, index) => describedMarkup.set(key, describedLines[index] ?? null));
      }
      return described;
    },
    enabled: keys.length > 0,
    refetchInterval: (state) => {
      const data = state.state.data;
      if (!data || data.lines) {
        return false;
      }
      return data.pending ? PENDING_RETRY_MS : FAILED_RETRY_MS;
    },
    staleTime: Infinity,
    gcTime: 0,
  });
  return {
    markupOf: (line, itemType) => describedMarkup.get(lineKey(line, itemType)) ?? lineSummary(line),
    note: keys.length > 0 ? descriptionNote(query.data) : null,
    version: `${stringsRevision()}:${describedMarkup.size}`,
  };
}

export function forgetDescriptionsForTests(): void {
  describedMarkup.clear();
  cacheGeneration++;
}
