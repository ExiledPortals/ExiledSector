package exiledsector.items;

import exiledsector.charts.ChartDrops;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.charts.ChartEffect;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.charts.ChartGenContext;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import exiledsector.modifiers.TierRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartTest {

    private static final String SINGLE = "star_chart_single";
    private static final String BINARY = "star_chart_binary_far";
    private static final String UNGATED_UNIQUE = StarChartFixtures.TEST_UNIQUE;
    private static final String GATED_UNIQUE = StarChartFixtures.TEST_GATED_UNIQUE;

    @BeforeEach
    void setUp() throws Exception {
        ItemFixtures.registerShippedItems();
        ItemImplicits.register(HullItemRows.shippedRows(ItemImplicits.dataPath()));
        StarChartFixtures.registerShipped();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        ItemImplicits.clear();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    private static Set<String> modifierIds(Item item) {
        Set<String> ids = new HashSet<>();
        item.explicits().forEach(modifier -> ids.add(modifier.modifierId()));
        return ids;
    }

    @Test
    void chartDefinitionsLoadAsTheirOwnTypeAndStayOutOfTheShipItemList() {
        assertEquals(8, ItemDefinitions.ofType(ItemType.STAR_CHART).size());
        assertTrue(ItemDefinitions.all().stream().noneMatch(definition -> definition.type() == ItemType.STAR_CHART));
        assertTrue(ItemDefinitions.ofType(ItemType.HULL_ITEM).stream().allMatch(definition -> definition.type() == ItemType.HULL_ITEM));
        ItemDefinition single = ItemDefinitions.get(SINGLE);
        assertEquals(ItemType.STAR_CHART, single.type());
        assertNull(single.kind());
        assertEquals("SINGLE", StarChartDefinitions.systemType(SINGLE));
        assertEquals(StarChartDefinitions.ANY_SYSTEM_TYPE, StarChartDefinitions.systemType("not_a_chart"));
        assertTrue(ItemDefinitions.get(UNGATED_UNIQUE).unique());
    }

    @Test
    void theChartTierMatchesTheRunTiers() {
        assertEquals(exiledsector.charts.ChartTiers.LOWEST_TIER, StarChart.LOWEST_TIER);
        assertEquals(exiledsector.charts.ChartTiers.HIGHEST_TIER, StarChart.HIGHEST_TIER);
    }

    @Test
    void aChartRolledAtATierKeepsItThroughTheStoreAndClampsOutOfRangeTiers() {
        StarChart chart = StarChartFixtures.chart(SINGLE, 5L, 7);
        assertEquals(7, chart.tier());
        assertEquals(ItemType.STAR_CHART, chart.type());

        ItemStore store = ItemStore.forStarCharts();
        Item stored = store.add(ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 5L, 7));
        assertInstanceOf(StarChart.class, stored);
        assertEquals(7, stored.tier());
        assertTrue(stored.id().startsWith("star_chart_"));
        assertEquals(chart.explicits(), stored.explicits());
        assertEquals(StarChart.HIGHEST_TIER, StarChartFixtures.chart(SINGLE, 5L, 99).tier());
        assertEquals(StarChart.LOWEST_TIER, StarChartFixtures.chart(SINGLE, 5L, -3).tier());
        assertEquals(StarChart.LOWEST_TIER, ItemData.rolled(ItemDefinitions.get(SINGLE), 5L).preview().tier());
    }

    @Test
    void theTierSurvivesMirroringTransposingCorruptionAndRerolls() {
        ItemStore store = ItemStore.forStarCharts();
        Item chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 11L, 9));

        Item mirror = store.addMirrorOf(chart);
        assertEquals(9, mirror.tier());
        assertTrue(mirror.mirrored());
        assertEquals(chart.implicits(), mirror.implicits());

        assertSame(chart, ItemCrafting.recalibrate(chart, new Random(3)));
        assertEquals(9, chart.tier());
        assertSame(chart, ItemCrafting.randomiseModifiers(store, chart, new Random(4), definition -> true));
        assertEquals(9, chart.tier());

        Item unique = store.add(ItemData.rolledAtTier(ItemDefinitions.get(UNGATED_UNIQUE), 12L, 13));
        Item transposed = ItemCrafting.transpose(store, unique, new Random(5), definition -> true);
        assertNotNull(transposed);
        assertEquals(GATED_UNIQUE, transposed.definitionId());
        assertEquals(13, transposed.tier());
        assertEquals(ItemType.STAR_CHART, transposed.type());
    }

    @Test
    void transposingAShipItemNeverPicksAChart() {
        ItemStore store = new ItemStore();
        ItemDefinition shipUnique = ItemDefinitions.all().stream()
                .filter(definition -> definition.unique() && definition.type() == ItemType.HULL_ITEM).findFirst().orElseThrow();
        Item item = store.add(shipUnique, 1L);
        for (int attempt = 0; attempt < 40; attempt++) {
            Item transposed = ItemCrafting.transpose(store, item, new Random(attempt), definition -> true);
            assertNotNull(transposed);
            assertTrue(transposed.type() != ItemType.STAR_CHART, transposed.definitionId());
            item = transposed;
        }
    }

    @Test
    void chartRollsRespectMinimumTierGates() {
        ItemDefinition single = ItemDefinitions.get(SINGLE);
        boolean extraOfficersAtHighTier = false;
        boolean bossSizeAtTierOne = false;
        for (long seed = 0; seed < 400; seed++) {
            assertFalse(modifierIds(StarChartFixtures.chart(SINGLE, seed, 2)).contains("HYBRID_CHART_EXTRA_OFFICERS"), "seed " + seed);
            extraOfficersAtHighTier |= modifierIds(StarChartFixtures.chart(SINGLE, seed, 10)).contains("HYBRID_CHART_EXTRA_OFFICERS");
            bossSizeAtTierOne |= modifierIds(StarChartFixtures.chart(SINGLE, seed, 1)).contains("HYBRID_CHART_BOSS_SIZE");
        }
        assertTrue(extraOfficersAtHighTier);
        assertTrue(bossSizeAtTierOne, "every tier has a boss, so its size prefix rolls from tier 1");
        assertEquals(TierRange.atLeast(3), single.poolEntryFor("HYBRID_CHART_EXTRA_OFFICERS").tiers());
        assertEquals(TierRange.ANY, single.poolEntryFor("HYBRID_CHART_BOSS_SIZE").tiers());
        assertEquals(TierRange.ANY, single.poolEntryFor("CHART_BOSS_SPOILS_PERCENT").tiers());
        assertEquals(TierRange.ANY, single.poolEntryFor("HYBRID_CHART_FLEET_SIZE").tiers());
    }

    @Test
    void chartsCarryUpToSixModifiersWithThreeOnEachSide() {
        Set<Integer> counts = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            StarChart chart = StarChartFixtures.chart(SINGLE, seed, 12);
            counts.add(chart.explicits().size());
            assertTrue(chart.explicits().stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count() <= 3);
            assertTrue(chart.explicits().stream().filter(modifier -> modifier.slot() == ModifierSlot.SUFFIX).count() <= 3);
            assertEquals(chart.explicits().size() <= 2 ? ItemRarity.COMMON : ItemRarity.RARE, chart.rarity());
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), counts);
        assertEquals(ItemRarity.UNIQUE, StarChartFixtures.chart(UNGATED_UNIQUE, 1L, 3).rarity());
    }

    @Test
    void tierBandsScaleTheShownValuesWithTheChartsTier() {
        StarChart lowTier = StarChartFixtures.chart(GATED_UNIQUE, 3L, 1);
        StarChart highTier = StarChartFixtures.chart(GATED_UNIQUE, 3L, 12);
        String lowText = lowTier.effectLines().get(0).plain();
        String highText = highTier.effectLines().get(0).plain();

        assertTrue(lowText.contains("200% increased"), lowText);
        assertTrue(highText.contains("700% increased"), highText);
        assertTrue(lowTier.effectLines(true).stream().anyMatch(line -> line.plain().contains("(")),
                "ranged tier bands show the band's range: " + lowTier.effectLines(true));
    }

    @Test
    void everyChartRollsOneInhabitingFactionAsItsOnlyImplicit() {
        Set<String> factionImplicits = new HashSet<>();
        for (String definitionId : List.of(SINGLE, BINARY, GATED_UNIQUE)) {
            for (long seed = 0; seed < 40; seed++) {
                List<RolledModifier> implicits = StarChartFixtures.chart(definitionId, seed, 5).implicits();
                assertEquals(1, implicits.size(), definitionId + " " + implicits);
                assertTrue(implicits.get(0).modifierId().startsWith("CHART_FACTION_"), implicits.get(0).modifierId());
                factionImplicits.add(implicits.get(0).modifierId());
            }
        }
        StarChart single = StarChartFixtures.chart(SINGLE, 8L, 3);

        assertTrue(factionImplicits.size() > 5, "the faction varies by seed: " + factionImplicits);
        assertEquals(single.implicits(), StarChartFixtures.chart(SINGLE, 8L, 3).implicits());
        assertEquals(1, single.implicitLines(false).size());
        assertTrue(single.implicitLines(false).get(0).plain().startsWith("The system is inhabited by "), single.implicitLines(false).get(0).plain());
        assertEquals(StarChartImplicits.sharedChoices(), StarChartImplicits.corruptionPool());
        assertEquals(single.implicits(), single.traits().implicits(), "the rolled faction is stored so it never changes with the faction list");
    }

    @Test
    void aChartKeepsReadingItsFactionAfterTheFactionLeavesTheList() throws Exception {
        StarChart chart = StarChartFixtures.chart(SINGLE, 8L, 3);
        String factionImplicit = chart.implicits().get(0).modifierId();
        String keptFaction = factionImplicit.equals("CHART_FACTION_PIRATES") ? "hegemony" : "pirates";
        exiledsector.charts.ChartFactions.register(new org.json.JSONArray("[{\"faction\": \"" + keptFaction + "\", \"weight\": \"1\"}]"));

        assertEquals(factionImplicit, chart.implicits().get(0).modifierId());
        assertTrue(chart.implicitLines(false).get(0).plain().startsWith("The system is inhabited by "), chart.implicitLines(false).get(0).plain());
    }

    @Test
    void storedImplicitsThatNoLongerExistAreReplacedByTheSeededFaction() {
        StarChart chart = StarChartFixtures.chart(SINGLE, 8L, 3);
        List<RolledModifier> seeded = List.copyOf(chart.implicits());
        chart.replaceImplicits(List.of(new RolledModifier("CHART_BASE_FLEETS_BINARY", ModifierSlot.IMPLICIT,
                List.of(new RolledEffect("CHART_BASE_FLEETS", 1f)))));

        assertEquals(seeded, chart.implicits());
    }

    @Test
    void aDroppedChartStoresItsFactionFromTheStart() throws Exception {
        ItemData dropped = ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 8L, 3);

        assertEquals(StarChartFixtures.chart(SINGLE, 8L, 3).implicits(), dropped.traits().implicits());
        String droppedFaction = dropped.traits().implicits().get(0).modifierId();
        String keptFaction = droppedFaction.equals("CHART_FACTION_PIRATES") ? "hegemony" : "pirates";
        exiledsector.charts.ChartFactions.register(new org.json.JSONArray("[{\"faction\": \"" + keptFaction + "\", \"weight\": \"1\"}]"));
        assertEquals(dropped.traits().implicits(), dropped.preview().implicits(), "a later faction list change leaves the chart's faction alone");
    }

    @Test
    void theCodecRoundTripsTierQualityCorruptionAndChangedImplicits() {
        StarChart chart = StarChartFixtures.chart(SINGLE, 21L, 6);
        chart.applyTraits(new ItemTraits(6, 12, CorruptionOutcome.IMPLICIT_REPLACED, null));
        Modifier replacement = StarChartImplicits.sharedChoices().stream().filter(implicit -> !implicit.id().equals(chart.implicits().get(0).modifierId()))
                .findFirst().orElseThrow();
        chart.replaceImplicits(List.of(RolledModifier.roll(replacement, new Random(1), false)));

        String encoded = ItemData.of(chart).encode();
        ItemData decoded = ItemData.parse(encoded);
        StarChart restored = (StarChart) decoded.preview();

        assertTrue(encoded.contains("\"t\":6") && encoded.contains("\"q\":12") && encoded.contains("\"c\":\"IMPLICIT_REPLACED\""), encoded);
        assertEquals(6, restored.tier());
        assertEquals(12, restored.quality());
        assertEquals(CorruptionOutcome.IMPLICIT_REPLACED, restored.corruption());
        assertEquals(chart.explicits(), restored.explicits());
        assertEquals(chart.implicits(), restored.implicits());
        assertEquals(chart.name(), restored.name());
    }

    @Test
    void chartsTravelAsTheirOwnSpecialItemThatTheHullItemDecoderIgnores() {
        ItemData chart = ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 4L, 2);
        SpecialItemData special = chart.toSpecialItem();

        assertEquals(ItemData.STAR_CHART_ITEM_ID, special.getId());
        assertNull(ItemData.of(special));
        assertEquals(chart.traits(), ItemData.ofStarChart(special).traits());
        assertEquals(2, ItemData.ofStarChart(special).preview().tier());
        assertEquals(ItemData.ITEM_ID, ItemData.rolled(ItemDefinitions.all().iterator().next(), 1L).toSpecialItem().getId());
        assertNull(ItemData.rolled(ItemDefinitions.all().iterator().next(), 1L).traits());
        assertEquals(StarChart.LOWEST_TIER, ItemData.ofStarChart(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, SINGLE + "|5")).preview().tier());
    }

    @Test
    void theStoresNeverTakeEachOthersItems() {
        ItemStore hullStore = new ItemStore();
        ItemStore chartStore = ItemStore.forStarCharts();
        ItemData chart = ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 4L, 2);
        ItemData hullItem = ItemData.rolled(ItemDefinitions.all().iterator().next(), 4L);

        assertNull(hullStore.add(chart));
        assertNull(chartStore.add(hullItem));
        assertNotNull(hullStore.add(hullItem));
        assertNotNull(chartStore.add(chart));
        assertTrue(hullStore.owned().stream().noneMatch(item -> item.type() == ItemType.STAR_CHART));
        assertTrue(chartStore.owned().stream().allMatch(item -> item.type() == ItemType.STAR_CHART));
        assertFalse(hullStore.holdsStarCharts());
        assertTrue(chartStore.holdsStarCharts());
        assertTrue(ItemStore.starChartDataKey() != null && !ItemStore.starChartDataKey().equals(ItemStore.dataKey()));
    }

    @Test
    void absorbingDispatchesOnTheSpecialItemId() {
        ItemData chart = ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 4L, 5);
        ItemData hullItem = ItemData.rolled(ItemDefinitions.all().iterator().next(), 4L);
        CargoStackAPI chartStack = stack(chart.toSpecialItem(), 2f);
        CargoStackAPI hullStack = stack(hullItem.toSpecialItem(), 1f);
        CargoStackAPI upgradeStack = stack(RefitSchematic.MILITARY.toSpecialItem(), 1f);
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(chartStack, hullStack, upgradeStack)));

        ItemStore hullStore = new ItemStore();
        assertEquals(2, hullStore.absorbFrom(cargo));
        verify(cargo, never()).removeStack(chartStack);
        ItemStore chartStore = ItemStore.forStarCharts();
        assertEquals(2, chartStore.absorbFrom(cargo));
        verify(cargo).removeStack(chartStack);
        assertEquals(2, chartStore.owned().size());
        assertTrue(chartStore.owned().stream().allMatch(item -> item.tier() == 5));
        assertTrue(ItemStore.carriesStarCharts(cargo));
        assertEquals(1, hullStore.owned().size());
        assertEquals(1, hullStore.schematicCount(RefitSchematic.MILITARY));
    }

    @Test
    void aChartSpecialHoldingAShipItemStaysInCargo() {
        ItemData hullItem = ItemData.rolled(ItemDefinitions.all().iterator().next(), 4L);
        CargoStackAPI disguised = stack(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, hullItem.encode()), 1f);
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(disguised)));

        assertEquals(0, ItemStore.forStarCharts().absorbFrom(cargo));
        verify(cargo, never()).removeStack(disguised);
    }

    private static CargoStackAPI stack(SpecialItemData special, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void aSavedChartStoreLoadsBackUnderStableAliases() {
        ItemStore store = ItemStore.forStarCharts();
        Item chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get(BINARY), 77L, 8));
        chart.corrupt(CorruptionOutcome.NOTHING);

        String xml = xstream().toXML(store);
        ItemStore loaded = (ItemStore) xstream().fromXML(xml);
        Item restored = loaded.owned().get(0);

        assertTrue(xml.contains("<exiledSector.StarChart>") && xml.contains("<tier>8</tier>"), xml);
        assertFalse(xml.contains("exiledsector.items"), xml);
        assertInstanceOf(StarChart.class, restored);
        assertEquals(8, restored.tier());
        assertTrue(restored.corrupted());
        assertTrue(loaded.holdsStarCharts());
        assertEquals(chart.explicits(), restored.explicits());
        assertEquals(chart.implicits(), restored.implicits());
    }

    @Test
    void aChartWhoseDefinitionGoesMissingKeepsItsTierUntilItReturns() {
        ItemStore store = ItemStore.forStarCharts();
        store.add(ItemData.rolledAtTier(ItemDefinitions.get(BINARY), 77L, 8));
        String xml = xstream().toXML(store);

        StarChartItems.clear();
        Item unresolved = ((ItemStore) xstream().fromXML(xml)).owned().get(0);
        assertInstanceOf(UnresolvedItem.class, unresolved);
        String resaved = xstream().toXML(unresolved);
        assertTrue(resaved.contains("<chartTier>8</chartTier>"), resaved);

        try {
            StarChartFixtures.registerShipped();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Item restored = (Item) xstream().fromXML(resaved);
        assertInstanceOf(StarChart.class, restored);
        assertEquals(8, restored.tier());
    }

    @Test
    void chartEffectNamesNeverOverlapSkillEffectNames() {
        for (ChartEffect effect : ChartEffect.values()) {
            assertTrue(effect.name().startsWith("CHART_"), effect.name());
            assertThrows(IllegalArgumentException.class, () -> SkillEffect.byName(effect.name()), effect.name());
            assertSame(effect, ItemEffects.forType(ItemType.STAR_CHART).byName(effect.name()));
            assertNull(ItemEffects.SKILL_EFFECTS.byName(effect.name()));
        }
        assertEquals(ItemEffects.SKILL_EFFECTS, ItemEffects.forType(ItemType.HULL_ITEM));
    }

    @Test
    void chartsNameThemselvesWithTheStarChartNoun() {
        boolean sawCommonName = false;
        for (long seed = 0; seed < 50 && !sawCommonName; seed++) {
            StarChart chart = StarChartFixtures.chart(SINGLE, seed, 3);
            if (chart.rarity() == ItemRarity.COMMON) {
                sawCommonName = true;
                assertTrue(chart.displayName().title().toLowerCase(java.util.Locale.ROOT).contains("star chart"), chart.displayName().title());
                assertEquals("Single Star Chart", chart.displayName().baseName());
            }
        }
        assertTrue(sawCommonName);
        assertTrue(StarChartFixtures.chart(SINGLE, 1L, 4).headerLines().get(0).plain().startsWith("Tier 4 chart, single star"));
    }

    @Test
    void aRollWhoseModifierLeftThePoolFallsBackToTheCatalogueOrIsDroppedAndFlagged() {
        List<RolledModifier> movedExplicits = List.of(new RolledModifier("HYBRID_CHART_BOSS_SIZE", ModifierSlot.PREFIX,
                List.of(new RolledEffect("CHART_BOSS_SIZE_PERCENT", 100f), new RolledEffect("CHART_QUANTITY_PERCENT", 0f))));
        StarChart moved = (StarChart) Item.create(null, UNGATED_UNIQUE, 4L, movedExplicits, ItemTraits.ofTier(12));
        ChartGenContext movedContext = ChartGenContext.from(moved);

        assertNull(moved.definition().poolEntryFor("HYBRID_CHART_BOSS_SIZE"));
        assertTrue(movedContext.applied().contains(new AppliedEffect(ChartEffect.CHART_BOSS_SIZE_PERCENT, 540f)), movedContext.applied().toString());
        assertTrue(movedContext.applied().contains(new AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 8f)), movedContext.applied().toString());
        assertTrue(moved.effectLines().get(0).plain().contains("540%"), moved.effectLines().toString());

        List<RolledModifier> orphanExplicits = List.of(new RolledModifier("CHART_REMOVED_MODIFIER", ModifierSlot.SUFFIX,
                List.of(new RolledEffect("CHART_EXTRA_FLEETS", 73f))));
        StarChart orphaned = (StarChart) Item.create(null, SINGLE, 4L, orphanExplicits, ItemTraits.ofTier(6));
        ChartGenContext orphanContext = ChartGenContext.from(orphaned);
        List<String> orphanLines = orphaned.effectLines().stream().map(StyledText::plain).toList();

        assertTrue(orphanContext.applied().stream().noneMatch(applied -> applied.effect() == ChartEffect.CHART_EXTRA_FLEETS),
                orphanContext.applied().toString());
        assertEquals(1, orphanLines.size(), orphanLines.toString());
        assertTrue(orphanLines.get(0).contains("Unreadable modifier"), orphanLines.toString());
        assertFalse(orphanLines.get(0).contains("73"), orphanLines.toString());

        ItemDefinition hullDefinition = ItemDefinitions.ofType(ItemType.HULL_ITEM).iterator().next();
        RolledEffect hullLine = new RolledEffect("HULL_MULT", 73f);
        RolledEffect armorLine = new RolledEffect("ARMOR_FLAT", 7f);
        Item hullItem = Item.create(null, hullDefinition.id(), 4L, List.of(new RolledModifier("NOT_IN_ANY_POOL", ModifierSlot.PREFIX,
                List.of(hullLine, armorLine))));
        assertEquals(List.of(hullLine.description().plain(), armorLine.description().plain()),
                hullItem.effectLines().stream().map(StyledText::plain).toList(), "hull items keep showing their rolled values");
    }

    @Test
    void uniqueChartsNeverExistBelowTheirMinimumTier() {
        ItemDefinition gatedUnique = ItemDefinitions.get(GATED_UNIQUE);

        assertEquals(4, StarChartDefinitions.minTier(GATED_UNIQUE));
        assertEquals(StarChart.LOWEST_TIER, StarChartDefinitions.minTier(UNGATED_UNIQUE));
        assertEquals(StarChart.LOWEST_TIER, StarChartDefinitions.minTier(SINGLE));
        assertEquals(4, ItemData.rolledAtTier(gatedUnique, 3L, 1).traits().tier());
        assertEquals(4, ItemData.rolledAtTier(gatedUnique, 3L, 1).preview().tier());
        assertEquals(4, ItemData.rolled(gatedUnique, 3L).preview().tier());
        assertEquals(4, new ItemData(GATED_UNIQUE, 3L).preview().tier());
        assertEquals(9, ItemData.rolledAtTier(gatedUnique, 3L, 9).preview().tier());
        ItemData lowTierData = new ItemData(GATED_UNIQUE, 3L, null, null, ItemTraits.ofTier(2));
        assertEquals(4, lowTierData.preview().tier());
        assertEquals(4, ItemStore.forStarCharts().add(lowTierData).tier());
        assertEquals(4, ItemData.ofStarChart(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, GATED_UNIQUE + "|5")).preview().tier());
        assertTrue(exiledsector.charts.ChartTiers.forTier(StarChartDefinitions.minTier(GATED_UNIQUE)).boss(),
                "the gated unique's boss prefix always has a boss to strengthen");

        Random random = new Random(5);
        boolean gatedUniqueAtTierFour = false;
        for (int draw = 0; draw < 1000; draw++) {
            assertNotEquals(GATED_UNIQUE, ChartDrops.pickChartAtTier(random, 3, definition -> true).id());
            gatedUniqueAtTierFour |= GATED_UNIQUE.equals(ChartDrops.pickChartAtTier(random, 4, definition -> true).id());
        }
        assertTrue(gatedUniqueAtTierFour);

        ItemDefinition ungatedUnique = ItemDefinitions.get(UNGATED_UNIQUE);
        assertFalse(ItemCrafting.canTranspose(ungatedUnique, 3, definition -> true));
        assertTrue(ItemCrafting.canTranspose(ungatedUnique, 4, definition -> true));
        ItemStore store = ItemStore.forStarCharts();
        Item lowUngatedUnique = store.add(ItemData.rolledAtTier(ungatedUnique, 12L, 3));
        assertNull(ItemCrafting.transpose(store, lowUngatedUnique, new Random(5), definition -> true));
    }

    @Test
    void aChartWithoutTraitsRollsAtItsLowestTier() {
        for (long seed = 0; seed < 400; seed++) {
            Item chart = new ItemData(SINGLE, seed).preview();
            assertEquals(StarChart.LOWEST_TIER, chart.tier());
            Set<String> ids = modifierIds(chart);
            assertFalse(ids.contains("HYBRID_CHART_EXTRA_OFFICERS"), "seed " + seed + ": " + ids);
        }
    }

    @Test
    void aTierlessViewListsEveryBandsWholeRange() {
        ModifierLine fleetSize = ModifierLine.parse("CHART_FLEET_SIZE_PERCENT:t1=15-20/t6=50-70/t11=150-200").line();
        ModifierLine sensorRange = ModifierLine.parse("CHART_SENSOR_RANGE_PERCENT:t1=-25--15/t6=-35--25").line();
        ModifierLine extraFleets = ModifierLine.parse("CHART_EXTRA_FLEETS:t1=1/t8=2/t13=3").line();
        ModifierEffects chartEffects = ItemEffects.forType(ItemType.STAR_CHART);

        assertTrue(fleetSize.listsEveryValueFor(ScaleContext.NONE));
        assertEquals("15-20/50-70/150-200% increased enemy fleet size",
                new RolledEffect("CHART_FLEET_SIZE_PERCENT", 50f).scaledValuesDescription(chartEffects, fleetSize).plain());
        assertEquals("15-20/50-70/150-200% increased enemy fleet size",
                new RolledEffect("CHART_FLEET_SIZE_PERCENT", 45f).rangeDescription(chartEffects, fleetSize).plain());
        assertEquals("Your fleet has 15-25/25-35% less sensor range while in the system",
                new RolledEffect("CHART_SENSOR_RANGE_PERCENT", -35f).scaledValuesDescription(chartEffects, sensorRange).plain());
        assertTrue(new RolledEffect("CHART_EXTRA_FLEETS", 3f).scaledValuesDescription(chartEffects, extraFleets).plain().startsWith("1/2/3 "));
        ModifierLine hullValues = ModifierLine.parse("ARMOR_FLAT:5/10/15/20").line();
        assertEquals(new RolledEffect("ARMOR_FLAT", 5f).scaledValuesDescription(ItemEffects.SKILL_EFFECTS, hullValues.scaledValues()).plain(),
                new RolledEffect("ARMOR_FLAT", 5f).scaledValuesDescription(ItemEffects.SKILL_EFFECTS, hullValues).plain());
    }

    @Test
    void aMirroredChartStaysMirroredThroughTheCodecAndTheStore() {
        ItemStore store = ItemStore.forStarCharts();
        Item chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get(SINGLE), 31L, 7));
        Item mirror = store.addMirrorOf(chart);

        String plainEncoded = ItemData.of(chart).encode();
        String mirrorEncoded = ItemData.of(mirror).encode();
        ItemData decodedMirror = ItemData.parse(mirrorEncoded);

        assertFalse(plainEncoded.contains("\"m\""), plainEncoded);
        assertTrue(mirrorEncoded.contains("\"m\":true"), mirrorEncoded);
        assertTrue(decodedMirror.traits().mirrored());
        assertTrue(decodedMirror.preview().mirrored());
        assertEquals(7, decodedMirror.preview().tier());
        assertTrue(ItemStore.forStarCharts().add(decodedMirror).mirrored());
        assertTrue(ItemData.ofStarChart(decodedMirror.toSpecialItem()).preview().mirrored());
        assertFalse(ItemData.parse(plainEncoded).preview().mirrored());
        assertFalse(ItemTraits.ofTier(3).mirrored());
    }

    @Test
    void rollOddsLeaveOutEntriesGatedAboveTheTier() {
        ItemDefinition single = ItemDefinitions.get(SINGLE);

        assertFalse(ItemRollOdds.weightShares(single, 1).containsKey("HYBRID_CHART_EXTRA_OFFICERS"));
        assertTrue(ItemRollOdds.weightShares(single, 3).containsKey("HYBRID_CHART_EXTRA_OFFICERS"));
        assertTrue(ItemRollOdds.weightShares(single).containsKey("HYBRID_CHART_EXTRA_OFFICERS"));
        assertTrue(ItemRollOdds.weightShares(single, 1).containsKey("HYBRID_CHART_BOSS_SIZE"));
        double prefixShare = ItemRollOdds.weightShares(single, 1).entrySet().stream()
                .filter(entry -> single.poolEntryFor(entry.getKey()).slot() == ModifierSlot.PREFIX).mapToDouble(java.util.Map.Entry::getValue).sum();
        assertEquals(1.0, prefixShare, 1e-9);
    }
}
