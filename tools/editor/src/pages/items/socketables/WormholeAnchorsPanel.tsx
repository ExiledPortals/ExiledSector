import { Button, Checkbox, ColorInput, Divider, Group, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useCallback, useState } from 'react';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DescriptionField, IconField, IdField, ItemList, ListDetail, NameAndRarity, type ListedItem } from '../components/ItemFields.tsx';
import { unlockOptions } from '../itemOptions.ts';
import type { ItemsPanelProps } from '../itemsDocument.ts';
import { anchorVolumes, exitCandidates, newWormholeAnchor, type WormholeAnchor } from '../model/anchors.ts';
import { otherItemIds } from '../model/workspace.ts';

function withCurrent(options: string[], current: string): string[] {
  return current === '' || options.includes(current) ? options : [current, ...options];
}

export function WormholeAnchorsPanel({ document, model, vocabulary }: ItemsPanelProps) {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const listed: ListedItem[] = model.anchors.anchors.map((anchor, index) => ({
    index,
    label: anchor.name || anchor.id,
    description: `${anchor.volume || 'no volume'} - ${anchor.exitNode || 'no exit'}`,
    icon: anchor.icon,
  }));
  const selected = model.anchors.anchors[selectedIndex];

  const addAnchor = () => {
    const index = model.anchors.anchors.length;
    document.update((draft) => {
      draft.anchors.anchors.push(newWormholeAnchor(draft.anchors.anchors, vocabulary, draft.nodeFacts, otherItemIds(draft, 'anchor')));
    });
    setSelectedIndex(index);
  };

  return (
    <ListDetail
      list={
        <>
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addAnchor}>
            New anchor
          </Button>
          <Text size="xs" c="dimmed">
            Each tree volume has one anchor, whose exit node must be a wormhole exit node in that volume. Anchors drop from fleets of the factions mapped to their volume in npc_faction_volumes.csv and of any extra drop factions.
          </Text>
          <ItemList items={listed} selectedIndex={selectedIndex} onSelect={setSelectedIndex} emptyText="No anchors yet." />
        </>
      }
      detail={
        selected ? (
          <AnchorForm key={selectedIndex} document={document} model={model} vocabulary={vocabulary} anchor={selected} index={selectedIndex} onSelect={setSelectedIndex} />
        ) : (
          <Text c="dimmed">Select an anchor, or add a new one.</Text>
        )
      }
    />
  );
}

interface AnchorFormProps extends ItemsPanelProps {
  anchor: WormholeAnchor;
  index: number;
  onSelect: (index: number) => void;
}

function AnchorForm({ document, model, vocabulary, anchor, index, onSelect }: AnchorFormProps) {
  const updateDocument = document.update;
  const edit = useCallback((recipe: (draft: WormholeAnchor) => void) => updateDocument((draft) => recipe(draft.anchors.anchors[index])), [updateDocument, index]);

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Delete "${anchor.id}"?`,
      message: 'Anchors players already own will show as unrecognised, and nothing will lead to this volume.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => void draft.anchors.anchors.splice(index, 1));
      onSelect(Math.max(0, index - 1));
    }
  };

  const volumes = withCurrent(anchorVolumes(vocabulary), anchor.volume);
  const exits = withCurrent(exitCandidates(model.nodeFacts, anchor.volume), anchor.exitNode);

  return (
    <Stack gap="sm" maw={1100}>
      <IconField icon={anchor.icon} targetFolder="graphics/icons/" onChange={(icon) => edit((draft) => void (draft.icon = icon))} />
      <IdField
        id={anchor.id}
        loadedId={anchor.loadedId}
        noun="anchor"
        renameNote={`Its socketable.${anchor.loadedId ?? anchor.id}.* keys in zh_CN.json keep the old id and must be renamed by hand.`}
        onRename={(newId) => edit((draft) => void (draft.id = newId))}
      />
      <Group grow align="flex-start">
        <Select
          label="Volume"
          description="The tree volume this anchor opens a wormhole to"
          data={volumes}
          value={anchor.volume === '' ? null : anchor.volume}
          onChange={(value) =>
            edit((draft) => {
              draft.volume = value ?? '';
              draft.exitNode = exitCandidates(model.nodeFacts, draft.volume)[0] ?? '';
            })
          }
          allowDeselect={false}
          searchable
        />
        <Select
          label="Exit node"
          description="A wormhole exit node in the volume; the far end the anchor opens onto"
          data={exits}
          value={anchor.exitNode === '' ? null : anchor.exitNode}
          onChange={(value) => edit((draft) => void (draft.exitNode = value ?? ''))}
          allowDeselect={false}
          searchable
        />
      </Group>
      <NameAndRarity
        name={anchor.name}
        rarity={anchor.rarity}
        onName={(name) => edit((draft) => void (draft.name = name))}
        onRarity={(rarity) => edit((draft) => void (draft.rarity = rarity))}
      />
      <Group grow align="flex-start">
        <ColorInput
          label="Faction colour"
          description="#RRGGBB for the cargo icon's frame, lights and wormhole effects; bake the icon glow with tools/WormholeAnchorIcons.java"
          format="hex"
          value={anchor.color}
          onChange={(value) => edit((draft) => void (draft.color = value.toUpperCase()))}
        />
        <TextInput
          label="Crest faction"
          description="Faction id whose crest shows as the icon's badge; blank shows none"
          value={anchor.crestFaction}
          onChange={(event) => {
            const text = event.currentTarget.value;
            edit((draft) => void (draft.crestFaction = text));
          }}
        />
      </Group>
      <Group grow align="flex-start">
        <TextInput
          label="Extra drop factions"
          description="Faction ids separated by ; whose fleets also drop this anchor, on top of the factions mapped to its volume"
          value={anchor.dropFactions}
          onChange={(event) => {
            const text = event.currentTarget.value;
            edit((draft) => void (draft.dropFactions = text));
          }}
        />
        <DecimalInput
          label="Drop chance per 100 DP"
          description="Rolled once per won battle, scaled by the DP of matching ships defeated, capped at one in two"
          value={anchor.dropChance}
          onChange={(text) => edit((draft) => void (draft.dropChance = text))}
        />
      </Group>
      <Checkbox
        label="Always drops"
        description="Every won battle that defeats a matching ship drops this anchor, ignoring the drop chance"
        checked={anchor.guaranteedDrop}
        onChange={(event) => {
          const checked = event.currentTarget.checked;
          edit((draft) => void (draft.guaranteedDrop = checked));
        }}
      />
      <Group grow align="flex-start">
        <DecimalInput
          label="Market chance per restock"
          description="Chance for each open market of a matching faction to stock this anchor when its stock refreshes; blank means never sold"
          value={anchor.marketChance}
          onChange={(text) => edit((draft) => void (draft.marketChance = text))}
        />
        <DecimalInput
          label="Price (credits)"
          description="What markets charge for it and pay for it; blank means 100000"
          value={anchor.price}
          onChange={(text) => edit((draft) => void (draft.price = text))}
        />
      </Group>
      <Select
        label="Unlock condition (the anchor stays inert until it is met)"
        data={unlockOptions(anchor.unlock, vocabulary, true)}
        value={anchor.unlock}
        onChange={(value) => edit((draft) => void (draft.unlock = value ?? ''))}
        allowDeselect={false}
        searchable
      />
      <DescriptionField description={anchor.description} onChange={(description) => edit((draft) => void (draft.description = description))} />
      <Divider />
      <Group>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this anchor
        </Button>
      </Group>
    </Stack>
  );
}
