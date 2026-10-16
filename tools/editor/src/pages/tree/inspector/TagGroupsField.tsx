import { Input, MultiSelect, Select, Stack } from '@mantine/core';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { replaceGroupTags, TAG_GROUPS, tagGroupOf, tagsOfGroup, type TagGroupKey } from '../model/tags.ts';

interface TagGroupsFieldProps {
  label: string;
  vocabulary: Vocabulary;
  tags: string[];
  groups: TagGroupKey[];
  onChange: (tags: string[]) => void;
}

export function TagGroupsField({ label, vocabulary, tags, groups, onChange }: TagGroupsFieldProps) {
  const unknownTags = tags.filter((tag) => !vocabulary.allTags.includes(tag));
  const change = (group: TagGroupKey | 'unknown', chosen: string[]) => onChange(replaceGroupTags(vocabulary, tags, group, chosen));

  return (
    <Input.Wrapper label={label}>
      <Stack gap={4} mt={2}>
        {TAG_GROUPS.map(({ key, label: groupLabel }) => {
          const inScope = groups.includes(key);
          const active = tags.filter((tag) => vocabulary.allTags.includes(tag) && tagGroupOf(vocabulary, tag) === key);
          const options = inScope ? tagsOfGroup(vocabulary, key) : active;
          if (options.length === 0) {
            return null;
          }
          const error = inScope ? undefined : 'Not expected here - remove';
          if (key === 'region' && inScope) {
            return (
              <Select
                key={key}
                size="xs"
                placeholder={`${groupLabel}: none`}
                data={options}
                value={active[0] ?? null}
                onChange={(value) => change(key, value ? [value] : [])}
                searchable
                clearable
                error={active.length > 1 ? `Several region tags: ${active.join(', ')}` : undefined}
                leftSection={<span style={{ fontSize: 10, opacity: 0.6 }}>{groupLabel}</span>}
                leftSectionWidth={56}
              />
            );
          }
          return (
            <MultiSelect
              key={key}
              size="xs"
              placeholder={active.length ? undefined : `${groupLabel}: none`}
              data={options}
              value={active}
              onChange={(chosen) => change(key, chosen)}
              searchable
              hidePickedOptions={false}
              error={error}
              leftSection={<span style={{ fontSize: 10, opacity: 0.6 }}>{groupLabel}</span>}
              leftSectionWidth={key === 'requirement' ? 80 : 56}
            />
          );
        })}
        {unknownTags.length > 0 ? (
          <MultiSelect size="xs" data={unknownTags} value={unknownTags} onChange={(chosen) => change('unknown', chosen)} error="Unknown tags - remove" />
        ) : null}
      </Stack>
    </Input.Wrapper>
  );
}
