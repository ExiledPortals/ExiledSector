export interface TreeFiles {
  typesResource: string;
  typesFileName: string;
  layoutResource: string;
  layoutFileName: string;
  declaresVolumes: boolean;
}

export const SHIP_TREE_FILES: TreeFiles = {
  typesResource: 'types',
  typesFileName: 'skill_types.json',
  layoutResource: 'tree',
  layoutFileName: 'ship_skill_tree.json',
  declaresVolumes: false,
};

export const ATLAS_TREE_FILES: TreeFiles = {
  typesResource: 'atlas-types',
  typesFileName: 'atlas_tree_types.json',
  layoutResource: 'atlas-tree',
  layoutFileName: 'atlas_tree.json',
  declaresVolumes: true,
};
