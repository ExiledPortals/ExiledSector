import { Avatar, Box, Divider, Group, ScrollArea, Stack, Text, TextInput, Title } from '@mantine/core';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import {
  materialIcon,
  CRAFTING_ITEMS,
  costRow,
  parseCraftingCsv,
  serializeCraftingCsv,
  setMaterialIcon,
  validateCrafting,
  type CraftingModel,
} from '../model/crafting.ts';

export function CraftingTab() {
  const document = useDataDocument<CraftingModel>({
    key: 'items.crafting',
    noun: 'crafting',
    fileNames: ['item-crafting', 'special-items'],
    parse: (files) => {
      const crafting = parseCraftingCsv(files['item-crafting'].content);
      return { costTable: crafting.table, costs: crafting.rows, specialItems: parseCsvTable(files['special-items'].content) };
    },
    serialize: (model) => ({
      'item-crafting': serializeCraftingCsv(model.costTable, model.costs),
      'special-items': serializeCsvTable(model.specialItems),
    }),
    validate: validateCrafting,
    savedMessage: () => 'Saved item_crafting.csv and special_items.csv',
  });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document} />
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <ScrollArea style={{ flex: 1 }} type="auto">
            <Stack p="md" gap="md" maw={1000}>
              <Text size="xs" c="dimmed">
                Edits data/config/exiledSector/item_crafting.csv (costs) and the plugin params column of data/campaign/special_items.csv (images). Images picked from
                graphics/unused/ move to graphics/icons/cargo/. The three ciphers only work on star charts and only drop inside chart pockets.
              </Text>
              {CRAFTING_ITEMS.map((item, itemIndex) => {
                const icon = materialIcon(model.specialItems, item.id);
                const setIcon = (path: string) => document.update((draft) => setMaterialIcon(draft.specialItems, item.id, path));
                const cost = costRow(model, item.id);
                const startsCipherGroup = item.starChart && !CRAFTING_ITEMS[itemIndex - 1]?.starChart;
                return (
                  <Stack key={item.id} gap="xs">
                    {startsCipherGroup && (
                      <Title order={5} mt="sm">
                        Star chart ciphers
                      </Title>
                    )}
                    <Group align="flex-end" wrap="nowrap">
                      <Avatar src={fileUrl(icon)} radius="sm" size={56} />
                      <TextInput style={{ flex: 2 }} label={`${item.label} - image`} value={icon} onChange={(event) => setIcon(event.currentTarget.value.trim())} />
                      <ImagePickButton targetFolder="graphics/icons/cargo/" onPicked={setIcon} />
                      {item.cost && cost ? (
                        <DecimalInput
                          w={180}
                          label="Parts to synthesise"
                          value={cost.partsCost}
                          onChange={(text) =>
                            document.update((draft) => {
                              const draftCost = costRow(draft, item.id);
                              if (draftCost) {
                                draftCost.partsCost = text;
                              }
                            })
                          }
                        />
                      ) : (
                        <Stack gap={2} w={180}>
                          <Text size="sm" fw={500}>
                            Parts to synthesise
                          </Text>
                          <Text size="xs" c="dimmed">
                            Not synthesised - comes from disassembly or loot.
                          </Text>
                        </Stack>
                      )}
                    </Group>
                  </Stack>
                );
              })}
            </Stack>
          </ScrollArea>
        )}
      </DocumentState>
    </Stack>
  );
}
