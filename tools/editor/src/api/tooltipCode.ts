import { create } from 'zustand';
import { mergeDescriptions, type Described, type LabelledDescription, type TooltipContent } from '../shared/tooltipContent.ts';
import { getJson, postJson, SERVER_HINT } from './client.ts';
import { stringsRevision } from './describe.ts';

export type TooltipCodeBuild = 'idle' | 'waiting' | 'building' | 'failed';

export interface TooltipCodeStatus {
  build: TooltipCodeBuild;
  message: string;
  copiedAtMs: number | null;
  codeRevision: number;
}

const STATUS_POLL_MS = 2000;
const UNREACHABLE_STATUS: TooltipCodeStatus = { build: 'failed', message: `The editor server is not running - ${SERVER_HINT}.`, copiedAtMs: null, codeRevision: 0 };

export const useTooltipCodeStatus = create<{ status: TooltipCodeStatus | null }>(() => ({ status: null }));

let statusPoll = 0;

function isBuilding(status: TooltipCodeStatus): boolean {
  return status.build === 'building' || status.build === 'waiting';
}

function showStatus(status: TooltipCodeStatus): void {
  useTooltipCodeStatus.setState({ status });
  window.clearTimeout(statusPoll);
  if (isBuilding(status)) {
    statusPoll = window.setTimeout(() => void loadTooltipCodeStatus(), STATUS_POLL_MS);
  }
}

export async function loadTooltipCodeStatus(): Promise<void> {
  try {
    const body = await getJson<TooltipCodeStatus & { ok: boolean }>('/api/describe/status');
    showStatus(body);
  } catch {
    showStatus(UNREACHABLE_STATUS);
  }
}

export async function refreshTooltipCode(): Promise<void> {
  const result = await postJson<TooltipCodeStatus>('/api/describe/refresh', {});
  showStatus(result.status === 0 ? UNREACHABLE_STATUS : result.data);
}

export function codeRevision(): number {
  return useTooltipCodeStatus.getState().status?.codeRevision ?? 0;
}

export type DescribedByHullSize = Record<string, Described>;

export type DescribedTypes =
  | { kind: 'ready'; byTypeId: ReadonlyMap<string, DescribedByHullSize> }
  | { kind: 'note'; message: string; pending: boolean };

interface DescribedTypesEntry {
  key: string;
  result: Promise<DescribedTypes>;
  settled: DescribedTypes | null;
}

function hashText(text: string): number {
  let hash = 5381;
  for (let index = 0; index < text.length; index++) {
    hash = (hash * 33 + (text.codePointAt(index) ?? 0)) % 2147483647;
  }
  return hash;
}

async function fetchDescribedTypes(typesJson: string, hullSizes: string[]): Promise<DescribedTypes> {
  try {
    const response = await fetch('/api/describe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Skill-Tree-Editor': '1' },
      body: `{"types":${typesJson},"hullSizes":${JSON.stringify(hullSizes)}}`,
    });
    const body = (await response.json()) as { descriptions?: Record<string, DescribedByHullSize>; pending?: boolean; message?: string; error?: string };
    if (body.descriptions) {
      return { kind: 'ready', byTypeId: new Map(Object.entries(body.descriptions)) };
    }
    return { kind: 'note', message: body.message || body.error || 'Preparing in-game tooltips...', pending: Boolean(body.pending) };
  } catch {
    return { kind: 'note', message: `In-game tooltips need the editor server - ${SERVER_HINT}.`, pending: false };
  }
}

export type TypeTooltip = { content: TooltipContent } | { note: string; pending: boolean };

export function typeTooltip(described: DescribedTypes, typeId: string, hullSizes: { value: string; label: string }[]): TypeTooltip {
  if (described.kind === 'note') {
    return { note: described.message, pending: described.pending };
  }
  const byHullSize = described.byTypeId.get(typeId);
  if (!byHullSize) {
    return { note: `Unknown or invalid type "${typeId}"`, pending: false };
  }
  const entries: LabelledDescription[] = [];
  for (const size of hullSizes) {
    const entry = byHullSize[size.value] as Described | undefined;
    if (!entry || entry.error) {
      return { note: entry?.error ?? 'Could not describe this type', pending: false };
    }
    entries.push({ label: size.label, described: entry });
  }
  return { content: mergeDescriptions(entries) };
}

export class DescribedTypesCache {
  private latest: DescribedTypesEntry | null = null;
  private lastReady: DescribedTypes | null = null;

  constructor(private readonly hullSizes: string[]) {}

  private keyOf(typesJson: string): string {
    return `${typesJson.length}|${hashText(typesJson)}|${stringsRevision()}|${codeRevision()}`;
  }

  describe(typesJson: string): Promise<DescribedTypes> {
    const key = this.keyOf(typesJson);
    if (this.latest?.key === key && this.latest.settled?.kind !== 'note') {
      return this.latest.result;
    }
    const entry: DescribedTypesEntry = { key, result: fetchDescribedTypes(typesJson, this.hullSizes), settled: null };
    entry.result = entry.result.then((described) => {
      entry.settled = described;
      if (described.kind === 'ready') {
        this.lastReady = described;
      }
      return described;
    });
    this.latest = entry;
    return entry.result;
  }

  current(typesJson: string): DescribedTypes | null {
    return this.latest?.key === this.keyOf(typesJson) ? this.latest.settled : null;
  }

  lastDescribed(): DescribedTypes | null {
    return this.lastReady;
  }
}
