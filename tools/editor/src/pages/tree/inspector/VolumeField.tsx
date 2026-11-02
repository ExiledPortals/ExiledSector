import type { Vocabulary } from '../../../api/vocabulary.ts';
import { VOLUME_ONLY_TAG_GROUPS } from '../model/tags.ts';
import { TagGroupsField } from './TagGroupsField.tsx';

export function VolumeField({ vocabulary, tags, onChange }: { vocabulary: Vocabulary; tags: string[] | undefined; onChange: (tags: string[]) => void }) {
  return (
    <TagGroupsField label="Volume (volume tag, used by the in-game volume toggles)" vocabulary={vocabulary} tags={tags ?? []} groups={VOLUME_ONLY_TAG_GROUPS} onChange={onChange} />
  );
}
