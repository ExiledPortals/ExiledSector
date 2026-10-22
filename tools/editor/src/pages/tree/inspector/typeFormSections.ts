export type TypeFormSection =
  | 'identity'
  | 'tags'
  | 'effects'
  | 'hullSizeEffects'
  | 'vanillaHullMod'
  | 'unlockConditions'
  | 'exclusivity'
  | 'itemCost'
  | 'temporary'
  | 'requiredHullSizes'
  | 'moduleShare'
  | 'optionalOptions'
  | 'texts'
  | 'todo';

export const INSPECTOR_TYPE_SECTIONS: TypeFormSection[] = ['texts', 'effects', 'hullSizeEffects', 'itemCost', 'temporary'];
export const DIALOG_TYPE_SECTIONS: TypeFormSection[] = [
  'identity',
  'tags',
  'effects',
  'hullSizeEffects',
  'vanillaHullMod',
  'unlockConditions',
  'exclusivity',
  'itemCost',
  'temporary',
  'requiredHullSizes',
  'moduleShare',
  'optionalOptions',
  'texts',
  'todo',
];
