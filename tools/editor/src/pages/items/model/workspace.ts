import type { DataFiles } from '../../../api/client.ts';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { lineEndingOf, type LineEnding } from '../../../shared/lineEndings.ts';
import { serializeSkillTypesText } from '../../tree/model/serialization.ts';
import type { SkillType } from '../../tree/model/types.ts';
import {
  markAnchorsSaved,
  missingVolumes,
  parseWormholeAnchorsCsv,
  serializeWormholeAnchorsCsv,
  treeNodeFacts,
  validateWormholeAnchors,
  WORMHOLE_ANCHOR_RESOURCE,
  type TreeNodeFacts,
  type WormholeAnchorsModel,
} from './anchors.ts';
import {
  definitionWarnings,
  HULL_ITEM_TYPE,
  itemTypeOfKind,
  markDefinitionsSaved,
  parseFamilyDefinitions,
  serializeFamilyDefinitions,
  SUBROUTINE_TYPE,
  validateItemDefinitions,
  type FamilyDefinitions,
  type ItemDefinition,
} from './definitions.ts';
import {
  DOCTRINE_ARCHIVE_RESOURCE,
  DOCTRINE_SUBROUTINE_KIND,
  doctrineTypeFacts,
  markDoctrinesSaved,
  parseDoctrineArchivesCsv,
  serializeDoctrineArchivesCsv,
  uncoveredDoctrines,
  validateDoctrineArchives,
  type DoctrineArchivesModel,
  type DoctrineTypeFacts,
} from './doctrines.ts';
import {
  CORRUPTION_POOLS,
  parseCorruptionPoolCsv,
  parseImplicitsCsv,
  parseStarChartImplicitsCsv,
  serializeCorruptionPoolCsv,
  serializeImplicitsCsv,
  serializeStarChartImplicitsCsv,
  type CorruptionPoolModel,
  type ImplicitsModel,
} from './implicits.ts';
import type { ItemFamily } from './itemFamily.ts';
import { modifierUsage, referenceIssues } from './modifierUsage.ts';
import {
  markModifiersSaved,
  modifierById,
  parseModifiersCsv,
  serializeModifiersCsv,
  STAR_CHART_TYPE,
  validateModifiers,
  type ItemModifier,
  type ModifiersModel,
  type ModifierUsage,
} from './modifiers.ts';

export const TYPES_RESOURCE = 'types';
const TREE_RESOURCE = 'tree';
export const SUBROUTINES_RESOURCE = 'subroutines';
export const HULL_ITEMS_RESOURCE = 'hull-items';
export const STAR_CHARTS_RESOURCE = 'star-charts';
export const SHIP_MODIFIERS_RESOURCE = 'item-modifiers';
export const CHART_MODIFIERS_RESOURCE = 'star-chart-modifiers';
const HULL_IMPLICITS_RESOURCE = 'hull-item-implicits';
const CHART_IMPLICITS_RESOURCE = 'star-chart-implicits';

export const RESOURCE_FILE_NAMES: Record<string, string> = {
  [TYPES_RESOURCE]: 'skill_types.json',
  [SUBROUTINES_RESOURCE]: 'subroutines.csv',
  [HULL_ITEMS_RESOURCE]: 'hull_items.csv',
  [STAR_CHARTS_RESOURCE]: 'star_charts.csv',
  [SHIP_MODIFIERS_RESOURCE]: 'item_modifiers.csv',
  [CHART_MODIFIERS_RESOURCE]: 'star_chart_modifiers.csv',
  [HULL_IMPLICITS_RESOURCE]: 'hull_item_implicits.csv',
  [CHART_IMPLICITS_RESOURCE]: 'star_chart_implicits.csv',
  [DOCTRINE_ARCHIVE_RESOURCE]: 'doctrine_archives.csv',
  [WORMHOLE_ANCHOR_RESOURCE]: 'wormhole_anchors.csv',
  ...Object.fromEntries(CORRUPTION_POOLS.map((pool) => [pool.resource, pool.fileName])),
};

