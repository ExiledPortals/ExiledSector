export type TypeFormSection =
  | 'identity'
  | 'tags'
  | 'effects'
  | 'hullSizeEffects'
  | 'integrated'
  | 'vanillaHullMod'
  | 'unlockConditions'
  | 'exclusivity'
  | 'itemCost'
  | 'temporary'
  | 'requiredHullSizes'
  | 'moduleShare'
  | 'options'
  | 'texts'
  | 'flavour'
  | 'todo';

export const NODE_HEADER_TYPE_SECTIONS: TypeFormSection[] = ['tags'];
export const INSPECTOR_TYPE_SECTIONS: TypeFormSection[] = ['texts', 'effects', 'hullSizeEffects', 'integrated', 'itemCost', 'temporary'];
export const DIALOG_TYPE_SECTIONS: TypeFormSection[] = [
  'identity',
  'tags',
  'effects',
  'hullSizeEffects',
  'integrated',
  'vanillaHullMod',
  'unlockConditions',
  'exclusivity',
  'itemCost',
  'temporary',
  'requiredHullSizes',
  'moduleShare',
  'options',
  'texts',
  'todo',
];
