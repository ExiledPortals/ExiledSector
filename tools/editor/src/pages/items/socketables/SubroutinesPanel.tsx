import { Button, Divider, Group, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import { useCallback, useState } from 'react';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DescriptionNote } from '../components/GameText.tsx';
import { DescriptionField, IconField, IdField, ItemList, ListDetail, NameAndRarity, UniqueFields, type ListedItem } from '../components/ItemFields.tsx';
import { ModifierPool } from '../components/ModifierPool.tsx';
import { modifierLinesOf, useLineDescriptions, type LineDescriptions } from '../describedLines.ts';
import type { ItemsPanelProps } from '../itemsDocument.ts';
import {
  editablePool,
  hasModspecCacheFields,
  isModspecCacheSubroutine,
  isTrueText,
  isUniqueSubroutine,
  itemTypeOfKind,
  newItemDefinition,
  SUBROUTINE_KIND,
  SUBROUTINE_TYPE,
  UNIQUE_LINES_GROUP,
  UNIQUE_SUBROUTINE_TYPE,
  uniqueItemId,
  type ItemDefinition,
} from '../model/definitions.ts';
import { DOCTRINE_SUBROUTINE_KIND } from '../model/doctrines.ts';
import { MODIFIER_FILE_NAMES } from '../model/itemFamily.ts';
import { otherItemIds } from '../model/workspace.ts';

function isOtherSubroutine(definition: ItemDefinition): boolean {
  return itemTypeOfKind(definition.kind) === SUBROUTINE_TYPE && definition.subroutineKind !== DOCTRINE_SUBROUTINE_KIND;
}

export function SubroutinesPanel({ document, model, vocabulary }: ItemsPanelProps) {
  const [selectedIndex, setSelectedIndex] = useState(() => model.ship.definitions.findIndex(isOtherSubroutine));
  const kinds = vocabulary.subroutineKinds.filter((kind) => kind !== DOCTRINE_SUBROUTINE_KIND);
  const uniqueLineModifiers = model.shipModifiers.modifiers.filter((modifier) => modifier.itemTypes.includes(UNIQUE_SUBROUTINE_TYPE));
  const descriptions = useLineDescriptions(modifierLinesOf(uniqueLineModifiers, () => UNIQUE_SUBROUTINE_TYPE));
  const listed: ListedItem[] = model.ship.definitions
    .map((definition, index) => ({ definition, index }))
    .filter(({ definition }) => isOtherSubroutine(definition))
    .map(({ definition, index }) => ({ index, label: definition.name || definition.id, description: definition.subroutineKind || 'no kind', icon: definition.icon }));
  const selected = model.ship.definitions[selectedIndex];

  const addSubroutine = () => {
    const index = model.ship.definitions.length;
    document.update((draft) => {
      draft.ship.definitions.push(newItemDefinition(draft.ship.definitions, SUBROUTINE_KIND, draft.shipModifiers.modifiers, otherItemIds(draft, 'ship'), kinds[0] ?? ''));
    });
    setSelectedIndex(index);
  };

  return (
    <ListDetail
      list={
        <>
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addSubroutine}>
            New subroutine
          </Button>
          <ItemList items={listed} selectedIndex={selectedIndex} onSelect={setSelectedIndex} emptyText="No subroutines yet." />
        </>
      }
      detail={
        selected && isOtherSubroutine(selected) ? (
          <SubroutineForm
            key={selectedIndex}
            document={document}
            model={model}
            vocabulary={vocabulary}
            definition={selected}
            index={selectedIndex}
            kinds={kinds}
            descriptions={descriptions}
            onSelect={setSelectedIndex}
          />
        ) : (
          <Text c="dimmed">Select a subroutine, or add a new one. Doctrine Archives have their own tab.</Text>
        )
      }
    />
  );
}

interface SubroutineFormProps extends ItemsPanelProps {
  definition: ItemDefinition;
  index: number;
  kinds: string[];
  descriptions: LineDescriptions;
  onSelect: (index: number) => void;
}

function ModspecCacheFields({ definition, edit }: { definition: ItemDefinition; edit: (recipe: (draft: ItemDefinition) => void) => void }) {
  const nodeCountChances = definition.nodeCountChances ?? '';
  return (
    <>
      <Divider />
      <Group grow align="flex-start">
        <TextInput
          label="Excluded nodes"
          description="Notable node ids that never roll, separated by ;"
          value={definition.excludedNodes ?? ''}
          onChange={(event) => {
            const text = event.currentTarget.value;
            edit((draft) => void (draft.excludedNodes = text));
          }}
        />
        <TextInput
          label="Node count chances"
          description="Relative chances of rolling 1, 2, 3... nodes, separated by ;"
          placeholder="0.9;0.09;0.01"
          value={nodeCountChances}
          onChange={(event) => {
            const text = event.currentTarget.value;
            edit((draft) => void (draft.nodeCountChances = text));
          }}
          error={nodeCountChances.trim() === '' ? 'Enter at least one chance' : undefined}
        />
      </Group>
      <Group grow align="flex-start">
        <DecimalInput
          label="Fleet drop chance"
          description="Chance per 100 deployment points destroyed, from fleets of any faction"
          placeholder="never"
          value={definition.dropChance ?? ''}
          onChange={(text) => edit((draft) => void (draft.dropChance = text))}
        />
        <DecimalInput
          label="Salvage chance"
          description="Chance per salvaged site, scaled by the site's first item chance"
          placeholder="never"
          value={definition.salvageChance ?? ''}
          onChange={(text) => edit((draft) => void (draft.salvageChance = text))}
        />
      </Group>
    </>
  );
}

