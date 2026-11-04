package exiledsector.items;

import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EngramDataTest {

    @Test
    void theShippedCraftingItemsReserveModifiersThatNoItemPoolLists() throws Exception {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(HullItemRows.shippedRows(ItemModifiers.dataPath()), ItemModifiers.dataPath());
        integrity.checkDefinitions(HullItemRows.shippedRows(SubroutineDefinitions.dataPath()), SubroutineDefinitions.dataPath(), ItemType.SUBROUTINE);
        integrity.checkDefinitions(HullItemRows.shippedRows(HullItem.definitionsPath()), HullItem.definitionsPath(), ItemType.HULL_ITEM);
        integrity.checkKindImplicits(HullItemRows.shippedRows(ItemImplicits.dataPath()), ItemImplicits.dataPath());
        integrity.checkCorruptionPool(HullItemRows.shippedRows(SubroutineImplicits.DATA_PATH), SubroutineImplicits.DATA_PATH,
                "the subroutine corruption pool", ItemType.SUBROUTINE);
        integrity.checkCorruptionPool(HullItemRows.shippedRows(ItemImplicits.corruptionDataPath()), ItemImplicits.corruptionDataPath(),
                "the hull item corruption pool", ItemType.HULL_ITEM);
        JSONArray craftingItems = HullItemRows.shippedRows(Engrams.PATH);
        Engrams.checkRows(integrity, craftingItems, Engrams.PATH);

        List<Violation> violations = integrity.finish();
        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
        List<String> reserved = new ArrayList<>();
        for (int i = 0; i < craftingItems.length(); i++) {
            reserved.add(craftingItems.getJSONObject(i).getString("modifier"));
        }
        assertFalse(reserved.isEmpty());
        assertEquals(List.of(), violations.stream().filter(violation -> reserved.stream().anyMatch(violation.message()::contains)).toList());
    }
}
