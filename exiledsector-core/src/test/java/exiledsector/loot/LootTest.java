package exiledsector.loot;

import exiledsector.items.ItemData;
import exiledsector.items.ShipSchematic;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LootTest {

    @Test
    void addingLootAppendsItemsAndSchematicsAndSumsMaterialsInTheOrderTheyFirstDropped() {
        ItemData first = mock(ItemData.class);
        ItemData second = mock(ItemData.class);
        ShipSchematic schematic = mock(ShipSchematic.class);
        Map<String, Integer> firstMaterials = new LinkedHashMap<>();
        firstMaterials.put("feedstock", 3);
        firstMaterials.put("swarm", 1);
        Map<String, Integer> secondMaterials = new LinkedHashMap<>();
        secondMaterials.put("cipher", 2);
        secondMaterials.put("feedstock", 4);

        Loot combined = new Loot(List.of(first), List.of(), firstMaterials).plus(new Loot(List.of(second), List.of(schematic), secondMaterials));

        assertEquals(List.of(first, second), combined.items());
        assertEquals(List.of(schematic), combined.schematics());
        assertEquals(List.of("feedstock", "swarm", "cipher"), List.copyOf(combined.materials().keySet()));
        assertEquals(7, combined.materials().get("feedstock"));
    }

    @Test
    void addingNothingKeepsTheSameLootAndReplacingItemsKeepsTheRest() {
        Loot loot = new Loot(List.of(mock(ItemData.class)), List.of(mock(ShipSchematic.class)), Map.of("feedstock", 2));
        ItemData replacement = mock(ItemData.class);

        assertSame(loot, loot.plus(Loot.NONE));
        Loot replaced = loot.withItems(List.of(replacement));
        assertEquals(List.of(replacement), replaced.items());
        assertEquals(loot.schematics(), replaced.schematics());
        assertEquals(loot.materials(), replaced.materials());
        assertTrue(Loot.NONE.isEmpty());
    }
}
