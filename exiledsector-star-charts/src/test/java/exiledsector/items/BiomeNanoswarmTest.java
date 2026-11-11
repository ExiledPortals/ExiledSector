package exiledsector.items;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeNanoswarmTest {

    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String AMBUSHERS = "OPENING_WEAPON_DAMAGE_PERCENT_PIRATE_BADLANDS";
    private static final String PLASMATIC = "SHIELD_UPKEEP_MULT_TEST_NEBULA";
    private static final Set<String> BIOME_MODIFIERS = Set.of(CHRONAL, AMBUSHERS);
    private static final String BASE = "test_bridge";
    private static final String RELIC = "test_relic";
    private final ItemStore store = new ItemStore();
    private final CraftingContext openContext = new CraftingContext(store, definition -> true, Set.of());

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
    void theAdaptiveNanoswarmRerollsWithOneBiomeModifierAndOneToThreeOthers() {
        Set<Integer> counts = new HashSet<>();
        Set<String> grantedIds = new HashSet<>();
        Random random = new Random(7L);
        for (int use = 0; use < 80; use++) {
            Item item = withModifiers(rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f));

            assertSame(item, BiomeNanoswarm.ADAPTIVE.use(item, openContext, random));

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
    void theAdaptiveNanoswarmKeepsLockedAndInhibitedModifiers() {
        for (long seed = 0; seed < 40; seed++) {
            RolledModifier locked = rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f).asImmutable();
            RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
            Item item = withModifiers(locked, armor, rolled(ModifierSlot.PREFIX, "HULL_PERCENT", 5f));
            CraftingContext suffixInhibitor = new CraftingContext(store, definition -> true, Set.of(ModifierSlot.SUFFIX));

            BiomeNanoswarm.ADAPTIVE.use(item, suffixInhibitor, new Random(seed));

            assertTrue(item.explicits().contains(locked));
            assertTrue(item.explicits().contains(armor));
            assertEquals(1, biomeModifierCount(item));
            assertDistinctEffectsAndSides(item);
        }
    }

    @Test
    void theGraftingNanoswarmSwapsOneModifierForABiomeModifier() {
        for (long seed = 0; seed < 40; seed++) {
            RolledModifier capacity = rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f);
            RolledModifier dissipation = rolled(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 5f);
            RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
            Item item = withModifiers(capacity, dissipation, armor);

            assertSame(item, BiomeNanoswarm.GRAFTING.use(item, openContext, new Random(seed)));

            assertEquals(3, item.explicits().size());
            assertEquals(1, biomeModifierCount(item));
            assertTrue(item.explicits().contains(armor));
            assertTrue(item.explicits().contains(capacity) != item.explicits().contains(dissipation));
            assertDistinctEffectsAndSides(item);
        }
    }

    @Test
    void theGraftingNanoswarmReplacesABiomeModifierOnAnItemWithTwo() throws Exception {
        registerThreeBiomes();
        for (long seed = 0; seed < 40; seed++) {
            RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
            Item item = withModifiers(biome(CHRONAL), biome(AMBUSHERS), armor);

            BiomeNanoswarm.GRAFTING.use(item, openContext, new Random(seed));

            assertEquals(BiomeModifiers.MAX_PER_ITEM, biomeModifierCount(item));
            assertTrue(item.explicits().contains(armor));
            assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(PLASMATIC)));
        }
    }

    @Test
    void theAdaptiveNanoswarmAddsNoThirdBiomeModifier() throws Exception {
        registerThreeBiomes();
        CraftingContext prefixInhibitor = new CraftingContext(store, definition -> true, Set.of(ModifierSlot.PREFIX));
        for (long seed = 0; seed < 40; seed++) {
            Item item = withModifiers(biome(CHRONAL), biome(AMBUSHERS));

            BiomeNanoswarm.ADAPTIVE.use(item, prefixInhibitor, new Random(seed));

            assertEquals(BiomeModifiers.MAX_PER_ITEM, biomeModifierCount(item));
            assertFalse(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(PLASMATIC)));
        }
    }

    @Test
    void theGraftingNanoswarmNeedsAModifierItCanReplace() {
        Item empty = withModifiers();
        Item locked = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f).asImmutable());
        Item open = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f));
        CraftingContext prefixInhibitor = new CraftingContext(store, definition -> true, Set.of(ModifierSlot.PREFIX));

        assertFalse(BiomeNanoswarm.GRAFTING.canUse(empty, openContext));
        assertFalse(BiomeNanoswarm.GRAFTING.canUse(locked, openContext));
        assertTrue(BiomeNanoswarm.GRAFTING.canUse(open, openContext));
        assertFalse(BiomeNanoswarm.GRAFTING.canUse(open, prefixInhibitor));
        assertNull(BiomeNanoswarm.GRAFTING.use(locked, openContext, new Random(1L)));
    }

    @Test
    void aGraftedSideMustHaveRoomForTheBiomeModifier() {
        RolledModifier armor = rolled(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 5f);
        Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f), rolled(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 5f),
                armor);

        assertFalse(BiomeNanoswarm.GRAFTING.wouldChange(item, armor));
        assertEquals(Set.of(), ItemCrafting.inhibitorsSpentBy(BiomeNanoswarm.GRAFTING, item, List.of(CraftingInhibitor.SUFFIX)));
        assertEquals(Set.of(CraftingInhibitor.PREFIX), ItemCrafting.inhibitorsSpentBy(BiomeNanoswarm.GRAFTING, item,
                List.of(CraftingInhibitor.PREFIX)));
    }

    @Test
    void bothNanoswarmsNeedANonUniqueHullItemWithBiomeModifiersForItsPart() {
        Item relic = store.add(new ItemData(RELIC, 1L));
        Item item = withModifiers(rolled(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 5f));

        for (BiomeNanoswarm craftingItem : BiomeNanoswarm.values()) {
            assertFalse(craftingItem.canUse(relic, openContext));
            assertTrue(craftingItem.canUse(item, openContext));
            assertFalse(craftingItem.appliesTo(ItemType.STAR_CHART));
            assertFalse(craftingItem.dropsFromShipLoot());
            assertFalse(craftingItem.growable());
        }
        BiomeModifiers.unregisterAll();
        for (BiomeNanoswarm craftingItem : BiomeNanoswarm.values()) {
            assertFalse(craftingItem.canUse(item, openContext));
        }
    }

    @Test
    void theNanoswarmsAreFoundByItemId() {
        assertSame(BiomeNanoswarm.ADAPTIVE, BiomeNanoswarm.byItemId("exiledSector_adaptive_nanoswarm"));
        assertSame(BiomeNanoswarm.GRAFTING, BiomeNanoswarm.byItemId("exiledSector_grafting_nanoswarm"));
        assertNull(BiomeNanoswarm.byItemId("exiledSector_immutable_nanoswarm"));
    }
}
