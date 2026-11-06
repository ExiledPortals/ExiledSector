import type { Vocabulary } from '../../../api/vocabulary.ts';

export type TagGroupKey = 'theme' | 'volume' | 'requirement' | 'gate' | 'rule';

export const TAG_GROUPS: { key: TagGroupKey; label: string }[] = [
  { key: 'theme', label: 'Theme' },
  { key: 'volume', label: 'Volume' },
  { key: 'requirement', label: 'Requirement' },
  { key: 'gate', label: 'Gate' },
  { key: 'rule', label: 'Rule' },
];

export const TYPE_TAG_GROUPS: TagGroupKey[] = ['theme', 'requirement', 'rule'];
export const NODE_TAG_GROUPS: TagGroupKey[] = ['volume', 'requirement', 'gate'];
export const VOLUME_ONLY_TAG_GROUPS: TagGroupKey[] = ['volume'];

export function isVolumeTag(vocabulary: Vocabulary, tag: string): boolean {
  return vocabulary.volumeTags.includes(tag);
}

export function tagGroupOf(vocabulary: Vocabulary, tag: string): TagGroupKey {
  if (isVolumeTag(vocabulary, tag)) {
    return 'volume';
  }
  if (vocabulary.gateTags.includes(tag)) {
    return 'gate';
  }
  if (vocabulary.ruleTags.includes(tag)) {
    return 'rule';
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
