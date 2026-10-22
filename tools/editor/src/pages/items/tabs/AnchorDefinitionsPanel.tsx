import { Avatar, Box, Button, ColorInput, Divider, Group, NavLink, ScrollArea, Select, Stack, Text, Textarea, TextInput } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useCallback, useEffect, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import {
  anchorVolumes,
  exitCandidates,
  markAnchorsSaved,
  missingVolumes,
  newWormholeAnchor,
  parseWormholeAnchorsCsv,
  serializeWormholeAnchorsCsv,
  treeNodeFacts,
  validateWormholeAnchors,
  WORMHOLE_ANCHOR_FILE_NAME,
  WORMHOLE_ANCHOR_RESOURCE,
  type TreeNodeFacts,
  type WormholeAnchor,
  type WormholeAnchorsModel,
} from '../model/anchors.ts';
import { parseItemDefinitions, parseStarChartDefinitions, UNLOCK_LABELS } from '../model/definitions.ts';
import { DEFINITION_FAMILY_OPTIONS, type DefinitionFamily } from '../model/itemFamily.ts';
import { FamilyOptionSwitch } from './FamilySwitch.tsx';

const LIST_WIDTH = 270;
const OTHER_ITEM_RESOURCES = ['subroutines', 'hull-items', 'star-charts'];

interface AnchorDocumentModel extends WormholeAnchorsModel {
  nodeFacts: Record<string, TreeNodeFacts>;
  otherItemIds: string[];
}

export function AnchorDefinitionsPanel({ vocabulary, onFamily }: { vocabulary: Vocabulary; onFamily: (family: DefinitionFamily) => void }) {
  const document = useDataDocument<AnchorDocumentModel>({
    key: 'items.anchors',
    noun: 'wormhole anchor',
    fileNames: [WORMHOLE_ANCHOR_RESOURCE, 'tree', 'types', ...OTHER_ITEM_RESOURCES],
    parse: (files) => ({
      ...parseWormholeAnchorsCsv(files[WORMHOLE_ANCHOR_RESOURCE].content),
      nodeFacts: treeNodeFacts(files.tree.content, files.types.content, vocabulary),
      otherItemIds: [
        ...parseItemDefinitions(files.subroutines.content, files['hull-items'].content).definitions.map((definition) => definition.id),
        ...parseStarChartDefinitions(files['star-charts'].content).definitions.map((definition) => definition.id),
      ],
    }),
    serialize: (model) => ({ [WORMHOLE_ANCHOR_RESOURCE]: serializeWormholeAnchorsCsv(model) }),
    validate: (model) => validateWormholeAnchors(model.anchors, vocabulary, model.nodeFacts, model.otherItemIds),
    warnings: (model) => missingVolumes(model.anchors, vocabulary).map((volume) => `${volume} has no anchor, so nothing can open its wormhole`),
    savedMessage: () => `Saved ${WORMHOLE_ANCHOR_FILE_NAME}`,
    afterSave: (draft, saved) => markAnchorsSaved(draft, saved),
  });
  const [selectedIndex, setSelectedIndex] = useState(0);
  const anchorCount = document.state.status === 'ready' ? document.state.model.anchors.length : 0;

  const addAnchor = () => {
    document.update((draft) => {
      draft.anchors.push(newWormholeAnchor(draft.anchors, vocabulary, draft.nodeFacts, draft.otherItemIds));
    });
    setSelectedIndex(anchorCount);
  };

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <FamilyOptionSwitch<AnchorDocumentModel, DefinitionFamily>
            document={document}
            family="anchor"
            onFamily={onFamily}
            noun="wormhole anchor"
            options={DEFINITION_FAMILY_OPTIONS}
          />
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addAnchor} disabled={document.state.status !== 'ready'}>
            New anchor
          </Button>
        </DataToolbar>
        <Text size="xs" c="dimmed" mt={4}>
          Edits data/config/exiledSector/wormhole_anchors.csv. Each tree volume has exactly one anchor, and its exit node must be a wormhole exit node in
          that volume. Anchors roll nothing. They drop from fleets of the factions mapped to their volume in npc_faction_volumes.csv and of any extra drop
          factions listed here. Names and descriptions are English; add zh text under socketable.&lt;id&gt;.name / .description in zh_CN.json.
        </Text>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
            <Stack w={LIST_WIDTH} gap="xs" p="xs" style={{ flexShrink: 0, minHeight: 0 }}>
              <ScrollArea style={{ flex: 1 }} type="auto">
                {model.anchors.length === 0 ? (
                  <Text size="sm" c="dimmed">
                    No anchors yet.
                  </Text>
                ) : (
                  model.anchors.map((anchor, index) => (
                    <NavLink
                      key={index}
                      active={index === selectedIndex}
                      onClick={() => setSelectedIndex(index)}
                      label={anchor.name || anchor.id}
                      description={`${anchor.volume || 'no volume'} - ${anchor.exitNode || 'no exit'}`}
                      leftSection={<Avatar src={fileUrl(anchor.icon)} radius="sm" size={32} />}
                    />
                  ))
                )}
              </ScrollArea>
            </Stack>
            <Divider orientation="vertical" />
            <ScrollArea style={{ flex: 1 }} type="auto">
              <Box p="md">
                {model.anchors[selectedIndex] ? (
                  <AnchorForm
                    key={selectedIndex}
                    document={document}
                    anchor={model.anchors[selectedIndex]}
                    index={selectedIndex}
                    vocabulary={vocabulary}
                    nodeFacts={model.nodeFacts}
                    onSelect={setSelectedIndex}
                  />
                ) : (
                  <Text c="dimmed">Select an anchor, or add a new one.</Text>
                )}
              </Box>
            </ScrollArea>
          </Group>
        )}
      </DocumentState>
    </Stack>
  );
}

