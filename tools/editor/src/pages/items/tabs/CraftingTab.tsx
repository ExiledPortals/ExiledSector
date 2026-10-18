import { Avatar, Box, Divider, Group, ScrollArea, Stack, Text, TextInput } from '@mantine/core';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import {
  commodityIcon,
  CRAFTING_ITEMS,
  costRow,
  parseCraftingCsv,
  serializeCraftingCsv,
  setCommodityIcon,
  validateCrafting,
  type CraftingModel,
} from '../model/crafting.ts';

export function CraftingTab() {
  const document = useDataDocument<CraftingModel>({
    key: 'items.crafting',
    noun: 'crafting',
    fileNames: ['item-crafting', 'commodities'],
    parse: (files) => {
      const crafting = parseCraftingCsv(files['item-crafting'].content);
      return { costTable: crafting.table, costs: crafting.rows, commodities: parseCsvTable(files.commodities.content) };
    },
    serialize: (model) => ({
      'item-crafting': serializeCraftingCsv(model.costTable, model.costs),
      commodities: serializeCsvTable(model.commodities),
    }),
    validate: validateCrafting,
    savedMessage: () => 'Saved item_crafting.csv and commodities.csv',
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
                Edits data/config/exiledSector/hullItem_crafting.csv (costs) and the icon column of data/campaign/commodities.csv (images). Images picked from
                graphics/unused/ move to graphics/icons/cargo/.
              </Text>
              {CRAFTING_ITEMS.map((item) => {
                const icon = commodityIcon(model.commodities, item.id);
                const setIcon = (path: string) => document.update((draft) => setCommodityIcon(draft.commodities, item.id, path));
                const cost = costRow(model, item.id);
                return (
                  <Group key={item.id} align="flex-end" wrap="nowrap">
                    {item.commodity ? (
                      <>
                        <Avatar src={fileUrl(icon)} radius="sm" size={56} />
                        <TextInput style={{ flex: 2 }} label={`${item.label} - image`} value={icon} onChange={(event) => setIcon(event.currentTarget.value.trim())} />
                        <ImagePickButton targetFolder="graphics/icons/cargo/" onPicked={setIcon} />
                      </>
                    ) : (
                      <Stack gap={2} style={{ flex: 2 }}>
                        <Text size="sm" fw={500}>
                          {item.label}
                        </Text>
                        <Text size="xs" c="dimmed">
                          Cycles through the icons of every non-unique subroutine in hullItems.csv.
                        </Text>
                      </Stack>
                    )}
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
                          Not synthesised - comes from disassembly and loot.
                        </Text>
                      </Stack>
                    )}
                  </Group>
                );
              })}
            </Stack>
          </ScrollArea>
        )}
      </DocumentState>
    </Stack>
  );
}
