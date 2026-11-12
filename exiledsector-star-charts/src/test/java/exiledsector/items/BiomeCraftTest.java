package exiledsector.items;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeCraftTest {

    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String AMBUSHERS = "OPENING_WEAPON_DAMAGE_PERCENT_PIRATE_BADLANDS";
    private static final String PLASMATIC = "SHIELD_UPKEEP_MULT_TEST_NEBULA";
    private static final Set<String> BIOME_MODIFIERS = Set.of(CHRONAL, AMBUSHERS);
    private static final String BASE = "test_bridge";
    private static final String RELIC = "test_relic";
    private static final int PLENTY = 1_000_000;
    private final ItemStore store = new ItemStore();

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(CHRONAL, "prefix", "TIMEFLOW_PERCENT:3:5", "Chronal"))
                .put(HullItemRows.modifierRow(AMBUSHERS, "prefix", "OPENING_WEAPON_DAMAGE_PERCENT:10:15", "Ambusher's"))
                .put(HullItemRows.modifierRow(PLASMATIC, "suffix", "SHIELD_UPKEEP_MULT:-20:-15", "of Plasma")));
        ItemDefinitions.register(List.of(
                definition(BASE, false, List.of(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), prefix("FLUX_DISSIPATION_MULT")),
                        List.of(suffix("BEAM_WEAPON_RANGE_PERCENT"), suffix("ARMOR_PERCENT"), suffix("HULL_MULT"))),
                definition(RELIC, true, List.of(prefix("ARMOR_PERCENT")), List.of())));
        BiomeModifiers.register(new JSONArray()
                .put(biomeRow(CHRONAL, "abyssal_rift"))
                .put(biomeRow(AMBUSHERS, "pirate_badlands")));
    }

    @AfterEach
    void tearDown() {
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject biomeRow(String modifierId, String biomeId) throws JSONException {
        return new JSONObject().put("modifier", modifierId).put("biome", biomeId).put("item", "bridge");
    }

    private static PoolEntry prefix(String effectName) {
        return PoolEntry.of(ModifierSlot.PREFIX, effectName, 4f, 6f, 1f);
    }

    private static PoolEntry suffix(String effectName) {
        return PoolEntry.of(ModifierSlot.SUFFIX, effectName, 4f, 6f, 1f);
    }

    private static ItemDefinition definition(String id, boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, ItemKind.BRIDGE, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "", prefixes, suffixes, "");
    }

    private static RolledModifier rolled(ModifierSlot slot, String effectName, float magnitude) {
        return RolledModifier.single(slot, new RolledLine(effectName, magnitude));
    }

    private static RolledModifier biome(String modifierId) {
        return RolledModifier.roll(ItemModifiers.get(modifierId), new Random(1L));
    }

    private Item withModifiers(RolledModifier... modifiers) {
        return store.add(new ItemData(BASE, 11L, List.of(modifiers), null));
    }

    private static CraftingMaterials materials(int feedstockEach, int inhibitorsEach) {
        return materials(feedstockEach, inhibitorsEach, PLENTY);
    }

    private static CraftingMaterials materials(int feedstockEach, int inhibitorsEach, int reshapingCount) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put(InfusedCraft.reshapingId(), reshapingCount);
        BiomeCraft.feedstockIds().forEach(feedstockId -> counts.put(feedstockId, feedstockEach));
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            counts.put(inhibitor.itemId(), inhibitorsEach);
        }
        return CraftingMaterials.storedForTests(counts);
    }

    private static CraftingMaterials plenty() {
        return materials(PLENTY, PLENTY);
    }

    private static long biomeModifierCount(Item item) {
        return item.explicits().stream().map(RolledModifier::modifierId).filter(BiomeModifiers::isBiomeModifier).count();
    }

    private static void registerThreeBiomes() throws JSONException {
        BiomeModifiers.register(new JSONArray()
                .put(biomeRow(CHRONAL, "abyssal_rift"))
                .put(biomeRow(AMBUSHERS, "pirate_badlands"))
                .put(biomeRow(PLASMATIC, "test_nebula")));
    }

    private static void assertDistinctEffectsAndSides(Item item) {
        Set<String> effects = new HashSet<>();
        item.explicits().forEach(modifier -> modifier.lines().forEach(line -> assertTrue(effects.add(line.effectName()), line.effectName())));
        assertTrue(item.explicits().stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count() <= 2);
        assertTrue(item.explicits().stream().filter(modifier -> modifier.slot() == ModifierSlot.SUFFIX).count() <= 2);
    }

    @Test
    void theAdaptiveReforgeRerollsWithOneBiomeModifierAndOneToThreeOthers() {
        Set<Integer> counts = new HashSet<>();
        Set<String> grantedIds = new HashSet<>();
        Random random = new Random(7L);
        CraftingMaterials materials = plenty();
        for (int use = 0; use < 80; use++) {
            Item item = withModifiers(rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f));

            assertEquals(Optional.of(Set.of()), BiomeCraft.ADAPTIVE.use(item, materials, Set.of(), random));

            int count = item.explicits().size();
            assertTrue(count >= 2 && count <= 4, "count " + count);
            counts.add(count);
            assertEquals(1, biomeModifierCount(item));
            item.explicits().stream().map(RolledModifier::modifierId).filter(BIOME_MODIFIERS::contains).forEach(grantedIds::add);
            assertDistinctEffectsAndSides(item);
        }
        assertEquals(Set.of(2, 3, 4), counts);
        assertEquals(BIOME_MODIFIERS, grantedIds);
    }

    @Test
    void theAdaptiveReforgeKeepsLockedAndInhibitedModifiers() {
        for (long seed = 0; seed < 40; seed++) {
            RolledModifier locked = rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f).asImmutable();
            RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
            Item item = withModifiers(locked, armor, rolled(ModifierSlot.PREFIX, "HULL_PERCENT", 5f));

            BiomeCraft.ADAPTIVE.use(item, plenty(), Set.of(CraftingInhibitor.SUFFIX), new Random(seed));

            assertTrue(item.explicits().contains(locked));
            assertTrue(item.explicits().contains(armor));
            assertEquals(1, biomeModifierCount(item));
            assertDistinctEffectsAndSides(item);
        }
    }

    @Test
    void theBiomeGraftSwapsOneModifierForABiomeModifier() {
        for (long seed = 0; seed < 40; seed++) {
            RolledModifier capacity = rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f);
            RolledModifier dissipation = rolled(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 5f);
            RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
            Item item = withModifiers(capacity, dissipation, armor);

            assertTrue(BiomeCraft.GRAFTING.use(item, plenty(), Set.of(), new Random(seed)).isPresent());

            assertEquals(3, item.explicits().size());
            assertEquals(1, biomeModifierCount(item));
            assertTrue(item.explicits().contains(armor));
            assertTrue(item.explicits().contains(capacity) != item.explicits().contains(dissipation));
            assertDistinctEffectsAndSides(item);
        }
    }

    @Test
    void theBiomeGraftReplacesABiomeModifierOnAnItemWithTwo() throws Exception {
        registerThreeBiomes();
        for (long seed = 0; seed < 40; seed++) {
            RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
            Item item = withModifiers(biome(CHRONAL), biome(AMBUSHERS), armor);

            BiomeCraft.GRAFTING.use(item, plenty(), Set.of(), new Random(seed));

            assertEquals(BiomeModifiers.MAX_PER_ITEM, biomeModifierCount(item));
            assertTrue(item.explicits().contains(armor));
            assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(PLASMATIC)));
        }
    }

    @Test
    void theAdaptiveReforgeAddsNoThirdBiomeModifier() throws Exception {
        registerThreeBiomes();
        for (long seed = 0; seed < 40; seed++) {
            Item item = withModifiers(biome(CHRONAL), biome(AMBUSHERS));

            BiomeCraft.ADAPTIVE.use(item, plenty(), Set.of(CraftingInhibitor.PREFIX), new Random(seed));

            assertEquals(BiomeModifiers.MAX_PER_ITEM, biomeModifierCount(item));
            assertFalse(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(PLASMATIC)));
        }
    }

    @Test
    void theBiomeGraftNeedsAModifierItCanReplace() {
        Item empty = withModifiers();
        Item locked = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f).asImmutable());
        Item open = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f));

        assertEquals("chart.biomeCraft.refused.grafting", BiomeCraft.GRAFTING.verdict(empty, Set.of()).refusalKey());
        assertFalse(BiomeCraft.GRAFTING.verdict(locked, Set.of()).allowed());
        assertTrue(BiomeCraft.GRAFTING.verdict(open, Set.of()).allowed());
        assertEquals("ui.infusedCraft.refused.inhibited", BiomeCraft.GRAFTING.verdict(open, Set.of(CraftingInhibitor.PREFIX)).refusalKey());
        assertEquals(Optional.empty(), BiomeCraft.GRAFTING.use(locked, plenty(), Set.of(), new Random(1L)));
    }

    @Test
    void aGraftedSideMustHaveRoomForTheBiomeModifier() {
        RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
        Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f), rolled(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 5f),
                armor);

        assertFalse(BiomeCraft.GRAFTING.wouldChange(item, armor));
        assertEquals(Set.of(), BiomeCraft.GRAFTING.inhibitorsSpent(item, List.of(CraftingInhibitor.SUFFIX)));
        assertEquals(Set.of(CraftingInhibitor.PREFIX), BiomeCraft.GRAFTING.inhibitorsSpent(item, List.of(CraftingInhibitor.PREFIX)));
    }

    @Test
    void onlyTheAdaptiveReforgeSpendsATagInhibitor() {
        Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f), rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f));

        assertEquals(Set.of(CraftingInhibitor.SHIELD), BiomeCraft.ADAPTIVE.inhibitorsSpent(item, List.of(CraftingInhibitor.SHIELD)));
        assertEquals(Set.of(), BiomeCraft.GRAFTING.inhibitorsSpent(item, List.of(CraftingInhibitor.SHIELD)));
    }

    @Test
    void bothCraftsNeedANonUniqueHullItemWithBiomeModifiersForItsPart() {
        Item relic = store.add(new ItemData(RELIC, 1L));
        Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f));

        for (BiomeCraft craft : BiomeCraft.values()) {
            assertFalse(craft.verdict(relic, Set.of()).allowed(), craft.id());
            assertTrue(craft.verdict(item, Set.of()).allowed(), craft.id());
            assertFalse(craft.verdict(null, Set.of()).allowed(), craft.id());
        }
        BiomeModifiers.unregisterAll();
        for (BiomeCraft craft : BiomeCraft.values()) {
            assertFalse(craft.verdict(item, Set.of()).allowed(), craft.id());
        }
    }

    @Test
    void eachCraftCostsItsAmountOfEveryInfusedFeedstockAndOfReshapingNanoswarms() {
        assertEquals(100, BiomeCraft.ADAPTIVE.feedstockCost());
        assertEquals(1000, BiomeCraft.GRAFTING.feedstockCost());
        assertEquals(1, BiomeCraft.ADAPTIVE.reshapingCost());
        assertEquals(2, BiomeCraft.GRAFTING.reshapingCost());
        assertEquals(List.of("exiledSector_threat_feedstock", "exiledSector_shroud_feedstock", "exiledSector_phase_feedstock"), BiomeCraft.feedstockIds());
        for (BiomeCraft craft : BiomeCraft.values()) {
            int cost = craft.feedstockCost();
            CraftingMaterials materials = materials(cost + 5, 3, craft.reshapingCost() + 1);
            Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f), rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f));

            assertTrue(craft.canAfford(materials), craft.id());
            assertTrue(craft.use(item, materials, Set.of(), new Random(3L)).isPresent(), craft.id());

            BiomeCraft.feedstockIds().forEach(feedstockId -> assertEquals(5, materials.count(feedstockId), craft.id() + " " + feedstockId));
            assertEquals(1, materials.count(InfusedCraft.reshapingId()), craft.id());
            assertFalse(craft.canAfford(materials(PLENTY, 3, craft.reshapingCost() - 1)), craft.id());
            assertFalse(craft.canAfford(materials), craft.id());
        }
    }

    @Test
    void aRefusedOrUnaffordableCraftSpendsNothing() {
        for (BiomeCraft craft : BiomeCraft.values()) {
            int cost = craft.feedstockCost();
            Map<String, Integer> counts = new HashMap<>(Map.of("exiledSector_threat_feedstock", cost, "exiledSector_shroud_feedstock", cost,
                    "exiledSector_phase_feedstock", cost - 1, CraftingInhibitor.PREFIX.itemId(), 1));
            CraftingMaterials shortOfPhase = CraftingMaterials.storedForTests(counts);
            Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f), rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f));
            List<RolledModifier> before = List.copyOf(item.explicits());

            assertFalse(craft.canAfford(shortOfPhase), craft.id());
            assertEquals(Optional.empty(), craft.use(item, shortOfPhase, Set.of(CraftingInhibitor.PREFIX), new Random(3L)), craft.id());

            assertEquals(before, item.explicits(), craft.id());
            assertEquals(cost, shortOfPhase.count("exiledSector_threat_feedstock"), craft.id());
            assertEquals(cost - 1, shortOfPhase.count("exiledSector_phase_feedstock"), craft.id());
            assertEquals(1, shortOfPhase.count(CraftingInhibitor.PREFIX.itemId()), craft.id());
        }
    }

    @Test
    void anInhibitorIsSpentOnlyWhenItProtectedAModifierTheCraftWouldChange() {
        CraftingMaterials materials = materials(PLENTY, 2);
        Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f), rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f));

        Optional<Set<CraftingInhibitor>> spent = BiomeCraft.ADAPTIVE.use(item, materials, Set.of(CraftingInhibitor.SUFFIX), new Random(5L));

        assertEquals(Optional.of(Set.of(CraftingInhibitor.SUFFIX)), spent);
        assertEquals(1, materials.count(CraftingInhibitor.SUFFIX.itemId()));
        assertEquals(2, materials.count(CraftingInhibitor.PREFIX.itemId()));
    }
}
