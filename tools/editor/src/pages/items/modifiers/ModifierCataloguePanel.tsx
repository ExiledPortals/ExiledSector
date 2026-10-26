import { Badge, Box, Button, Group, MultiSelect, ScrollArea, Select, Stack, Text, TextInput, UnstyledButton } from '@mantine/core';
import { IconPlus, IconSearch } from '@tabler/icons-react';
import { memo, useState } from 'react';
import { DescriptionNote, GameLines } from '../components/GameText.tsx';
import { ListDetail } from '../components/ItemFields.tsx';
import { displayItemType, modifierLinesOf, plainText, useLineDescriptions, type LineDescriptions } from '../describedLines.ts';
import type { ItemsPanelProps } from '../itemsDocument.ts';
import { ITEM_TYPE_LABELS } from '../model/definitions.ts';
import type { ItemFamily } from '../model/itemFamily.ts';
import { isUsed } from '../model/modifierUsage.ts';
import { familyItemTypes, MODIFIER_SLOT_LABELS, MODIFIER_SLOTS, newModifier, type ItemModifier, type ModifierSlot } from '../model/modifiers.ts';
import { chartModifierUsage, modifiersOf, shipModifierUsage } from '../model/workspace.ts';
import { ModifierForm } from './ModifierForm.tsx';

const CATALOGUE_LIST_WIDTH = 480;

interface CatalogueRowProps {
  modifier: ItemModifier;
  index: number;
  family: ItemFamily;
  selected: boolean;
  used: boolean;
  descriptions: LineDescriptions;
  onSelect: (index: number) => void;
}

const CatalogueRow = memo(
  function CatalogueRow({ modifier, index, family, selected, used, descriptions, onSelect }: CatalogueRowProps) {
    const itemTypes = family === 'chart' ? '' : ` - ${modifier.itemTypes.map((itemType) => ITEM_TYPE_LABELS[itemType] ?? itemType).join(', ')}`;
    return (
      <UnstyledButton onClick={() => onSelect(index)} w="100%" mb={6} aria-label={`Select ${modifier.id}`} aria-pressed={selected}>
        <GameLines lines={modifier.lines} itemType={displayItemType(modifier, family)} descriptions={descriptions} selected={selected} />
        <Group gap={6} wrap="nowrap" mt={2}>
          <Text size="xs" c="dimmed" truncate>
            {MODIFIER_SLOT_LABELS[modifier.slot as ModifierSlot] ?? modifier.slot}
            {itemTypes}
            {modifier.name ? ` - ${modifier.name}` : ''} - {modifier.id}
          </Text>
          {!used && (
            <Badge size="xs" variant="light" color="gray">
              unused
            </Badge>
          )}
        </Group>
      </UnstyledButton>
    );
  },
  (previous, next) =>
    previous.modifier === next.modifier &&
    previous.index === next.index &&
    previous.selected === next.selected &&
    previous.used === next.used &&
    previous.descriptions.version === next.descriptions.version &&
    previous.onSelect === next.onSelect,
);

export function ModifierCataloguePanel({ document, model, vocabulary, family }: ItemsPanelProps & { family: ItemFamily }) {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [slotFilter, setSlotFilter] = useState('');
  const [itemTypeFilter, setItemTypeFilter] = useState<string[]>([]);
  const [searchText, setSearchText] = useState('');
  const modifiers = modifiersOf(model, family);
  const descriptions = useLineDescriptions(modifierLinesOf(modifiers, (modifier) => displayItemType(modifier, family)));
  const usage = family === 'chart' ? chartModifierUsage(model) : shipModifierUsage(model);
  const needle = searchText.trim().toLowerCase();
  const searchTextOf = (modifier: ItemModifier) =>
    `${modifier.lines.map((line) => plainText(descriptions.markupOf(line, displayItemType(modifier, family)))).join(' ')} ${modifier.name} ${modifier.id}`.toLowerCase();
  const shown = modifiers
    .map((modifier, index) => ({ modifier, index }))
    .filter(({ modifier }) => !slotFilter || modifier.slot === slotFilter)
    .filter(({ modifier }) => itemTypeFilter.length === 0 || modifier.itemTypes.some((itemType) => itemTypeFilter.includes(itemType)))
    .filter(({ modifier }) => !needle || searchTextOf(modifier).includes(needle));
  const selected = modifiers[selectedIndex];

  const addModifier = () => {
    const slot = (slotFilter || 'prefix') as ModifierSlot;
    const otherIds = new Set(modifiersOf(model, family === 'chart' ? 'ship' : 'chart').map((modifier) => modifier.id));
    document.update((draft) => {
      const catalogue = family === 'chart' ? draft.chartModifiers : draft.shipModifiers;
      catalogue.modifiers.push(newModifier(catalogue.modifiers, slot, vocabulary, family, otherIds));
    });
    setSelectedIndex(modifiers.length);
    setSearchText('');
    setItemTypeFilter([]);
  };

  const slotOptions = [
    { value: '', label: `All slots (${modifiers.length})` },
    ...MODIFIER_SLOTS.map((slot) => ({ value: slot, label: `${MODIFIER_SLOT_LABELS[slot]} (${modifiers.filter((modifier) => modifier.slot === slot).length})` })),
  ];

  return (
    <ListDetail
      listWidth={CATALOGUE_LIST_WIDTH}
      list={
        <>
          <Group gap="xs" wrap="nowrap">
            <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addModifier}>
              New modifier
            </Button>
            <Select size="xs" style={{ flex: 1 }} data={slotOptions} value={slotFilter} onChange={(value) => setSlotFilter(value ?? '')} allowDeselect={false} aria-label="Slot filter" />
          </Group>
          {family === 'ship' && (
            <MultiSelect
              size="xs"
              placeholder="Every item type"
              data={familyItemTypes(family, vocabulary).map((itemType) => ({ value: itemType, label: ITEM_TYPE_LABELS[itemType] ?? itemType }))}
              value={itemTypeFilter}
              onChange={setItemTypeFilter}
              aria-label="Item type filter"
              clearable
            />
          )}
          <TextInput
            placeholder="Search the in-game text, name or id"
            aria-label="Search modifiers"
            leftSection={<IconSearch size={14} />}
            value={searchText}
            onChange={(event) => setSearchText(event.currentTarget.value)}
          />
          <DescriptionNote descriptions={descriptions} />
          <ScrollArea style={{ flex: 1 }} type="auto">
            <Box pr="sm">
              {shown.length === 0 ? (
                <Text size="sm" c="dimmed">
                  No modifiers match.
                </Text>
              ) : (
                shown.map(({ modifier, index }) => (
                  <CatalogueRow
                    key={index}
                    modifier={modifier}
                    index={index}
                    family={family}
                    selected={index === selectedIndex}
                    used={isUsed(usage.get(modifier.id))}
                    descriptions={descriptions}
                    onSelect={setSelectedIndex}
                  />
                ))
              )}
            </Box>
          </ScrollArea>
        </>
      }
      detail={
        selected ? (
          <Stack gap="sm">
            <ModifierForm
              key={`${family}-${selectedIndex}`}
              document={document}
              model={model}
              vocabulary={vocabulary}
              family={family}
              modifier={selected}
              index={selectedIndex}
              usage={usage.get(selected.id)}
              descriptions={descriptions}
              onSelect={setSelectedIndex}
            />
          </Stack>
        ) : (
          <Text c="dimmed">Select a modifier, or add a new one.</Text>
        )
      }
    />
  );
}
