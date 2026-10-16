import { Group, ScrollArea, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import classes from './ImageBrowser.module.css';
import { fileUrl, folderCounts, imageFileName, imageFolder } from './imagePaths.ts';

interface ImageBrowserProps {
  paths: string[];
  selectedPath?: string | null;
  onSelect: (path: string) => void;
  height?: number | string;
}

export function ImageBrowser({ paths, selectedPath, onSelect, height = 480 }: ImageBrowserProps) {
  const [folder, setFolder] = useState('');
  const [filterText, setFilterText] = useState('');

  const folderOptions = useMemo(
    () => [
      { value: '', label: `All folders (${paths.length})` },
      ...folderCounts(paths).map(({ folder: name, count }) => ({ value: name, label: `${name || '(root)'} (${count})` })),
    ],
    [paths],
  );

  const shownPaths = useMemo(() => {
    const needle = filterText.trim().toLowerCase();
    return paths.filter((path) => (!folder || imageFolder(path) === folder) && (!needle || path.toLowerCase().includes(needle)));
  }, [paths, folder, filterText]);

  return (
    <Stack gap="xs">
      <Group grow>
        <Select label="Folder" data={folderOptions} value={folder} onChange={(value) => setFolder(value ?? '')} searchable allowDeselect={false} />
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
