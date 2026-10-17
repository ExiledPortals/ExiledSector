import { Alert, Center, Loader, Tabs } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import { useVocabulary } from '../../api/vocabulary.ts';
import { CraftingTab } from './tabs/CraftingTab.tsx';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';
import { DropsTab } from './tabs/DropsTab.tsx';
import { ImplicitsTab } from './tabs/ImplicitsTab.tsx';
import { ModifiersTab } from './tabs/ModifiersTab.tsx';
import { NamesTab } from './tabs/NamesTab.tsx';

const PANEL_STYLE = { flex: 1, minHeight: 0 };

export function HullItemsPage() {
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
  return (
    <Tabs defaultValue="definitions" keepMounted h="100%" display="flex" style={{ flexDirection: 'column' }}>
      <Tabs.List px="md" pt="xs">
        <Tabs.Tab value="definitions">Definitions</Tabs.Tab>
        <Tabs.Tab value="modifiers">Modifiers</Tabs.Tab>
        <Tabs.Tab value="implicits">Implicits</Tabs.Tab>
        <Tabs.Tab value="names">Names</Tabs.Tab>
        <Tabs.Tab value="drops">Drops</Tabs.Tab>
        <Tabs.Tab value="crafting">Crafting</Tabs.Tab>
      </Tabs.List>
      <Tabs.Panel value="definitions" style={PANEL_STYLE}>
        <DefinitionsTab vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="modifiers" style={PANEL_STYLE}>
        <ModifiersTab vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="implicits" style={PANEL_STYLE}>
        <ImplicitsTab vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="names" style={PANEL_STYLE}>
        <NamesTab />
      </Tabs.Panel>
      <Tabs.Panel value="drops" style={PANEL_STYLE}>
        <DropsTab />
      </Tabs.Panel>
      <Tabs.Panel value="crafting" style={PANEL_STYLE}>
        <CraftingTab />
      </Tabs.Panel>
    </Tabs>
  );
}
