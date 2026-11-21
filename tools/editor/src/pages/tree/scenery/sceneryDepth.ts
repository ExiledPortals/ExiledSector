import type { Decoration, DecorationKind } from '../model/decorations.ts';

export const SCENERY_DEPTHS = [
  'behindStars',
  'starDisc',
  'starAtmosphere',
  'starAurora',
  'belts',
  'creatures',
  'foregroundImages',
  'units',
  'starGlow',
] as const;

export type SceneryDepth = (typeof SCENERY_DEPTHS)[number];

const OVER_TREE_DEPTHS: ReadonlySet<SceneryDepth> = new Set<SceneryDepth>(['starGlow']);

export function isOverTree(depth: SceneryDepth): boolean {
  return OVER_TREE_DEPTHS.has(depth);
}

const RENDERER_KIND_ORDER: DecorationKind[] = ['image', 'star', 'ringBelt', 'serpent', 'threatUnit', 'shroudedMaw', 'hypershunt'];

function depthsOf(decoration: Decoration): SceneryDepth[] {
  switch (decoration.kind) {
    case 'image':
      return [decoration.behindStars ? 'behindStars' : 'foregroundImages'];
    case 'star':
      return ['starDisc', 'starAtmosphere', 'starAurora', 'starGlow'];
    case 'ringBelt':
      return ['belts'];
    case 'serpent':
      return ['creatures'];
    case 'threatUnit':
    case 'shroudedMaw':
    case 'hypershunt':
      return ['units'];
  }
}

export interface SceneryDraw {
  depth: SceneryDepth;
  decoration: Decoration;
}

export function sceneryDrawOrder(decorations: readonly Decoration[], overTree: boolean): SceneryDraw[] {
  const draws: SceneryDraw[] = [];
  for (const depth of SCENERY_DEPTHS) {
    if (isOverTree(depth) !== overTree) {
      continue;
    }
    for (const kind of RENDERER_KIND_ORDER) {
      for (const decoration of decorations) {
        if (decoration.kind === kind && depthsOf(decoration).includes(depth)) {
          draws.push({ depth, decoration });
        }
      }
    }
  }
  return draws;
}