export const ITEMS_RESOURCES: string[] = [...Object.keys(RESOURCE_FILE_NAMES), TREE_RESOURCE];

export interface ItemsModel {
  ship: FamilyDefinitions;
  charts: FamilyDefinitions;
  shipModifiers: ModifiersModel;
  chartModifiers: ModifiersModel;
  hullImplicits: ImplicitsModel;
  chartImplicits: ImplicitsModel;
  corruptionPools: CorruptionPoolModel[];
  doctrines: DoctrineArchivesModel;
  anchors: WormholeAnchorsModel;
  skillTypes: SkillType[];
  typesText: string;
  typesKey: string;
  typesLineEnding: LineEnding;
  typesSaveCount: number;
  nodeFacts: Record<string, TreeNodeFacts>;
  typeFacts: Record<string, DoctrineTypeFacts>;
  savedTexts: Record<string, string>;
}

export function parseItemsModel(files: DataFiles, vocabulary: Vocabulary): ItemsModel {
  const definitionTexts = Object.fromEntries([SUBROUTINES_RESOURCE, HULL_ITEMS_RESOURCE, STAR_CHARTS_RESOURCE].map((name) => [name, files[name].content]));
  const typesText = files[TYPES_RESOURCE].content;
  const model: ItemsModel = {
    ship: parseFamilyDefinitions('ship', definitionTexts),
    charts: parseFamilyDefinitions('chart', definitionTexts),
    shipModifiers: parseModifiersCsv(files[SHIP_MODIFIERS_RESOURCE].content),
    chartModifiers: parseModifiersCsv(files[CHART_MODIFIERS_RESOURCE].content),
    hullImplicits: parseImplicitsCsv(files[HULL_IMPLICITS_RESOURCE].content, vocabulary),
    chartImplicits: parseStarChartImplicitsCsv(files[CHART_IMPLICITS_RESOURCE].content, vocabulary),
    corruptionPools: CORRUPTION_POOLS.map((pool) => parseCorruptionPoolCsv(files[pool.resource].content, pool)),
    doctrines: parseDoctrineArchivesCsv(files[DOCTRINE_ARCHIVE_RESOURCE].content),
    anchors: parseWormholeAnchorsCsv(files[WORMHOLE_ANCHOR_RESOURCE].content),
    skillTypes: (JSON.parse(typesText) as { skillTypes: SkillType[] }).skillTypes,
    typesText,
    typesKey: files[TYPES_RESOURCE].hash,
    typesLineEnding: lineEndingOf(typesText),
    typesSaveCount: 0,
    nodeFacts: treeNodeFacts(files[TREE_RESOURCE].content, typesText, vocabulary),
    typeFacts: doctrineTypeFacts(typesText),
    savedTexts: {},
  };
  model.savedTexts = serializeItemsModel(model);
  return model;
}

export function serializeItemsModel(model: ItemsModel): Record<string, string> {
  return {
    ...serializeFamilyDefinitions(model.ship),
    ...serializeFamilyDefinitions(model.charts),
    [SHIP_MODIFIERS_RESOURCE]: serializeModifiersCsv(model.shipModifiers),
    [CHART_MODIFIERS_RESOURCE]: serializeModifiersCsv(model.chartModifiers),
    [HULL_IMPLICITS_RESOURCE]: serializeImplicitsCsv(model.hullImplicits),
    [CHART_IMPLICITS_RESOURCE]: serializeStarChartImplicitsCsv(model.chartImplicits),
    ...Object.fromEntries(model.corruptionPools.map((pool) => [pool.pool.resource, serializeCorruptionPoolCsv(pool)])),
    [DOCTRINE_ARCHIVE_RESOURCE]: serializeDoctrineArchivesCsv(model.doctrines),
    [WORMHOLE_ANCHOR_RESOURCE]: serializeWormholeAnchorsCsv(model.anchors),
    [TYPES_RESOURCE]: serializeSkillTypesText(model.skillTypes, model.typesLineEnding),
  };
}

