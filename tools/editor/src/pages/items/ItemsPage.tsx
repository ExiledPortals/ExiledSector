import { Alert, Box, Center, Divider, Loader, Stack, Tabs } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import { useVocabulary, type Vocabulary } from '../../api/vocabulary.ts';
import { DataToolbar } from '../../shared/DataToolbar.tsx';
import { DocumentState } from '../../shared/DocumentState.tsx';
import { HullItemTypesPanel } from './hullItems/HullItemTypesPanel.tsx';
import { useItemsDocument } from './itemsDocument.ts';
import { ModifierCataloguePanel } from './modifiers/ModifierCataloguePanel.tsx';
import { DoctrineArchivesPanel } from './socketables/DoctrineArchivesPanel.tsx';
import { SubroutinesPanel } from './socketables/SubroutinesPanel.tsx';
import { WormholeAnchorsPanel } from './socketables/WormholeAnchorsPanel.tsx';

const FILL = { flex: 1, minHeight: 0 };
const TAB_COLUMN = { flexDirection: 'column' } as const;

export function ItemsPage() {
  const vocabulary = useVocabulary();
  if (vocabulary.isError) {
    return (
      <Alert m="md" color="red" icon={<IconAlertTriangle />} title="Could not load editor_vocabulary.json">
        {vocabulary.error.message}
      </Alert>
    );
  }
  if (!vocabulary.data) {
    return (
      <Center h="100%">
        <Loader />
      </Center>
    );
  }
  return <ItemsWorkspace vocabulary={vocabulary.data} />;
}

function ItemsWorkspace({ vocabulary }: { vocabulary: Vocabulary }) {
  const document = useItemsDocument(vocabulary);
  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document} />
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <Tabs defaultValue="hull-items" keepMounted display="flex" style={{ ...FILL, ...TAB_COLUMN }}>
            <Tabs.List px="md" pt="xs">
              <Tabs.Tab value="hull-items">Hull Items</Tabs.Tab>
              <Tabs.Tab value="star-charts">Star Charts</Tabs.Tab>
              <Tabs.Tab value="socketables">Skill Tree Socketables</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="hull-items" style={FILL}>
              <Tabs defaultValue="item-types" keepMounted variant="pills" h="100%" display="flex" style={TAB_COLUMN}>
                <Tabs.List px="md" py="xs">
                  <Tabs.Tab value="item-types">Item types</Tabs.Tab>
                  <Tabs.Tab value="modifiers">Modifiers</Tabs.Tab>
                </Tabs.List>
                <Tabs.Panel value="item-types" style={FILL}>
                  <HullItemTypesPanel document={document} model={model} vocabulary={vocabulary} />
                </Tabs.Panel>
                <Tabs.Panel value="modifiers" style={FILL}>
                  <ModifierCataloguePanel document={document} model={model} vocabulary={vocabulary} family="ship" />
                </Tabs.Panel>
              </Tabs>
            </Tabs.Panel>
            <Tabs.Panel value="star-charts" style={FILL}>
              <ModifierCataloguePanel document={document} model={model} vocabulary={vocabulary} family="chart" />
            </Tabs.Panel>
            <Tabs.Panel value="socketables" style={FILL}>
              <Tabs defaultValue="subroutines" keepMounted variant="pills" h="100%" display="flex" style={TAB_COLUMN}>
                <Tabs.List px="md" py="xs">
                  <Tabs.Tab value="subroutines">Subroutines</Tabs.Tab>
                  <Tabs.Tab value="doctrines">Doctrine Archives</Tabs.Tab>
                  <Tabs.Tab value="anchors">Wormhole Anchors</Tabs.Tab>
                </Tabs.List>
                <Tabs.Panel value="subroutines" style={FILL}>
                  <SubroutinesPanel document={document} model={model} vocabulary={vocabulary} />
                </Tabs.Panel>
                <Tabs.Panel value="doctrines" style={FILL}>
                  <DoctrineArchivesPanel document={document} model={model} vocabulary={vocabulary} />
                </Tabs.Panel>
                <Tabs.Panel value="anchors" style={FILL}>
                  <WormholeAnchorsPanel document={document} model={model} vocabulary={vocabulary} />
                </Tabs.Panel>
              </Tabs>
            </Tabs.Panel>
          </Tabs>
        )}
      </DocumentState>
    </Stack>
  );
}
