package exiledsector.loot;

import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropTypeTest {

    private static Map<DropType, List<DropType>> expectedChains() {
        Map<DropType, List<DropType>> chains = new EnumMap<>(DropType.class);
        chains.put(DropType.ITEM, List.of(DropType.ITEM));
        chains.put(DropType.HULL_ITEM, List.of(DropType.HULL_ITEM, DropType.ITEM));
        for (DropType section : List.of(DropType.BRIDGE, DropType.CREW_QUARTERS, DropType.ENGINE_ROOM, DropType.REACTOR,
                DropType.ARMOR_PLATING, DropType.WEAPON_MOUNT, DropType.SHIELD_GENERATOR, DropType.PHASE_COIL, DropType.FLIGHT_DECK)) {
            chains.put(section, List.of(section, DropType.HULL_ITEM, DropType.ITEM));
        }
        for (DropType category : List.of(DropType.SUBROUTINE, DropType.STAR_CHART, DropType.SCHEMATIC, DropType.CRAFTING_ITEM,
                DropType.FEEDSTOCK, DropType.CATALYST, DropType.WEAPON, DropType.RESOURCE, DropType.BLUEPRINT, DropType.AI_CORE,
                DropType.COLONY_ITEM)) {
            chains.put(category, List.of(category, DropType.ITEM));
        }
        for (DropType subcategory : List.of(DropType.BASIC_CRAFTING_ITEM, DropType.ENGRAM, DropType.INHIBITOR)) {
            chains.put(subcategory, List.of(subcategory, DropType.CRAFTING_ITEM, DropType.ITEM));
        }
        return chains;
    }

    @Test
    void everyDropTypeAddsUpThroughTheQuantityTree() {
        Map<DropType, List<DropType>> expected = expectedChains();

        assertEquals(DropType.values().length, expected.size());
        for (DropType type : DropType.values()) {
            assertEquals(expected.get(type), type.chain(), type.name());
            assertEquals(type.chain().size() > 1 ? type.chain().get(1) : null, type.parent(), type.name());
        }
    }

    @Test
    void theNineSectionsSitUnderHullItemsAndEveryItemKindHasItsOwnNode() {
        for (ItemKind kind : ItemKind.values()) {
            DropType type = DropType.forItemKind(kind);
            assertEquals(kind, type.itemKind());
            assertEquals(kind.fitsSection(), type.isSection(), kind.name());
            assertEquals(kind.fitsSection() ? DropType.HULL_ITEM : DropType.ITEM, type.parent(), kind.name());
        }
        assertNull(DropType.forItemKind(null));
    }

    @Test
    void inhibitorsHaveTheirOwnCraftingNodeAndEveryOtherCraftingItemIsBasic() {
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertEquals(DropType.INHIBITOR, DropType.forCraftingItem(inhibitor.itemId()), inhibitor.name());
        }
        assertEquals(DropType.BASIC_CRAFTING_ITEM, DropType.forCraftingItem("exiledSector_augmentation_nanoswarm"));
        assertEquals(DropType.BASIC_CRAFTING_ITEM, DropType.forCraftingItem("exiledSector_replicator_nanoseed"));
        assertEquals(DropType.BASIC_CRAFTING_ITEM, DropType.forCraftingItem("exiledSector_reforge_cipher"));
    }

    @Test
    void aDefinitionReadsItsKindsNodeOrTheHullItemNodeOrTheRoot() {
        assertEquals(DropType.BRIDGE, DropType.forDefinition(definition(ItemKind.BRIDGE)));
        assertEquals(DropType.SUBROUTINE, DropType.forDefinition(definition(ItemKind.SUBROUTINE)));
    }

    private static ItemDefinition definition(ItemKind kind) {
        return new ItemDefinition("test_" + kind.name(), kind, "test", ItemDefinition.FALLBACK_ICON, 10f, false, "", List.of(), List.of(), null);
    }

    @Test
    void withoutASectorEveryDropTypeCanDrop() {
        for (DropType type : DropType.values()) {
            assertTrue(type.canDrop(null), type.name());
        }
    }
}
