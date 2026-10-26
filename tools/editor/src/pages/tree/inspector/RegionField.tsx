import type { Vocabulary } from '../../../api/vocabulary.ts';
import { REGION_ONLY_TAG_GROUPS } from '../model/tags.ts';
import { TagGroupsField } from './TagGroupsField.tsx';

export function RegionField({ vocabulary, tags, onChange }: { vocabulary: Vocabulary; tags: string[] | undefined; onChange: (tags: string[]) => void }) {
  return (
    <TagGroupsField label="Area (region tag, used by the in-game area toggles)" vocabulary={vocabulary} tags={tags ?? []} groups={REGION_ONLY_TAG_GROUPS} onChange={onChange} />
  );
}