interface AnchorFormProps {
  document: DataDocument<AnchorDocumentModel>;
  anchor: WormholeAnchor;
  index: number;
  vocabulary: Vocabulary;
  nodeFacts: Record<string, TreeNodeFacts>;
  onSelect: (index: number) => void;
}

function withCurrent(options: string[], current: string): string[] {
  return current === '' || options.includes(current) ? options : [current, ...options];
}

function AnchorForm({ document, anchor, index, vocabulary, nodeFacts, onSelect }: AnchorFormProps) {
  const [idText, setIdText] = useState(anchor.id);
  useEffect(() => setIdText(anchor.id), [anchor.id]);
  const updateDocument = document.update;
  const edit = useCallback(
    (recipe: (draft: WormholeAnchor) => void) =>
      updateDocument((draft) => {
        recipe(draft.anchors[index]);
      }),
    [updateDocument, index],
  );

  const commitId = async () => {
    const newId = idText.trim();
    if (newId === anchor.id) {
      return;
    }
    if (anchor.loadedId && anchor.id === anchor.loadedId) {
      const rename = await confirmAction({
        title: `Rename "${anchor.loadedId}" to "${newId}"?`,
        message:
          'Saved games store this id on every anchor players own. Once it has shipped, add a row to item_id_aliases.csv mapping the old id to the new one, or those anchors turn into unknown items. ' +
          `Its socketable.${anchor.loadedId}.* keys in zh_CN.json keep the old id and must be renamed by hand.`,
        confirmLabel: 'Rename',
        danger: true,
      });
      if (!rename) {
        setIdText(anchor.id);
        return;
      }
    }
    edit((draft) => void (draft.id = newId));
  };

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Delete "${anchor.id}"?`,
      message: 'Anchors players already own will show as unrecognised, and nothing will lead to this volume.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => {
        draft.anchors.splice(index, 1);
      });
      onSelect(Math.max(0, index - 1));
    }
  };

  const volumes = withCurrent(anchorVolumes(vocabulary), anchor.volume);
  const exits = withCurrent(exitCandidates(nodeFacts, anchor.volume), anchor.exitNode);
  const unlockOptions = [{ value: '', label: 'None' }, ...vocabulary.itemUnlocks.map((unlockId) => ({ value: unlockId, label: UNLOCK_LABELS[unlockId] ?? unlockId }))];
  for (let tier = vocabulary.lowestChartTier; tier <= vocabulary.highestChartTier; tier++) {
    unlockOptions.push({ value: `${vocabulary.clearedChartTierPrefix}${tier}`, label: `Cleared a tier ${tier} chart objective` });
  }
  if (!unlockOptions.some((option) => option.value === anchor.unlock)) {
    unlockOptions.push({ value: anchor.unlock, label: `${anchor.unlock} (unknown)` });
  }

  return (
    <Stack gap="sm" maw={1100}>
      <Group align="flex-end" wrap="nowrap">
        <Avatar src={fileUrl(anchor.icon)} radius="sm" size={64} />
        <TextInput style={{ flex: 1 }} label="Icon path" value={anchor.icon} onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.icon = text)); }} />
        <ImagePickButton onPicked={(path) => edit((draft) => void (draft.icon = path))} />
      </Group>
      <TextInput
        label={anchor.loadedId ? 'Id (saved games store it - renaming orphans owned anchors)' : 'Id'}
        value={idText}
        onChange={(event) => setIdText(event.currentTarget.value)}
        onBlur={() => void commitId()}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.currentTarget.blur();
          }
        }}
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
              draft.exitNode = exitCandidates(nodeFacts, draft.volume)[0] ?? '';
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
      <TextInput label="Name" value={anchor.name} onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.name = text)); }} />
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
      <DecimalInput w={220} label="Rarity (loot weight)" value={anchor.rarity} onChange={(text) => edit((draft) => void (draft.rarity = text))} />
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
        data={unlockOptions}
        value={anchor.unlock}
        onChange={(value) => edit((draft) => void (draft.unlock = value ?? ''))}
        allowDeselect={false}
        searchable
      />
      <Textarea
        label="Tooltip description (optional)"
        autosize
        minRows={3}
        value={anchor.description}
        onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.description = text)); }}
      />
      <Divider />
      <Group>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this anchor
        </Button>
      </Group>
    </Stack>
  );
}
