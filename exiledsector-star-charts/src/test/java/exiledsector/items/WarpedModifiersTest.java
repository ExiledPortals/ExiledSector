package exiledsector.items;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpedModifiersTest {

    private static final String BRIDGE = "test_bridge";
    private static final String REACTOR = "test_reactor";
    private static final String RELIC = "test_relic";
    private static final String BRIDGE_ARMOR = "T_BRIDGE_ARMOR";
    private static final String BRIDGE_HULL = "T_BRIDGE_HULL";
    private static final String VENTS = "T_VENTS";
    private static final String REACTOR_ARMOR = "T_REACTOR_ARMOR";
    private static final String SCALING = "T_SCALING";
    private static final String GROUNDED = "T_GROUNDED";
    private static final String TIMEFLOW = "T_TIMEFLOW";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(BRIDGE_ARMOR, "prefix", "ARMOR_PERCENT:4:6", "Armored"))
                .put(HullItemRows.modifierRow(BRIDGE_HULL, "suffix", "HULL_PERCENT:4:6", "Hulled"))
                .put(HullItemRows.modifierRow(VENTS, "prefix", "VENT_RATE_PERCENT:10:20", "Vented"))
                .put(HullItemRows.modifierRow(REACTOR_ARMOR, "suffix", "ARMOR_PERCENT:1:2; TIMEFLOW_PERCENT:1:2", "Plated"))
                .put(HullItemRows.modifierRow(SCALING, "suffix", "SOCKETED_HULL_ITEM_EFFECT_PERCENT:5:10", "Resonant"))
                .put(HullItemRows.modifierRow(GROUNDED, "suffix", "EMP_DAMAGE_TAKEN_PERCENT:-25:-20", "Grounded"))
                .put(HullItemRows.modifierRow(TIMEFLOW, "prefix", "TIMEFLOW_PERCENT:3:5", "Hasted")));
        ModifierSources.reserve(GROUNDED, "test_owner", ItemKind.sectionItemTypes());
        ItemDefinitions.register(List.of(
                definition(BRIDGE, ItemKind.BRIDGE, false, List.of(entry(BRIDGE_ARMOR)), List.of(entry(BRIDGE_HULL))),
                definition(REACTOR, ItemKind.REACTOR, false, List.of(entry(VENTS)), List.of(entry(REACTOR_ARMOR), entry(SCALING), entry(GROUNDED))),
                definition(RELIC, ItemKind.ENGINE_ROOM, true, List.of(entry(TIMEFLOW)), List.of())));
    }

    @AfterEach
    void tearDown() {
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static PoolEntry entry(String modifierId) {
        return new PoolEntry(ItemModifiers.get(modifierId), 1f);
    }

    private static ItemDefinition definition(String id, ItemKind kind, boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "", prefixes, suffixes, "");
    }

    private static List<String> poolIds(String definitionId) {
        return WarpedModifiers.poolFor(ItemDefinitions.get(definitionId)).stream().map(entry -> entry.modifier().id()).toList();
    }

    @Test
    void theWarpPoolLeavesOutOwnSectionEffectsReservedUniqueAndPartBoundModifiers() {
        assertEquals(List.of(VENTS), poolIds(BRIDGE));
        assertEquals(List.of(BRIDGE_HULL), poolIds(REACTOR));
        assertTrue(WarpedModifiers.partBound(ItemModifiers.get(SCALING)));
        assertFalse(WarpedModifiers.partBound(ItemModifiers.get(VENTS)));
    }

    @Test
    void warpingAddsOneWarpedModifierFromAnotherSection() {
        for (long seed = 0L; seed < 20L; seed++) {
            ItemData warped = WarpedModifiers.warp(ItemData.rolled(ItemDefinitions.get(BRIDGE), seed), new Random(seed));

            List<RolledModifier> warpedModifiers = warped.explicits().stream().filter(RolledModifier::warped).toList();
            assertEquals(1, warpedModifiers.size());
            assertEquals(VENTS, warpedModifiers.get(0).modifierId());
            assertTrue(warped.preview().isWarped());
        }
    }

    @Test
    void aWarpNeverDisplacesAModifierSharingItsEffect() {
        RolledModifier grounded = RolledModifier.roll(ItemModifiers.get(GROUNDED), new Random(1L));
        RolledModifier vented = RolledModifier.roll(ItemModifiers.get(VENTS), new Random(1L));
        ItemData carrying = new ItemData(BRIDGE, 7L, List.of(vented, grounded), null);

        assertSame(carrying, WarpedModifiers.warp(carrying, new Random(2L)));
    }

    @Test
    void anItemIsWarpedAtMostOnceAndUniquesNever() {
        ItemData warped = WarpedModifiers.warp(ItemData.rolled(ItemDefinitions.get(REACTOR), 3L), new Random(3L));
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), 3L);

        assertSame(warped, WarpedModifiers.warp(warped, new Random(4L)));
        assertSame(relic, WarpedModifiers.warp(relic, new Random(4L)));
    }
}
