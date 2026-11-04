package exiledsector.items;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModifierPartsDataTest {

    @BeforeEach
    void loadRealData() throws Exception {
        ShippedModifierParts.load();
    }

    @AfterEach
    void clear() {
        ShippedModifierParts.clear();
    }

    private static List<String> hullItemModifierIds() throws Exception {
        JSONArray rows = HullItemRows.shippedRows(ItemModifiers.dataPath());
        List<String> modifierIds = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            String modifierId = rows.getJSONObject(i).getString("id");
            if (ItemModifiers.itemTypes(modifierId).contains(ItemType.HULL_ITEM)) {
                modifierIds.add(modifierId);
            }
        }
        return modifierIds;
    }

    @Test
    void everyShippedHullItemModifierCanAppearOnAtLeastOnePart() throws Exception {
        List<String> modifierIds = hullItemModifierIds();
        List<String> orphans = modifierIds.stream()
                .filter(modifierId -> ModifierParts.parts(modifierId).isEmpty())
                .toList();

        assertFalse(modifierIds.isEmpty(), "no hull item modifiers loaded");
        assertEquals(List.of(), orphans, "no pool, unique, implicit, corruption pool or source gives these a part");
    }
}
