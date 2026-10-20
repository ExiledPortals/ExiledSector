import { Avatar, Group, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { fetchDescription, stringsRevision } from '../../../api/describe.ts';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { TooltipEffects } from '../../../shared/TooltipBody.tsx';
import { mergeDescriptions, type LabelledDescription, type TooltipContent } from '../../../shared/tooltipContent.ts';
import { HULL_SIZE_OPTIONS, type SkillType } from '../../tree/model/types.ts';
import { DOCTRINE_POOL_TIERS, listEntries, type DoctrineArchive, type DoctrinePoolKey } from '../model/doctrines.ts';

const RETRY_DELAY_MS = 1500;
const ICON_SIZE = 56;
const POOL_LABELS: Record<DoctrinePoolKey, string> = {
  notables: 'Replacement notables',
  keystones: 'Replacement keystones',
  smallNodes: 'Replacement small nodes',
};

export interface DoctrineTypeSource {
  typesById: ReadonlyMap<string, SkillType>;
  typesJson: string;
  typesKey: string;
  onIconPicked: (typeId: string, iconPath: string) => void;
}

type TypeTooltip = { content: TooltipContent } | { note: string; pending: boolean };

function useTypeTooltip(typeId: string, source: DoctrineTypeSource) {
  return useQuery({
    queryKey: ['doctrineTypeTooltip', source.typesKey, typeId, stringsRevision()],
    queryFn: async (): Promise<TypeTooltip> => {
      const entries: LabelledDescription[] = [];
      for (const size of HULL_SIZE_OPTIONS) {
        const described = await fetchDescription({ typesJson: source.typesJson, typeId, hullSize: size.value });
        if (described.pending || described.error) {
          return { note: described.message || described.error || 'Preparing in-game descriptions...', pending: Boolean(described.pending) };
        }
        entries.push({ label: size.label, described });
      }
      return { content: mergeDescriptions(entries) };
    },
    refetchInterval: (query) => (query.state.data && 'pending' in query.state.data && query.state.data.pending ? RETRY_DELAY_MS : false),
    staleTime: Infinity,
  });
}

function TypeEffects({ typeId, source }: { typeId: string; source: DoctrineTypeSource }) {
  const tooltip = useTypeTooltip(typeId, source);
  if (!tooltip.data) {
    return (
      <Text size="xs" c="dimmed">
        Loading the tooltip...
      </Text>
    );
  }
  if ('note' in tooltip.data) {
    return (
      <Text size="xs" c="dimmed">
        {tooltip.data.note}
      </Text>
    );
  }
  return <TooltipEffects content={tooltip.data.content} />;
}

function sharedIconNames(type: SkillType, listedTypes: SkillType[]): string[] {
  return listedTypes.filter((other) => other.id !== type.id && other.icon && other.icon === type.icon).map((other) => other.name || other.id);
}

function ReplacementRow({ typeId, source, listedTypes }: { typeId: string; source: DoctrineTypeSource; listedTypes: SkillType[] }) {
  const type = source.typesById.get(typeId);
  if (!type) {
    return (
      <Text size="sm" c="#fc6300">
        {typeId} is not in skill_types.json
      </Text>
    );
  }
  const sharedWith = sharedIconNames(type, listedTypes);
  return (
    <Group align="flex-start" wrap="nowrap" gap="sm">
      <Avatar src={fileUrl(type.icon)} alt={`${type.name || type.id} icon`} radius="xl" size={ICON_SIZE} />
      <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
        <Group gap="xs" align="baseline">
          <Text size="sm" fw={600} c="white">
            {type.name || type.id}
          </Text>
          <Text size="xs" c="dimmed">
            {type.id}
          </Text>
        </Group>
        <TypeEffects typeId={type.id} source={source} />
      </Stack>
      <Stack gap={4} w={260} align="flex-end" style={{ flexShrink: 0 }}>
        <ImagePickButton
          size="xs"
          label="Change image"
          aria-label={`Change image of ${type.name || type.id}`}
          onPicked={(iconPath) => source.onIconPicked(type.id, iconPath)}
        />
        <Text size="xs" c="dimmed" maw={260} truncate>
          {type.icon || 'No image'}
        </Text>
        {sharedWith.length > 0 && (
          <Text size="xs" c="#fc6300">
            Same image as {sharedWith.join(', ')}
          </Text>
        )}
      </Stack>
    </Group>
  );
}

export function DoctrineReplacementPools({ archive, source }: { archive: DoctrineArchive; source: DoctrineTypeSource }) {
  const pools = (Object.keys(DOCTRINE_POOL_TIERS) as DoctrinePoolKey[])
    .map((poolKey) => ({ poolKey, typeIds: listEntries(archive[poolKey]) }))
    .filter(({ typeIds }) => typeIds.length > 0);
  const listedTypes = pools.flatMap(({ typeIds }) => typeIds.map((typeId) => source.typesById.get(typeId))).filter((type): type is SkillType => type !== undefined);
  return (
    <>
      {pools.map(({ poolKey, typeIds }) => (
        <Stack key={poolKey} gap="xs">
          <Group gap="xs" align="baseline">
            <Text size="sm" fw={600} c="white">
              {POOL_LABELS[poolKey]}
            </Text>
            <Text size="xs" c="dimmed">
              {typeIds.length} {typeIds.length === 1 ? 'type' : 'types'}
            </Text>
          </Group>
          {typeIds.map((typeId) => (
            <ReplacementRow key={typeId} typeId={typeId} source={source} listedTypes={listedTypes} />
          ))}
        </Stack>
      ))}
    </>
  );
}
