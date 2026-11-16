import { Group, ScrollArea, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useMemo, useState, type ReactNode } from 'react';
import type { ImageUsage } from '../api/images.ts';
import classes from './ImageBrowser.module.css';
import { fileUrl, folderCounts, imageFileName, imageFolder } from './imagePaths.ts';

export type UsageFilter = 'all' | 'unused' | 'notAtlas';

const USAGE_FILTER_OPTIONS: { value: UsageFilter; label: string }[] = [
  { value: 'all', label: 'All images' },
  { value: 'unused', label: 'Unused anywhere' },
  { value: 'notAtlas', label: 'Not used in the Atlas Tree' },
];

interface ImageBrowserProps {
  paths: string[];
  selectedPath?: string | null;
  onSelect: (path: string) => void;
  height?: number | string;
  usage?: ImageUsage;
  initialFolder?: string;
  initialUsageFilter?: UsageFilter;
  intro?: ReactNode;
}

function matchesUsageFilter(usage: ImageUsage | undefined, path: string, filter: UsageFilter): boolean {
  const uses = usage?.[path] ?? [];
  if (filter === 'unused') {
    return uses.length === 0;
  }
  if (filter === 'notAtlas') {
    return !uses.some((use) => use.atlas);
  }
  return true;
}

function usageSummary(usage: ImageUsage, path: string): string {
  const uses = usage[path] ?? [];
  if (uses.length === 0) {
    return 'unused';
  }
  const atlasMark = uses.some((use) => use.atlas) ? ', Atlas' : '';
  return `${uses.length} ${uses.length === 1 ? 'use' : 'uses'}${atlasMark}`;
}

export function ImageBrowser({ paths, selectedPath, onSelect, height = 480, usage, initialFolder = '', initialUsageFilter = 'all', intro }: ImageBrowserProps) {
  const [folder, setFolder] = useState(initialFolder);
  const [filterText, setFilterText] = useState('');
  const [usageFilter, setUsageFilter] = useState<UsageFilter>(initialUsageFilter);

  const folderOptions = useMemo(() => {
    const counts = folderCounts(paths);
    const emptyInitialFolder = initialFolder && !counts.some(({ folder: name }) => name === initialFolder) ? [{ folder: initialFolder, count: 0 }] : [];
    return [
      { value: '', label: `All folders (${paths.length})` },
      ...[...emptyInitialFolder, ...counts].map(({ folder: name, count }) => ({ value: name, label: `${name || '(root)'} (${count})` })),
    ];
  }, [paths, initialFolder]);

  const shownPaths = useMemo(() => {
    const needle = filterText.trim().toLowerCase();
    return paths.filter(
      (path) =>
        (!folder || imageFolder(path) === folder) && (!needle || path.toLowerCase().includes(needle)) && (!usage || matchesUsageFilter(usage, path, usageFilter)),
    );
  }, [paths, folder, filterText, usage, usageFilter]);

  return (
    <Stack gap="xs">
      {intro}
      <Group grow>
        <Select label="Folder" data={folderOptions} value={folder} onChange={(value) => setFolder(value ?? '')} searchable allowDeselect={false} />
        {usage ? (
          <Select label="Show" data={USAGE_FILTER_OPTIONS} value={usageFilter} onChange={(value) => setUsageFilter((value as UsageFilter | null) ?? 'all')} allowDeselect={false} />
        ) : null}
        <TextInput
          label="Filter by path"
          placeholder="Filter by path..."
          leftSection={<IconSearch size={14} />}
          value={filterText}
          onChange={(event) => setFilterText(event.currentTarget.value)}
        />
      </Group>
      <ScrollArea h={height} type="auto">
        {shownPaths.length === 0 ? (
          <Text c="dimmed" size="sm">
            No images match.
          </Text>
        ) : (
          <div className={classes.grid}>
            {shownPaths.map((path) => (
              <button
                type="button"
                key={path}
                title={path}
                className={`${classes.tile} ${path === selectedPath ? classes.tileSelected : ''}`}
                onClick={() => onSelect(path)}
              >
                <img className={classes.thumbnail} src={fileUrl(path)} alt={imageFileName(path)} loading="lazy" />
                <span className={classes.name}>{imageFileName(path)}</span>
                {usage ? <span className={(usage[path]?.length ?? 0) === 0 ? classes.unused : classes.usage}>{usageSummary(usage, path)}</span> : null}
              </button>
            ))}
          </div>
        )}
      </ScrollArea>
      <Text size="xs" c="dimmed">
        {shownPaths.length} of {paths.length} images
      </Text>
    </Stack>
  );
}
