package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniquePoolRepairTest {

    private static final Modifier ZEAL = new Modifier("zeal", ModifierSlot.PREFIX, List.of(ModifierLine.range("ARMOR_PERCENT", 30f, 50f)));
    private static final Modifier PENANCE = new Modifier("penance", ModifierSlot.PREFIX, List.of(ModifierLine.range("PEAK_CR_DURATION_PERCENT", -40f, -30f)));
    private static final Modifier VOW = new Modifier("vow", ModifierSlot.SUFFIX, List.of(ModifierLine.range("HULL_PERCENT", 5f, 5f)));
    private static final RolledModifier OWNED_PENANCE = new RolledModifier("penance", ModifierSlot.PREFIX,
            List.of(new RolledEffect("PEAK_CR_DURATION_PERCENT", -35f)));
    private static final RolledModifier RETIRED_SPEED = new RolledModifier("speed", ModifierSlot.PREFIX,
            List.of(new RolledEffect("TOP_SPEED_PERCENT", 30f)));
    private static final RolledModifier UNLOADED = new RolledModifier("unloaded", ModifierSlot.PREFIX,
            List.of(new RolledEffect("NOT_A_LOADED_EFFECT", 10f)));

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
    }

    private static void register(boolean unique, String mechanic) {
        ItemDefinitions.register(List.of(new ItemDefinition("cell", ItemKind.BRIDGE, "Cell", ItemDefinition.FALLBACK_ICON, 1f, unique, "",
                List.of(new PoolEntry(ZEAL, 1f), new PoolEntry(PENANCE, 1f)), List.of(new PoolEntry(VOW, 1f)), "", null, null, List.of(),
                ItemDefinition.NO_SOCKET_LIMIT, mechanic)));
    }

    private static List<String> modifierIds(Item item) {
        return item.explicits().stream().map(RolledModifier::modifierId).toList();
    }

    @Test
    void aUniqueFollowsItsCurrentPoolOnLoad() {
        register(true, null);
        Item owned = Item.create("cell_1", "cell", 7L, List.of(RETIRED_SPEED, OWNED_PENANCE));

        assertEquals(List.of("penance", "zeal", "vow"), modifierIds(owned));
        assertEquals(OWNED_PENANCE, owned.explicits().get(0));
        float zeal = owned.explicits().get(1).lines().get(0).magnitude();
        assertTrue(zeal >= 30f && zeal <= 50f);
        assertEquals(5f, owned.explicits().get(2).lines().get(0).magnitude(), 0.001f);
        assertEquals(zeal, Item.create("cell_2", "cell", 7L, List.of(OWNED_PENANCE)).explicits().get(1).lines().get(0).magnitude(), 0.001f,
                "the added roll comes from the item's seed");
    }

    @Test
    void aModifierWhoseEffectNoLongerLoadsIsKept() {
        register(true, null);
        Item owned = Item.create("cell_1", "cell", 7L, List.of(UNLOADED, OWNED_PENANCE));

        assertEquals(List.of("unloaded", "penance", "zeal", "vow"), modifierIds(owned));
    }

    @Test
    void ordinaryAndOneOfItemsKeepTheirRolls() {
        register(false, null);
        assertEquals(List.of("speed", "penance"), modifierIds(Item.create("cell_1", "cell", 7L, List.of(RETIRED_SPEED, OWNED_PENANCE))));

        ItemDefinitions.clear();
        register(true, ItemDefinition.ONE_OF_MECHANIC);
        assertEquals(List.of("penance"), modifierIds(Item.create("cell_2", "cell", 7L, List.of(OWNED_PENANCE))));
    }
}
