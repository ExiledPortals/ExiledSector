package exiledsector.items;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierPartsDataTest {

    private static final Set<String> AWAITING_A_POOL = Set.of(
            "ENERGY_WEAPON_FLUX_COST_PERCENT",
            "WEAPON_TURN_RATE_PERCENT_BRIDGE");

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
                .filter(modifierId -> !AWAITING_A_POOL.contains(modifierId))
                .filter(modifierId -> ModifierParts.parts(modifierId).isEmpty())
                .toList();

        assertTrue(modifierIds.size() > AWAITING_A_POOL.size(), "no hull item modifiers loaded");
        assertEquals(List.of(), orphans, "no pool, unique, implicit, corruption pool or source gives these a part");
    }

    @Test
    void theExemptModifiersAreStillShippedWithoutAPart() throws Exception {
        List<String> modifierIds = hullItemModifierIds();
        for (String exempt : AWAITING_A_POOL) {
            assertTrue(modifierIds.contains(exempt), exempt + " is gone, so drop its exemption");
            assertEquals(Set.of(), ModifierParts.parts(exempt), exempt + " now has a part, so drop its exemption");
        }
    }
}