function SubroutineForm({ document, model, vocabulary, definition, index, kinds, descriptions, onSelect }: SubroutineFormProps) {
  const updateDocument = document.update;
  const edit = useCallback((recipe: (draft: ItemDefinition) => void) => updateDocument((draft) => recipe(draft.ship.definitions[index])), [updateDocument, index]);
  const subroutineKind = definition.subroutineKind ?? '';
  const kindOptions = subroutineKind === '' || kinds.includes(subroutineKind) ? kinds : [subroutineKind, ...kinds];
  const uniqueLines = definition.lines ?? [];
  const showsUniqueFields = isUniqueSubroutine(definition) || uniqueLines.length > 0 || Boolean(definition.source);

  const duplicate = () => {
    document.update((draft) => {
      const copy = structuredClone(definition);
      copy.id = uniqueItemId(draft.ship.definitions, `${definition.id}_copy`, otherItemIds(draft, 'ship'));
      delete copy.loadedId;
      draft.ship.definitions.splice(index + 1, 0, copy);
    });
    onSelect(index + 1);
  };

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Delete "${definition.id}"?`,
      message: 'Subroutines players already own will show as unrecognised.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => void draft.ship.definitions.splice(index, 1));
      onSelect(Math.max(0, index - 1));
    }
  };

  return (
    <Stack gap="sm" maw={1100}>
      <IconField icon={definition.icon} targetFolder="graphics/icons/socketables/" onChange={(icon) => edit((draft) => void (draft.icon = icon))} />
      <Group grow align="flex-start">
        <IdField
          id={definition.id}
          loadedId={definition.loadedId}
          noun="subroutine"
          renameNote={`Its socketable.${definition.loadedId ?? definition.id}.* keys in zh_CN.json keep the old id and must be renamed by hand.`}
          onRename={(newId) => edit((draft) => void (draft.id = newId))}
        />
        <Select
          label="Subroutine kind"
          description="Sets what the subroutine does on the skill tree"
          data={kindOptions}
          value={subroutineKind === '' ? null : subroutineKind}
          onChange={(value) => edit((draft) => void (draft.subroutineKind = value ?? ''))}
          allowDeselect={false}
          error={vocabulary.subroutineKinds.includes(subroutineKind) ? undefined : 'Pick a subroutine kind'}
        />
      </Group>
      <NameAndRarity
        name={definition.name}
        rarity={definition.rarity}
        onName={(name) => edit((draft) => void (draft.name = name))}
        onRarity={(rarity) => edit((draft) => void (draft.rarity = rarity))}
      />
      <DecimalInput
        w={320}
        label="Socket limit per ship"
        description="Most copies one ship can socket; blank or 0 for no limit"
        placeholder="no limit"
        value={definition.socketLimit ?? ''}
        onChange={(text) => edit((draft) => void (draft.socketLimit = text))}
      />
      <UniqueFields
        unique={isTrueText(definition.unique)}
        unlock={definition.unlock}
        vocabulary={vocabulary}
        uniqueLabel="Unique - shown with the name above in orange"
        unlockLabel="Drop condition (uniques never drop before player level 15)"
        onUnique={(checked) =>
          edit((draft) => {
            draft.unique = checked ? 'true' : '';
            if (!checked) {
              draft.unlock = '';
            }
          })
        }
        onUnlock={(unlock) => edit((draft) => void (draft.unlock = unlock))}
      />
      <DescriptionField description={definition.description} onChange={(description) => edit((draft) => void (draft.description = description))} />
      {(isModspecCacheSubroutine(definition) || hasModspecCacheFields(definition)) && <ModspecCacheFields definition={definition} edit={edit} />}
      {showsUniqueFields && (
        <>
          <Divider />
          <TextInput
            label="Drop source"
            description="faction:FACTION_ID - flagships of that faction's fleets can carry it; site:SALVAGE_SITE_ID - that salvage site's unique roll. Separate several with ;"
            value={definition.source ?? ''}
            onChange={(event) => {
              const text = event.currentTarget.value;
              edit((draft) => void (draft.source = text));
            }}
          />
          <DescriptionNote descriptions={descriptions} />
          <ModifierPool
            title={UNIQUE_LINES_GROUP.label}
            hint="every line always rolls once; values are edited in Hull Items > Modifiers"
            entries={uniqueLines}
            catalogue={model.shipModifiers.modifiers}
            slot={UNIQUE_LINES_GROUP.singular}
            itemType={UNIQUE_SUBROUTINE_TYPE}
            descriptions={descriptions}
            weighted={false}
            catalogueFileName={MODIFIER_FILE_NAMES.ship}
            onAdd={(modifierId) => edit((draft) => void editablePool(draft, UNIQUE_LINES_GROUP.key).push({ modifierId, weight: '' }))}
            onRemove={(entryIndex) => edit((draft) => void editablePool(draft, UNIQUE_LINES_GROUP.key).splice(entryIndex, 1))}
          />
        </>
      )}
      <Divider />
      <Group>
        <Button variant="default" leftSection={<IconCopy size={14} />} onClick={duplicate}>
          Duplicate
        </Button>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this subroutine
        </Button>
      </Group>
    </Stack>
  );
}
