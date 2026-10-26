import { Badge, Group, SegmentedControl, Select, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { fetchDataFiles } from '../../api/client.ts';
import { fetchDescription } from '../../api/describe.ts';
import { TooltipBody, TooltipNote } from '../../shared/TooltipBody.tsx';
import { mergeDescriptions, type LabelledDescription, type TooltipContent } from '../../shared/tooltipContent.ts';
import { TOOLTIP_PANEL_CLASS } from '../../shared/tooltipMarkup.ts';
import { skillTypesFileObject } from '../tree/model/serialization.ts';
import { HULL_SIZE_OPTIONS } from '../tree/model/types.ts';
import { useTreeStore } from '../tree/state/treeStore.ts';
import classes from './TextPage.module.css';

const DEBOUNCE_MS = 300;
const RETRY_DELAY_MS = 2000;
const ALL_SIZES = 'ALL';

type PreviewState = { kind: 'note'; message: string } | { kind: 'content'; content: TooltipContent; keys: string[] };

interface PreviewTypes {
  typesJson: string;
  options: { value: string; label: string }[];
}

let rememberedTypeId: string | null = null;
let rememberedHullSize = ALL_SIZES;

function typeOptions(types: { id: string; name?: string }[]): { value: string; label: string }[] {
  return types
    .map((type) => ({ value: type.id, label: type.name ? `${type.name} (${type.id})` : type.id }))
    .sort((first, second) => first.label.localeCompare(second.label));
}

function usePreviewTypes(): PreviewTypes | null {
  const treeTypes = useTreeStore((state) => (state.load.status === 'ready' ? state.document.types : null));
  const diskTypes = useQuery({
    queryKey: ['text-preview-types'],
    queryFn: async () => (await fetchDataFiles(['types'])).types.content,
    enabled: treeTypes === null,
  });
  return useMemo(() => {
    if (treeTypes) {
      return { typesJson: JSON.stringify(skillTypesFileObject(treeTypes)), options: typeOptions(treeTypes) };
    }
    if (!diskTypes.data) {
      return null;
    }
    const parsed = JSON.parse(diskTypes.data) as { skillTypes: { id: string; name?: string }[] };
    return { typesJson: JSON.stringify(parsed), options: typeOptions(parsed.skillTypes) };
  }, [treeTypes, diskTypes.data]);
}

interface TooltipPreviewProps {
  strings: Record<string, string>;
  selectedKey: string | null;
  editedKeys: Set<string>;
  onSelectKey: (key: string) => void;
}

export function TooltipPreview({ strings, selectedKey, editedKeys, onSelectKey }: TooltipPreviewProps) {
  const types = usePreviewTypes();
  const [typeId, setTypeId] = useState<string | null>(rememberedTypeId);
  const [hullSize, setHullSize] = useState(rememberedHullSize);
  const [preview, setPreview] = useState<PreviewState | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const typesJson = types?.typesJson ?? null;
  const shownTypeId = typeId ?? types?.options[0]?.value ?? null;

  useEffect(() => {
    if (!typesJson || !shownTypeId) {
      return;
    }
    let cancelled = false;
    let retryTimer = 0;
    const describeNow = async () => {
      const sizes = hullSize === ALL_SIZES ? HULL_SIZE_OPTIONS : HULL_SIZE_OPTIONS.filter((option) => option.value === hullSize);
      const entries: LabelledDescription[] = [];
      const keys = new Set<string>();
      for (const size of sizes) {
        const described = await fetchDescription({ typesJson, typeId: shownTypeId, hullSize: size.value, strings });
        if (cancelled) {
          return;
        }
        if (described.pending || described.error) {
          setPreview({ kind: 'note', message: described.message || described.error || 'Preparing in-game descriptions...' });
          if (described.pending) {
            retryTimer = window.setTimeout(() => setRetryCount((count) => count + 1), RETRY_DELAY_MS);
          }
          return;
        }
        described.keys?.forEach((key) => keys.add(key));
        entries.push({ label: size.label, described });
      }
      setPreview({ kind: 'content', content: mergeDescriptions(entries), keys: [...keys].sort((first, second) => first.localeCompare(second)) });
    };
    const timer = window.setTimeout(() => void describeNow(), DEBOUNCE_MS);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      window.clearTimeout(retryTimer);
    };
  }, [typesJson, shownTypeId, hullSize, strings, retryCount]);

  return (
    <Stack gap="sm">
      <Group gap="xs" align="flex-end" wrap="wrap">
        <Select
          style={{ flex: 1, minWidth: 240 }}
          label="Preview a node type"
          searchable
          data={types?.options ?? []}
          value={shownTypeId}
          onChange={(value) => {
            rememberedTypeId = value;
            setTypeId(value);
          }}
          nothingFoundMessage="No type matches"
        />
        <SegmentedControl
          size="xs"
          value={hullSize}
          onChange={(value) => {
            rememberedHullSize = value;
            setHullSize(value);
          }}
          data={[{ value: ALL_SIZES, label: 'All sizes' }, ...HULL_SIZE_OPTIONS]}
        />
      </Group>
      <div className={TOOLTIP_PANEL_CLASS} data-testid="tooltip-preview">
        {preview === null ? <TooltipNote>Loading the preview...</TooltipNote> : null}
        {preview?.kind === 'note' ? <TooltipNote>{preview.message}</TooltipNote> : null}
        {preview?.kind === 'content' ? <TooltipBody content={preview.content} showDetails /> : null}
      </div>
      {preview?.kind === 'content' ? (
        <Stack gap={4}>
          <Text size="xs" c="dimmed">
            Strings used by this tooltip (click one to edit it). Type names, flavour and effect text come from the type itself; edit those on the Skill tree page.
          </Text>
          <Group gap={6}>
            {preview.keys.map((key) => (
              <Badge
                key={key}
                className={classes.keyChip}
                variant={key === selectedKey ? 'filled' : 'light'}
                color={editedKeys.has(key) ? 'yellow' : key in strings ? 'blue' : 'red'}
                onClick={() => onSelectKey(key)}
                component="button"
              >
                {key}
              </Badge>
            ))}
          </Group>
        </Stack>
      ) : null}
    </Stack>
  );
}