export function changedItemFiles(model: ItemsModel): Record<string, string> {
  return Object.fromEntries(Object.entries(serializeItemsModel(model)).filter(([resource, text]) => model.savedTexts[resource] !== text));
}

export function savedItemsMessage(changed: readonly string[]): string {
  const names = changed.map((resource) => RESOURCE_FILE_NAMES[resource] ?? resource);
  if (names.length === 0) {
    return 'Nothing to save';
  }
  return `Saved ${names.length === 1 ? names[0] : `${names.slice(0, -1).join(', ')} and ${names[names.length - 1]}`}`;
}

export function shipModifierUsage(model: ItemsModel): Map<string, ModifierUsage> {
  return modifierUsage(model.ship.definitions, model.hullImplicits.rows, HULL_ITEM_TYPE, model.doctrines.archives, model.corruptionPools);
}

export function chartModifierUsage(model: ItemsModel): Map<string, ModifierUsage> {
  return modifierUsage(model.charts.definitions, model.chartImplicits.rows, STAR_CHART_TYPE);
}

export function familyCatalogue(model: ItemsModel, family: ItemFamily): ModifiersModel {
  return family === 'chart' ? model.chartModifiers : model.shipModifiers;
}

export function doctrineSubroutines(model: ItemsModel): ItemDefinition[] {
  return model.ship.definitions.filter((definition) => itemTypeOfKind(definition.kind) === SUBROUTINE_TYPE && definition.subroutineKind === DOCTRINE_SUBROUTINE_KIND);
}

function idsOf(definitions: readonly { id: string }[]): string[] {
  return definitions.map((definition) => definition.id);
}

function changedAny(changed: ReadonlySet<string>, resources: readonly string[]): boolean {
  return resources.some((resource) => changed.has(resource));
}

const SHIP_DEFINITION_RESOURCES = [SUBROUTINES_RESOURCE, HULL_ITEMS_RESOURCE];
const SHIP_REFERENCE_RESOURCES = [SHIP_MODIFIERS_RESOURCE, ...SHIP_DEFINITION_RESOURCES, HULL_IMPLICITS_RESOURCE, DOCTRINE_ARCHIVE_RESOURCE, ...CORRUPTION_POOLS.map((pool) => pool.resource)];
const CHART_REFERENCE_RESOURCES = [CHART_MODIFIERS_RESOURCE, STAR_CHARTS_RESOURCE, CHART_IMPLICITS_RESOURCE];

