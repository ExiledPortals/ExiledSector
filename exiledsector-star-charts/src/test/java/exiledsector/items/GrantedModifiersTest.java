package exiledsector.items;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantedModifiersTest {

    private static final String GROUNDED = "EMP_DAMAGE_TAKEN_PERCENT_GRANTED";
    private static final String BASE = "test_bridge";
    private static final String RELIC = "test_relic";
    private static final String PLAIN_PREFIX = "TIMEFLOW_PERCENT_PLAIN";
    private static final AffixLayout.Limits LIMITS = new AffixLayout.Limits(2, 4, 2, List.of(0.4f, 0.3f, 0.2f));

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(GROUNDED, "prefix", "EMP_DAMAGE_TAKEN_PERCENT:-25:-25", "Grounded"))
                .put(HullItemRows.modifierRow(PLAIN_PREFIX, "prefix", "TIMEFLOW_PERCENT:3:5", "Plain")));
        ItemDefinitions.register(List.of(
                definition(BASE, false, List.of(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), prefix("FLUX_DISSIPATION_MULT")),
                        List.of(suffix("BEAM_WEAPON_RANGE_PERCENT"), suffix("ARMOR_PERCENT"), suffix("HULL_MULT"))),
                definition(RELIC, true, List.of(prefix("ARMOR_PERCENT")), List.of())));
        ModifierSources.reserve(GROUNDED, "test_owner", ItemKind.sectionItemTypes());
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
    void grantingRerollsTheItemAroundTheGrantedModifier() {
        ItemData plain = ItemData.rolled(ItemDefinitions.get(BASE), 7L);

        ItemData granted = GrantedModifiers.grant(plain, GROUNDED, new Random(3L));

        assertEquals(plain.definitionId(), granted.definitionId());
        assertEquals(plain.seed(), granted.seed());
        assertTrue(granted.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(GROUNDED)));
        Item reloaded = Item.create("reloaded", BASE, 7L, granted.explicits());
        assertTrue(reloaded.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(GROUNDED)));
    }

    @Test
    void uniqueItemsAreNeverGranted() {
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), 7L);

        assertFalse(GrantedModifiers.canGrant(ItemDefinitions.get(RELIC), GROUNDED));
        assertSame(relic, GrantedModifiers.grant(relic, GROUNDED, new Random(1L)));
    }

    @Test
    void addingKeepsTheOtherModifiersWhileTheSideHasRoom() {
        ItemData item = itemWith(prefix("HULL_PERCENT"), suffix("ARMOR_PERCENT"));

        ItemData added = GrantedModifiers.add(item, GROUNDED, LIMITS, new Random(3L));

        assertEquals(List.of("HULL_PERCENT", GROUNDED, "ARMOR_PERCENT"), modifierIds(added));
        assertEquals(item.seed(), added.seed());
    }

    @Test
    void aFullSideGivesUpOneModifierForTheAddedOne() {
        ItemData item = itemWith(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), suffix("ARMOR_PERCENT"));

        List<String> added = modifierIds(GrantedModifiers.add(item, GROUNDED, LIMITS, new Random(3L)));

        assertEquals(3, added.size());
        assertTrue(added.contains(GROUNDED));
        assertTrue(added.contains("ARMOR_PERCENT"));
        assertTrue(added.contains("HULL_PERCENT") != added.contains("FLUX_CAPACITY_MULT"), added.toString());
    }

    @Test
    void aReservedModifierKeepsItsPlaceOnAFullSide() {
        for (long seed = 0L; seed < 20L; seed++) {
            ItemData grantedItem = GrantedModifiers.add(itemWith(prefix("HULL_PERCENT")), GROUNDED, LIMITS, new Random(seed));

            assertEquals(List.of(GROUNDED, PLAIN_PREFIX), modifierIds(GrantedModifiers.add(grantedItem, PLAIN_PREFIX, LIMITS, new Random(seed))));
        }
    }

    @Test
    void aModifierSharingTheAddedEffectMakesWayForIt() {
        ItemData item = itemWith(prefix("EMP_DAMAGE_TAKEN_PERCENT"), suffix("ARMOR_PERCENT"));

        assertEquals(List.of(GROUNDED, "ARMOR_PERCENT"), modifierIds(GrantedModifiers.add(item, GROUNDED, LIMITS, new Random(3L))));
    }

    @Test
    void addingAModifierTheItemHasOrToAUniqueChangesNothing() {
        ItemData item = GrantedModifiers.add(itemWith(prefix("HULL_PERCENT")), GROUNDED, LIMITS, new Random(3L));
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), 7L);

        assertSame(item, GrantedModifiers.add(item, GROUNDED, LIMITS, new Random(4L)));
        assertSame(relic, GrantedModifiers.add(relic, GROUNDED, LIMITS, new Random(4L)));
    }

    private static ItemData itemWith(PoolEntry... entries) {
        Random random = new Random(1L);
        List<RolledModifier> explicits = Arrays.stream(entries).map(entry -> RolledModifier.roll(entry.modifier(), random)).toList();
        return new ItemData(BASE, 7L, explicits, null);
    }

    private static List<String> modifierIds(ItemData item) {
        return item.explicits().stream().map(RolledModifier::modifierId).toList();
    }
}
