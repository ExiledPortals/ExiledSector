import type { EffectInfo, Vocabulary } from '../../api/vocabulary.ts';
import { themeTagsFor } from '../../shared/themeTags.ts';
import { DIALOG_TYPE_SECTIONS, INSPECTOR_TYPE_SECTIONS, NODE_HEADER_TYPE_SECTIONS, type TypeFormSection } from './inspector/typeFormSections.ts';
import { ATLAS_TREE_FILES, SHIP_TREE_FILES, type TreeFiles } from './model/treeFiles.ts';
import type { Tier } from './model/types.ts';

export interface TreeTypeSections {
  nodeHeader: TypeFormSection[];
  inspector: TypeFormSection[];
  dialog: TypeFormSection[];
}

export interface TreeKind {
  label: string;
  typeNoun: string;
  files: TreeFiles;
  draftStorageKey: string;
  reservesRetiredNodeIds: boolean;
  typeSections: TreeTypeSections;
  editorVocabulary: (vocabulary: Vocabulary, declaredVolumes: string[]) => Vocabulary;
}

export const SHIP_TREE_KIND: TreeKind = {
  label: 'skill tree',
  typeNoun: 'skill type',
  files: SHIP_TREE_FILES,
  draftStorageKey: 'exiledSectorEditor.treeDraft',
  reservesRetiredNodeIds: true,
  typeSections: { nodeHeader: NODE_HEADER_TYPE_SECTIONS, inspector: INSPECTOR_TYPE_SECTIONS, dialog: DIALOG_TYPE_SECTIONS },
  editorVocabulary: (vocabulary) => vocabulary,
};

export const ATLAS_TIERS: Tier[] = ['SMALL', 'NOTABLE', 'KEYSTONE', 'ROOT', 'WORMHOLE'];

const CHART_EFFECT_MODE = 'star chart effect';
const ATLAS_EFFECT_MODE = 'atlas effect';

function atlasEditorVocabulary(vocabulary: Vocabulary, declaredVolumes: string[]): Vocabulary {
  const chartEffects = vocabulary.chartEffectNames.map((name): EffectInfo => ({ name, statMode: CHART_EFFECT_MODE, lowerIsBetter: false }));
  const atlasEffects = vocabulary.atlasEffectNames.map((name): EffectInfo => ({ name, statMode: ATLAS_EFFECT_MODE, lowerIsBetter: false }));
  const treeEffects = [...chartEffects, ...atlasEffects];
  const chartThemeTags = themeTagsFor(vocabulary, 'star_chart');
  return {
    ...vocabulary,
    effectNames: treeEffects.map((effect) => effect.name),
    effectInfo: new Map(treeEffects.map((effect) => [effect.name, effect])),
    themeTags: chartThemeTags,
    volumeTags: declaredVolumes.slice(),
    requirementTags: [],
    gateTags: [],
    ruleTags: [],
    allTags: [...chartThemeTags, ...declaredVolumes],
    tiers: ATLAS_TIERS.slice(),
  };
}

export const ATLAS_TREE_KIND: TreeKind = {
  label: 'Atlas Tree',
  typeNoun: 'Atlas Tree type',
  files: ATLAS_TREE_FILES,
  draftStorageKey: 'exiledSectorEditor.atlasTreeDraft',
  reservesRetiredNodeIds: false,
  typeSections: { nodeHeader: [], inspector: ['flavour', 'effects'], dialog: ['identity', 'tags', 'effects', 'flavour', 'todo'] },
  editorVocabulary: atlasEditorVocabulary,
};

export function capitalisedLabel(kind: TreeKind): string {
  return kind.label.charAt(0).toUpperCase() + kind.label.slice(1);
}
