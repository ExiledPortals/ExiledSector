import { Alert, Center, Loader, Tabs } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import { useVocabulary } from '../../api/vocabulary.ts';
import { CHART_BLOCKED_ABILITY_SPEC, CHART_DROP_SPEC, CHART_FACTION_SPEC, CHART_POI_SPEC, CHART_TIER_SPEC } from './model/chartTables.ts';
import { ChartTableTab } from './tabs/ChartTableTab.tsx';
import { CraftingTab } from './tabs/CraftingTab.tsx';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';
import { DropsTab } from './tabs/DropsTab.tsx';
import { ImplicitsTab } from './tabs/ImplicitsTab.tsx';
import { ModifiersTab } from './tabs/ModifiersTab.tsx';
import { NamesTab } from './tabs/NamesTab.tsx';

const PANEL_STYLE = { flex: 1, minHeight: 0 };

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
  return (
    <Tabs defaultValue="definitions" keepMounted h="100%" display="flex" style={{ flexDirection: 'column' }}>
      <Tabs.List px="md" pt="xs">
        <Tabs.Tab value="definitions">Definitions</Tabs.Tab>
        <Tabs.Tab value="modifiers">Modifiers</Tabs.Tab>
        <Tabs.Tab value="implicits">Implicits</Tabs.Tab>
        <Tabs.Tab value="names">Names</Tabs.Tab>
        <Tabs.Tab value="drops">Drops</Tabs.Tab>
        <Tabs.Tab value="crafting">Crafting</Tabs.Tab>
        <Tabs.Tab value="chart-tiers">Chart tiers</Tabs.Tab>
        <Tabs.Tab value="chart-pois">Chart POIs</Tabs.Tab>
        <Tabs.Tab value="chart-factions">Chart factions</Tabs.Tab>
        <Tabs.Tab value="chart-drops">Chart drops</Tabs.Tab>
        <Tabs.Tab value="chart-blocked-abilities">Chart blocked abilities</Tabs.Tab>
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
      <Tabs.Panel value="chart-tiers" style={PANEL_STYLE}>
        <ChartTableTab spec={CHART_TIER_SPEC} vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="chart-pois" style={PANEL_STYLE}>
        <ChartTableTab spec={CHART_POI_SPEC} vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="chart-factions" style={PANEL_STYLE}>
        <ChartTableTab spec={CHART_FACTION_SPEC} vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="chart-drops" style={PANEL_STYLE}>
        <ChartTableTab spec={CHART_DROP_SPEC} vocabulary={vocabulary.data} />
      </Tabs.Panel>
      <Tabs.Panel value="chart-blocked-abilities" style={PANEL_STYLE}>
        <ChartTableTab spec={CHART_BLOCKED_ABILITY_SPEC} vocabulary={vocabulary.data} />
      </Tabs.Panel>
    </Tabs>
  );
}
