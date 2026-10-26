import { Avatar, Box, Checkbox, Divider, Group, NavLink, ScrollArea, Select, Stack, Text, Textarea, TextInput } from '@mantine/core';
import { useEffect, useState, type ReactNode } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { unlockOptions } from '../itemOptions.ts';

const LIST_WIDTH = 300;

interface ListDetailProps {
  list: ReactNode;
  detail: ReactNode;
  listWidth?: number;
}

export function ListDetail({ list, detail, listWidth = LIST_WIDTH }: ListDetailProps) {
  return (
    <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Stack w={listWidth} gap="xs" p="xs" style={{ flexShrink: 0, minHeight: 0 }}>
        {list}
      </Stack>
      <Divider orientation="vertical" />
      <ScrollArea style={{ flex: 1 }} type="auto">
        <Box p="md">{detail}</Box>
      </ScrollArea>
    </Group>
  );
}

export interface ListedItem {
  index: number;
  label: string;
  description: string;
  icon?: string;
}

interface ItemListProps {
  items: ListedItem[];
  selectedIndex: number;
  onSelect: (index: number) => void;
  emptyText: string;
}

export function ItemList({ items, selectedIndex, onSelect, emptyText }: ItemListProps) {
  return (
    <ScrollArea style={{ flex: 1 }} type="auto">
      {items.length === 0 ? (
        <Text size="sm" c="dimmed">
          {emptyText}
        </Text>
      ) : (
        items.map((item) => (
          <NavLink
            key={item.index}
            active={item.index === selectedIndex}
            onClick={() => onSelect(item.index)}
            label={item.label}
            description={item.description}
            leftSection={item.icon === undefined ? undefined : <Avatar src={fileUrl(item.icon)} radius="sm" size={32} />}
          />
        ))
      )}
    </ScrollArea>
  );
}

export function IconField({ icon, targetFolder, onChange }: { icon: string; targetFolder: string; onChange: (icon: string) => void }) {
  return (
    <Group align="flex-end" wrap="nowrap">
      <Avatar src={fileUrl(icon)} radius="sm" size={64} />
      <TextInput style={{ flex: 1 }} label="Icon path" value={icon} onChange={(event) => onChange(event.currentTarget.value)} />
      <ImagePickButton targetFolder={targetFolder} onPicked={onChange} />
    </Group>
  );
}

interface IdFieldProps {
  id: string;
  loadedId: string | undefined;
  noun: string;
  renameNote: string;
  onRename: (newId: string) => void;
}

export function IdField({ id, loadedId, noun, renameNote, onRename }: IdFieldProps) {
  const [idText, setIdText] = useState(id);
  useEffect(() => setIdText(id), [id]);
  const commit = async () => {
    const newId = idText.trim();
    if (newId === id) {
      return;
    }
    if (loadedId && id === loadedId) {
      const rename = await confirmAction({
        title: `Rename "${loadedId}" to "${newId}"?`,
        message: `Saved games store this id on every ${noun} players own. Once it has shipped, add a row to item_id_aliases.csv mapping the old id to the new one, or those items turn into unknown items. ${renameNote}`,
        confirmLabel: 'Rename',
        danger: true,
      });
      if (!rename) {
        setIdText(id);
        return;
      }
    }
    onRename(newId);
  };
  return (
    <TextInput
      label={loadedId ? `Id (saved games store it - renaming orphans owned ${noun}s)` : 'Id'}
      value={idText}
      onChange={(event) => setIdText(event.currentTarget.value)}
      onBlur={() => void commit()}
      onKeyDown={(event) => {
        if (event.key === 'Enter') {
          event.currentTarget.blur();
        }
      }}
    />
  );
}

interface UniqueFieldsProps {
  unique: boolean;
  unlock: string;
  vocabulary: Vocabulary;
  uniqueLabel: string;
  unlockLabel: string;
  onUnique: (unique: boolean) => void;
  onUnlock: (unlock: string) => void;
}

export function UniqueFields({ unique, unlock, vocabulary, uniqueLabel, unlockLabel, onUnique, onUnlock }: UniqueFieldsProps) {
  return (
    <>
      <Checkbox label={uniqueLabel} checked={unique} onChange={(event) => onUnique(event.currentTarget.checked)} />
      {unique && (
        <Select
          label={unlockLabel}
          data={unlockOptions(unlock, vocabulary, false)}
          value={unlock}
          onChange={(value) => onUnlock(value ?? '')}
          allowDeselect={false}
          searchable
        />
      )}
    </>
  );
}

export function NameAndRarity({ name, rarity, onName, onRarity }: { name: string; rarity: string; onName: (name: string) => void; onRarity: (rarity: string) => void }) {
  return (
    <Group grow align="flex-start">
      <TextInput label="Name" value={name} onChange={(event) => onName(event.currentTarget.value)} />
      <DecimalInput label="Rarity (loot weight)" value={rarity} onChange={onRarity} />
    </Group>
  );
}

export function DescriptionField({ description, onChange }: { description: string; onChange: (description: string) => void }) {
  return <Textarea label="Tooltip description (optional)" autosize minRows={3} value={description} onChange={(event) => onChange(event.currentTarget.value)} />;
}
