import { AFFIX_GROUPS, isTrueText, type HullItemDefinition } from './definitions.ts';
import type { ImplicitRow } from './implicits.ts';
import type { HullItemModifier, ModifierUsage } from './modifiers.ts';

export function modifierUsage(definitions: HullItemDefinition[], implicitRows: ImplicitRow[]): Map<string, ModifierUsage> {
  const usage = new Map<string, ModifierUsage>();
  const usageOf = (modifierId: string) => {
    let entry = usage.get(modifierId);
    if (!entry) {
      entry = { hullItems: [], nonUniqueHullItems: [], implicitTypes: [] };
      usage.set(modifierId, entry);
    }
    return entry;
  };
  for (const definition of definitions) {
    for (const group of AFFIX_GROUPS) {
      for (const entry of definition[group.key]) {
        if (entry.inlineText !== undefined) {
          continue;
        }
        const entryUsage = usageOf(entry.modifierId);
        entryUsage.hullItems.push(definition.id);
        if (!isTrueText(definition.unique)) {
          entryUsage.nonUniqueHullItems.push(definition.id);
        }
      }
    }
  }
  for (const row of implicitRows) {
    for (const reference of row.implicits) {
      if (reference.inlineText === undefined) {
        usageOf(reference.modifierId).implicitTypes.push(row.type);
      }
    }
  }
  return usage;
}

export function isUsed(usage: ModifierUsage | undefined): boolean {
  return Boolean(usage && (usage.hullItems.length > 0 || usage.implicitTypes.length > 0));
}

export function renameModifierReferences(definitions: HullItemDefinition[], implicitRows: ImplicitRow[], fromId: string, toId: string): number {
  let renamed = 0;
  for (const definition of definitions) {
    for (const group of AFFIX_GROUPS) {
      for (const entry of definition[group.key]) {
        if (entry.inlineText === undefined && entry.modifierId === fromId) {
          entry.modifierId = toId;
          renamed++;
        }
      }
    }
  }
  for (const row of implicitRows) {
    for (const reference of row.implicits) {
      if (reference.inlineText === undefined && reference.modifierId === fromId) {
        reference.modifierId = toId;
        renamed++;
      }
    }
  }
  return renamed;
}

export function referenceIssues(definitions: HullItemDefinition[], implicitRows: ImplicitRow[], catalogue: Map<string, HullItemModifier>): string[] {
  const issues: string[] = [];
  for (const definition of definitions) {
    for (const group of AFFIX_GROUPS) {
      for (const entry of definition[group.key]) {
        if (entry.inlineText !== undefined) {
          continue;
        }
        const modifier = catalogue.get(entry.modifierId);
        if (!modifier) {
          issues.push(`${definition.id} lists ${entry.modifierId}, which is not a modifier`);
        } else if (modifier.slot !== group.singular) {
          issues.push(`${definition.id} lists ${modifier.id} as a ${group.singular}, but it is a ${modifier.slot}`);
        }
      }
    }
  }
  for (const row of implicitRows) {
    for (const reference of row.implicits) {
      if (reference.inlineText !== undefined) {
        continue;
      }
      const modifier = catalogue.get(reference.modifierId);
      if (!modifier) {
        issues.push(`${row.type} implicits list ${reference.modifierId}, which is not a modifier`);
      } else if (modifier.slot !== 'implicit') {
        issues.push(`${row.type} implicits list ${modifier.id}, but it is a ${modifier.slot}`);
      }
    }
  }
  return issues;
}
