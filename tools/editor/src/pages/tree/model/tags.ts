import type { Vocabulary } from '../../../api/vocabulary.ts';

export type TagGroupKey = 'theme' | 'region' | 'requirement';

export const TAG_GROUPS: { key: TagGroupKey; label: string }[] = [
  { key: 'theme', label: 'Theme' },
  { key: 'region', label: 'Region' },
  { key: 'requirement', label: 'Requirement' },
];

export const TYPE_TAG_GROUPS: TagGroupKey[] = ['theme', 'requirement'];
export const NODE_TAG_GROUPS: TagGroupKey[] = ['region', 'requirement'];
export const REGION_ONLY_TAG_GROUPS: TagGroupKey[] = ['region'];

export function isRegionTag(vocabulary: Vocabulary, tag: string): boolean {
  return vocabulary.regionTags.includes(tag);
}

export function tagGroupOf(vocabulary: Vocabulary, tag: string): TagGroupKey {
  if (isRegionTag(vocabulary, tag)) {
    return 'region';
  }
  return vocabulary.requirementTags.includes(tag) ? 'requirement' : 'theme';
}

export function tagsOfGroup(vocabulary: Vocabulary, group: TagGroupKey): string[] {
  return vocabulary.allTags.filter((tag) => tagGroupOf(vocabulary, tag) === group);
}

export function replaceGroupTags(vocabulary: Vocabulary, currentTags: string[], group: TagGroupKey | 'unknown', chosen: string[]): string[] {
  const inGroup = (tag: string) => (group === 'unknown' ? !vocabulary.allTags.includes(tag) : vocabulary.allTags.includes(tag) && tagGroupOf(vocabulary, tag) === group);
  const kept = currentTags.filter((tag) => !inGroup(tag) || chosen.includes(tag));
  return [...kept, ...chosen.filter((tag) => !currentTags.includes(tag))];
}
