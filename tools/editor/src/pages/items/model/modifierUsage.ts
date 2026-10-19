import { HULL_ITEM_TYPE, isTrueText, poolEntries, referenceGroups, type ItemDefinition } from './definitions.ts';
import { DOCTRINE_TYPE, doctrineModifierReferences, doctrineReferenceIssues, renameDoctrineModifier, type DoctrineArchive } from './doctrines.ts';
import type { ImplicitRow } from './implicits.ts';
import type { ItemModifier, ModifierUsage } from './modifiers.ts';

function addOnce(values: string[], value: string): void {
  if (!values.includes(value)) {
    values.push(value);
  }
}

export function modifierUsage(
  definitions: ItemDefinition[],
  implicitRows: ImplicitRow[],
  implicitItemType: string = HULL_ITEM_TYPE,
  doctrineArchives: readonly DoctrineArchive[] = [],
): Map<string, ModifierUsage> {
  const usage = new Map<string, ModifierUsage>();
  const usageOf = (modifierId: string) => {
    let entry = usage.get(modifierId);
    if (!entry) {
      entry = { items: [], nonUniqueItems: [], itemTypes: [], implicitSections: [] };
      usage.set(modifierId, entry);
    }
    return entry;
  };
  for (const definition of definitions) {
    for (const group of referenceGroups(definition)) {
      for (const entry of poolEntries(definition, group.key)) {
        if (entry.inlineText !== undefined) {
          continue;
        }
        const entryUsage = usageOf(entry.modifierId);
        entryUsage.items.push(definition.id);
        addOnce(entryUsage.itemTypes, group.itemType);
        if (!isTrueText(definition.unique)) {
          entryUsage.nonUniqueItems.push(definition.id);
        }
      }
    }
  }
  for (const row of implicitRows) {
    for (const reference of row.implicits) {
      if (reference.inlineText === undefined) {
        const referenceUsage = usageOf(reference.modifierId);
        referenceUsage.implicitSections.push(row.section);
        addOnce(referenceUsage.itemTypes, implicitItemType);
      }
    }
  }
  for (const archive of doctrineArchives) {
    for (const reference of doctrineModifierReferences(archive)) {
      const referenceUsage = usageOf(reference.modifierId);
      referenceUsage.doctrineArchives = referenceUsage.doctrineArchives ?? [];
      addOnce(referenceUsage.doctrineArchives, archive.id);
      addOnce(referenceUsage.itemTypes, DOCTRINE_TYPE);
    }
  }
  return usage;
}

export function isUsed(usage: ModifierUsage | undefined): boolean {
  return Boolean(usage && (usage.items.length > 0 || usage.implicitSections.length > 0 || (usage.doctrineArchives?.length ?? 0) > 0));
}

export function usedByText(usage: ModifierUsage | undefined): string {
  return [...(usage?.items ?? []), ...(usage?.implicitSections ?? []), ...(usage?.doctrineArchives ?? [])].join(', ');
}

export function renameModifierReferences(
  definitions: ItemDefinition[],
  implicitRows: ImplicitRow[],
  fromId: string,
  toId: string,
  doctrineArchives: DoctrineArchive[] = [],
): number {
  let renamed = renameDoctrineModifier(doctrineArchives, fromId, toId);
  for (const definition of definitions) {
    for (const group of referenceGroups(definition)) {
      for (const entry of poolEntries(definition, group.key)) {
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

export function referenceIssues(
  definitions: ItemDefinition[],
  implicitRows: ImplicitRow[],
  catalogue: Map<string, ItemModifier>,
  doctrineArchives: readonly DoctrineArchive[] = [],
): string[] {
  const issues: string[] = doctrineReferenceIssues(doctrineArchives, catalogue);
  for (const definition of definitions) {
    for (const group of referenceGroups(definition)) {
      for (const entry of poolEntries(definition, group.key)) {
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
        issues.push(`${row.section} implicits list ${reference.modifierId}, which is not a modifier`);
      } else if (modifier.slot !== 'implicit') {
        issues.push(`${row.section} implicits list ${modifier.id}, but it is a ${modifier.slot}`);
      }
    }
  }
  return issues;
}
