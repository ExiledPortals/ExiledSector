import {
  ANCHOR_RECTANGLE_ASPECT,
  PARAMETERISED_UNLOCK_CONDITIONS,
  tierOf,
  type ConnectorCurve,
  type EffectEntry,
  type HullSizeEffectEntry,
  type IntegratedEffects,
  type NodePair,
  type OrbitAnchor,
  type SkillType,
  type TreeNode,
  type UnlockCondition,
} from './types.ts';

type JsonRecord = Record<string, unknown>;

export interface JsonField<TRecord> {
  key: keyof TRecord & string;
  keep?: (written: unknown, record: TRecord) => boolean;
  write?: (record: TRecord) => unknown;
}

function always(): boolean {
  return true;
}

function hasContent(value: unknown): boolean {
  return Array.isArray(value) ? value.length > 0 : Boolean(value);
}

export function writeFields<TRecord>(record: TRecord, fields: readonly JsonField<TRecord>[]): JsonRecord {
  const out: JsonRecord = {};
  for (const field of fields) {
    const value = field.write ? field.write(record) : (record as JsonRecord)[field.key];
    if ((field.keep ?? hasContent)(value, record)) {
      out[field.key] = value;
    }
  }
  return out;
}

export function refuseUnknownFields<TRecord>(json: unknown, fields: readonly JsonField<TRecord>[], fileName: string, where: string): void {
  const known = new Set<string>(fields.map((field) => field.key));
  const unknown = Object.keys((json ?? {}) as JsonRecord).filter((key) => !known.has(key));
  if (unknown.length > 0) {
    const listed = unknown.map((key) => `"${key}"`).join(', ');
    throw new Error(`${fileName} has unknown ${unknown.length > 1 ? 'fields' : 'field'} ${listed} in ${where}; the editor would drop ${unknown.length > 1 ? 'them' : 'it'} on save`);
  }
}

export function serializeUnlockConditions(conditions: UnlockCondition[] | undefined): UnlockCondition[] {
  const out: UnlockCondition[] = [];
  for (const condition of conditions ?? []) {
    if (condition.type === 'characterStat' && condition.statId) {
      out.push({ type: 'characterStat', statId: condition.statId });
    } else if (condition.type === 'minShipLevel' && condition.level) {
      out.push({ type: 'minShipLevel', level: condition.level });
    } else if (condition.type === 'memoryFlag' && condition.key) {
      out.push({ type: 'memoryFlag', key: condition.key });
    } else if (condition.type === 'anchorFound' && condition.volume) {
      out.push({ type: 'anchorFound', volume: condition.volume });
    } else if (condition.type === 'blueprint' && condition.id) {
      out.push({ type: 'blueprint', category: condition.category || 'hullmod', id: condition.id });
    } else if (condition.type && !PARAMETERISED_UNLOCK_CONDITIONS.has(condition.type)) {
      out.push({ type: condition.type });
    }
  }
  return out;
}

function serializeEffects(entries: EffectEntry[] | undefined): JsonRecord[] {
  return (entries ?? []).map((entry) => ({ effect: entry.effect, magnitude: entry.magnitude }));
}

function serializeHullSizeEffects(entries: HullSizeEffectEntry[] | undefined): JsonRecord[] {
  return (entries ?? []).map((entry) => ({
    effect: entry.effect,
    frigate: entry.frigate,
    destroyer: entry.destroyer,
    cruiser: entry.cruiser,
    capitalShip: entry.capitalShip,
  }));
}

export const INTEGRATED_EFFECTS_FIELDS: readonly JsonField<IntegratedEffects>[] = [
  { key: 'effects', write: (integrated) => serializeEffects(integrated.effects) },
  { key: 'hullSizeEffects', write: (integrated) => serializeHullSizeEffects(integrated.hullSizeEffects) },
  { key: 'effectText' },
];

export const SKILL_TYPE_FIELDS: readonly JsonField<SkillType>[] = [
  { key: 'id', keep: always },
  { key: 'name', keep: always },
  { key: 'icon', keep: always },
  { key: 'tier', write: tierOf, keep: (tier) => tier !== 'SMALL' },
  { key: 'effects', write: (type) => serializeEffects(type.effects) },
  { key: 'hullSizeEffects', write: (type) => serializeHullSizeEffects(type.hullSizeEffects) },
  { key: 'integrated', write: (type) => type.integrated && writeFields(type.integrated, INTEGRATED_EFFECTS_FIELDS) },
  { key: 'vanillaHullMod' },
  { key: 'itemCost', write: (type) => type.itemCost?.itemId && { itemId: type.itemCost.itemId, quantity: type.itemCost.quantity || 0 } },
  { key: 'temporaryAfterDeploymentSeconds' },
  { key: 'requiredHullSizes' },
  { key: 'unlockConditions', write: (type) => serializeUnlockConditions(type.unlockConditions) },
  { key: 'exclusiveHullMods' },
  { key: 'phantomHullMods' },
  { key: 'exclusiveSkillTypes' },
  { key: 'options' },
  { key: 'flavour' },
  { key: 'flavourAutomated' },
  { key: 'effectText' },
  { key: 'todo' },
  { key: 'tags' },
  { key: 'moduleShare' },
];

export const TREE_NODE_FIELDS: readonly JsonField<TreeNode>[] = [
  { key: 'id', keep: always },
  { key: 'type', keep: always },
  { key: 'connectedTo', keep: always },
  { key: 'x', keep: always },
  { key: 'y', keep: always },
  { key: 'orbitAnchorId' },
  { key: 'orbitAngle', keep: (_angle, node) => Boolean(node.orbitAnchorId) },
  { key: 'ringBeltPath' },
  { key: 'ringBeltColor' },
  { key: 'ringBeltWidth' },
  { key: 'wormholeColor' },
  { key: 'pairedWith' },
  { key: 'tags' },
];

function hasShapeBesidesCircle(anchor: OrbitAnchor): boolean {
  return Boolean(anchor.shape) && anchor.shape !== 'circle';
}

export const ORBIT_ANCHOR_FIELDS: readonly JsonField<OrbitAnchor>[] = [
  { key: 'id', keep: always },
  { key: 'x', keep: always },
  { key: 'y', keep: always },
  { key: 'radius', keep: always },
  { key: 'shape', keep: (_shape, anchor) => hasShapeBesidesCircle(anchor) },
  { key: 'aspectRatio', write: (anchor) => anchor.aspectRatio || ANCHOR_RECTANGLE_ASPECT, keep: (_ratio, anchor) => anchor.shape === 'rectangle' },
  { key: 'rotation', keep: (rotation, anchor) => hasShapeBesidesCircle(anchor) && Boolean(rotation) },
];

export const CONNECTOR_CURVE_FIELDS: readonly JsonField<ConnectorCurve>[] = [
  { key: 'a', keep: always },
  { key: 'b', keep: always },
  { key: 'controlX', keep: always },
  { key: 'controlY', keep: always },
];

export const NODE_PAIR_FIELDS: readonly JsonField<NodePair>[] = [
  { key: 'a', keep: always },
  { key: 'b', keep: always },
];
