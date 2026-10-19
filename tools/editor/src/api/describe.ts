import type { Described } from '../shared/tooltipContent.ts';
import { SERVER_HINT } from './client.ts';

export interface DescribeRequest {
  typesJson: string;
  typeId: string;
  hullSize: string;
  strings?: Record<string, string>;
}

let savedStringsRevision = 0;

export function stringsSaved(): void {
  savedStringsRevision++;
}

export function stringsRevision(): number {
  return savedStringsRevision;
}

export async function fetchDescription({ typesJson, typeId, hullSize, strings }: DescribeRequest): Promise<Described> {
  const stringsField = strings ? `,"strings":${JSON.stringify(strings)}` : '';
  try {
    const response = await fetch('/api/describe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Skill-Tree-Editor': '1' },
      body: `{"types":${typesJson},"typeId":${JSON.stringify(typeId)},"hullSize":${JSON.stringify(hullSize)}${stringsField}}`,
    });
    return (await response.json()) as Described;
  } catch {
    return { error: `In-game descriptions need the editor server - ${SERVER_HINT}.` };
  }
}

export interface DescribedModifierLines {
  pending?: boolean;
  error?: string;
  message?: string;
  lines?: (string | null)[];
}

export interface ModifierLineRequest {
  effect: string;
  itemType?: string;
  min?: number;
  max?: number;
  hullValues?: number[];
  tierBands?: { tier: number; min: number; max: number }[];
}

export async function fetchModifierLineDescriptions(modifierLines: ModifierLineRequest[]): Promise<DescribedModifierLines> {
  try {
    const response = await fetch('/api/describe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Skill-Tree-Editor': '1' },
      body: JSON.stringify({ modifierLines }),
    });
    return (await response.json()) as DescribedModifierLines;
  } catch {
    return { error: `In-game descriptions need the editor server - ${SERVER_HINT}.` };
  }
}
