export interface Described {
  pending?: boolean;
  error?: string;
  message?: string;
  title?: string;
  flavour?: string | null;
  effects?: string[];
  details?: string[];
  expandHint?: string;
}

export interface TooltipGroup {
  label: string | null;
  lines: string[];
}

export interface TooltipContent {
  title: string;
  flavour: string | null;
  effects: TooltipGroup[];
  details: TooltipGroup[];
  expandHint: string;
}

export interface LabelledDescription {
  label: string;
  described: Described;
}

function groupLines(entries: { label: string; lines: string[] }[]): TooltipGroup[] {
  if (entries.length === 1) {
    return entries[0].lines.length ? [{ label: null, lines: entries[0].lines }] : [];
  }
  const common = entries[0].lines.filter((line) => entries.every((entry) => entry.lines.includes(line)));
  const groups: TooltipGroup[] = common.length ? [{ label: null, lines: common }] : [];
  for (const entry of entries) {
    const own = entry.lines.filter((line) => !common.includes(line));
    if (own.length) {
      groups.push({ label: entry.label, lines: own });
    }
  }
  return groups;
}

export function mergeDescriptions(entries: LabelledDescription[]): TooltipContent {
  const first = entries[0].described;
  return {
    title: first.title ?? '',
    flavour: first.flavour ?? null,
    effects: groupLines(entries.map(({ label, described }) => ({ label, lines: described.effects ?? [] }))),
    details: groupLines(entries.map(({ label, described }) => ({ label, lines: described.details ?? [] }))),
    expandHint: first.expandHint ?? '',
  };
}
