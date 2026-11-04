package exiledsector.items;

import exiledsector.charts.anomaly.stormfront.StormLoot;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantedModifiersTest {

    private static final String STORMPROOF = "EMP_DAMAGE_TAKEN_PERCENT_STORM_FRONT";
    private static final String THUNDERHEAD = "NON_BEAM_ENERGY_WEAPON_REFRACTION_CHANCE_PERCENT_STORM_FRONT";
    private static final String BASE = "test_bridge";
    private static final String RELIC = "test_relic";
    private static final String PLAIN_PREFIX = "TIMEFLOW_PERCENT_PLAIN";
    private static final AffixLayout.Limits LIMITS = new AffixLayout.Limits(2, 4, 2, List.of(0.4f, 0.3f, 0.2f));

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(STORMPROOF, "prefix", "EMP_DAMAGE_TAKEN_PERCENT:-25:-25", "Stormproof"))
                .put(HullItemRows.modifierRow(PLAIN_PREFIX, "prefix", "TIMEFLOW_PERCENT:3:5", "Plain"))
                .put(HullItemRows.modifierRow(THUNDERHEAD, "suffix", "NON_BEAM_ENERGY_WEAPON_REFRACTION_CHANCE_PERCENT:8:8", "Thunderhead")));
        ItemDefinitions.register(List.of(
                definition(BASE, false, List.of(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), prefix("FLUX_DISSIPATION_MULT")),
                        List.of(suffix("BEAM_WEAPON_RANGE_PERCENT"), suffix("ARMOR_PERCENT"), suffix("HULL_MULT"))),
                definition(RELIC, true, List.of(prefix("ARMOR_PERCENT")), List.of())));
        StormLoot.reserveModifiers();
    }

    @AfterEach
    void tearDown() {
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
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

    @Test
    void theStormModifiersAreReservedSoNoPoolRollsThem() {
        assertTrue(ModifierSources.isReserved(STORMPROOF));
        assertTrue(ModifierSources.isReserved(THUNDERHEAD));
        assertEquals(ItemKind.sectionItemTypes(), List.copyOf(ModifierSources.parts(STORMPROOF)));
    }

    @Test
    void grantingRerollsTheItemAroundTheGrantedModifier() {
        ItemData plain = ItemData.rolled(ItemDefinitions.get(BASE), 7L);

        ItemData granted = GrantedModifiers.grant(plain, STORMPROOF, new Random(3L));

        assertEquals(plain.definitionId(), granted.definitionId());
        assertEquals(plain.seed(), granted.seed());
        assertTrue(granted.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(STORMPROOF)));
        Item reloaded = Item.create("reloaded", BASE, 7L, granted.explicits());
        assertTrue(reloaded.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(STORMPROOF)));
    }

    @Test
    void uniqueItemsAreNeverGranted() {
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), 7L);

        assertFalse(GrantedModifiers.canGrant(ItemDefinitions.get(RELIC), STORMPROOF));
        assertSame(relic, GrantedModifiers.grant(relic, STORMPROOF, new Random(1L)));
    }

    @Test
    void addingKeepsTheOtherModifiersWhileTheSideHasRoom() {
        ItemData item = itemWith(prefix("HULL_PERCENT"), suffix("ARMOR_PERCENT"));

        ItemData added = GrantedModifiers.add(item, STORMPROOF, LIMITS, new Random(3L));

        assertEquals(List.of("HULL_PERCENT", STORMPROOF, "ARMOR_PERCENT"), modifierIds(added));
        assertEquals(item.seed(), added.seed());
    }

    @Test
    void aFullSideGivesUpOneModifierForTheAddedOne() {
        ItemData item = itemWith(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), suffix("ARMOR_PERCENT"));

        List<String> added = modifierIds(GrantedModifiers.add(item, STORMPROOF, LIMITS, new Random(3L)));

        assertEquals(3, added.size());
        assertTrue(added.contains(STORMPROOF));
        assertTrue(added.contains("ARMOR_PERCENT"));
        assertTrue(added.contains("HULL_PERCENT") != added.contains("FLUX_CAPACITY_MULT"), added.toString());
    }

    @Test
    void aReservedModifierKeepsItsPlaceOnAFullSide() {
        for (long seed = 0L; seed < 20L; seed++) {
            ItemData stormItem = GrantedModifiers.add(itemWith(prefix("HULL_PERCENT")), STORMPROOF, LIMITS, new Random(seed));

            assertEquals(List.of(STORMPROOF, PLAIN_PREFIX), modifierIds(GrantedModifiers.add(stormItem, PLAIN_PREFIX, LIMITS, new Random(seed))));
        }
    }

    @Test
    void aModifierSharingTheAddedEffectMakesWayForIt() {
        ItemData item = itemWith(prefix("EMP_DAMAGE_TAKEN_PERCENT"), suffix("ARMOR_PERCENT"));

        assertEquals(List.of(STORMPROOF, "ARMOR_PERCENT"), modifierIds(GrantedModifiers.add(item, STORMPROOF, LIMITS, new Random(3L))));
    }

    @Test
    void addingAModifierTheItemHasOrToAUniqueChangesNothing() {
        ItemData item = GrantedModifiers.add(itemWith(prefix("HULL_PERCENT")), STORMPROOF, LIMITS, new Random(3L));
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), 7L);

        assertSame(item, GrantedModifiers.add(item, STORMPROOF, LIMITS, new Random(4L)));
        assertSame(relic, GrantedModifiers.add(relic, STORMPROOF, LIMITS, new Random(4L)));
    }

    private static ItemData itemWith(PoolEntry... entries) {
        Random random = new Random(1L);
        List<RolledModifier> explicits = Arrays.stream(entries).map(entry -> RolledModifier.roll(entry.modifier(), random)).toList();
        return new ItemData(BASE, 7L, explicits, null);
    }

    private static List<String> modifierIds(ItemData item) {
        return item.explicits().stream().map(RolledModifier::modifierId).toList();
    }

    @Test
    void aStormFightsGuaranteedHullItemIsABasicOneCarryingAStormModifier() {
        Random random = new Random(9L);
        for (int fight = 0; fight < 50; fight++) {
            ItemData stormItem = StormLoot.guaranteedItem(random, definition -> true);

            assertEquals(BASE, stormItem.definitionId());
            assertTrue(stormItem.explicits().stream().anyMatch(rolled -> StormLoot.MODIFIER_IDS.contains(rolled.modifierId())), stormItem.toString());
        }
        assertNull(StormLoot.guaranteedItem(random, definition -> false));
    }
}