export function validateItemsModel(model: ItemsModel, vocabulary: Vocabulary): string[] {
  const changed = new Set(Object.keys(changedItemFiles(model)));
  const shipCatalogue = modifierById(model.shipModifiers.modifiers);
  const chartCatalogue = modifierById(model.chartModifiers.modifiers);
  const shipIds = idsOf(model.ship.definitions);
  const chartIds = idsOf(model.charts.definitions);
  const issues: string[] = [];
  if (changedAny(changed, SHIP_DEFINITION_RESOURCES)) {
    issues.push(...validateItemDefinitions(model.ship.definitions, vocabulary, shipCatalogue, 'ship', chartIds));
  }
  if (changed.has(STAR_CHARTS_RESOURCE)) {
    issues.push(...validateItemDefinitions(model.charts.definitions, vocabulary, chartCatalogue, 'chart', shipIds));
  }
  if (changedAny(changed, [SHIP_MODIFIERS_RESOURCE, ...SHIP_DEFINITION_RESOURCES, DOCTRINE_ARCHIVE_RESOURCE])) {
    issues.push(...validateModifiers(model.shipModifiers.modifiers, vocabulary, shipModifierUsage(model), 'ship', new Set(idsOf(model.chartModifiers.modifiers))));
  }
  if (changedAny(changed, [CHART_MODIFIERS_RESOURCE, STAR_CHARTS_RESOURCE])) {
    issues.push(...validateModifiers(model.chartModifiers.modifiers, vocabulary, chartModifierUsage(model), 'chart', new Set(idsOf(model.shipModifiers.modifiers))));
  }
  if (changedAny(changed, SHIP_REFERENCE_RESOURCES)) {
    issues.push(...referenceIssues(model.ship.definitions, model.hullImplicits.rows, shipCatalogue, model.doctrines.archives, model.corruptionPools));
  }
  if (changedAny(changed, CHART_REFERENCE_RESOURCES)) {
    issues.push(...referenceIssues(model.charts.definitions, model.chartImplicits.rows, chartCatalogue));
  }
  if (changedAny(changed, [DOCTRINE_ARCHIVE_RESOURCE, SUBROUTINES_RESOURCE])) {
    issues.push(...validateDoctrineArchives(model.doctrines.archives, vocabulary, idsOf(doctrineSubroutines(model)), model.typeFacts, model.shipModifiers.modifiers));
  }
  if (changed.has(WORMHOLE_ANCHOR_RESOURCE)) {
    issues.push(...validateWormholeAnchors(model.anchors.anchors, vocabulary, model.nodeFacts, [...shipIds, ...chartIds]));
  }
  return [...new Set(issues)];
}

export function itemsModelWarnings(model: ItemsModel, vocabulary: Vocabulary): string[] {
  const changed = new Set(Object.keys(changedItemFiles(model)));
  const warnings: string[] = [];
  if (changedAny(changed, SHIP_DEFINITION_RESOURCES)) {
    warnings.push(...definitionWarnings(model.ship.definitions, 'ship'));
  }
  if (changed.has(STAR_CHARTS_RESOURCE)) {
    warnings.push(...definitionWarnings(model.charts.definitions, 'chart'));
  }
  if (changedAny(changed, [DOCTRINE_ARCHIVE_RESOURCE, SUBROUTINES_RESOURCE])) {
    warnings.push(...uncoveredDoctrines(model.doctrines.archives, idsOf(doctrineSubroutines(model))).map((doctrineId) => `${doctrineId} has no doctrine archive row, so it rewrites nothing`));
  }
  if (changed.has(WORMHOLE_ANCHOR_RESOURCE)) {
    warnings.push(...missingVolumes(model.anchors.anchors, vocabulary).map((volume) => `${volume} has no anchor, so nothing can open its wormhole`));
  }
  return warnings;
}

export function markItemsSaved(draft: ItemsModel, saved: ItemsModel): void {
  const savedTexts = serializeItemsModel(saved);
  for (const [resource, text] of Object.entries(savedTexts)) {
    if (saved.savedTexts[resource] !== text) {
      draft.savedTexts[resource] = text;
      if (resource === TYPES_RESOURCE) {
        draft.typesSaveCount += 1;
      }
    }
  }
  markDefinitionsSaved(draft.ship, saved.ship);
  markDefinitionsSaved(draft.charts, saved.charts);
  markModifiersSaved(draft.shipModifiers, saved.shipModifiers);
  markModifiersSaved(draft.chartModifiers, saved.chartModifiers);
  markDoctrinesSaved(draft.doctrines, saved.doctrines);
  markAnchorsSaved(draft.anchors, saved.anchors);
}

export function otherItemIds(model: ItemsModel, family: ItemFamily | 'anchor'): string[] {
  const ids = [...(family === 'ship' ? [] : idsOf(model.ship.definitions)), ...(family === 'chart' ? [] : idsOf(model.charts.definitions))];
  return family === 'anchor' ? ids : [...ids, ...idsOf(model.anchors.anchors)];
}

export function modifiersOf(model: ItemsModel, family: ItemFamily): ItemModifier[] {
  return familyCatalogue(model, family).modifiers;
}
