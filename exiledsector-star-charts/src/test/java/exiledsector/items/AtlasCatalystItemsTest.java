package exiledsector.items;

import exiledsector.charts.atlas.AtlasCatalysts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasCatalystItemsTest {

    private static final AtlasCatalysts.Catalyst ORDERS = new AtlasCatalysts.Catalyst("test_orders", "supply_convoy", AtlasCatalysts.Family.ORDERS, 1f, 1);
    private static final AtlasCatalysts.Catalyst SURVEY = new AtlasCatalysts.Catalyst("test_survey", "relay_network", AtlasCatalysts.Family.SURVEY, 1f, 1);
    private static final AtlasCatalysts.Catalyst TERMINUS = new AtlasCatalysts.Catalyst("test_terminus", "spatial_breach", AtlasCatalysts.Family.TERMINUS,
            1f, 1);
    private static final AtlasCatalysts.Catalyst UNCARRIED = new AtlasCatalysts.Catalyst("test_uncarried", "asteroid_storm",
            AtlasCatalysts.Family.TERMINUS, 1f, 1);

    @AfterEach
    void tearDown() {
        AtlasCatalysts.resetForTests();
    }

    @Test
    void carriedCatalystsAreGroupedByFamilyAndSkipWhatTheFleetDoesNotCarry() {
        AtlasCatalysts.useForTests(List.of(ORDERS, SURVEY, UNCARRIED, TERMINUS));
        Map<String, Integer> stored = new HashMap<>(Map.of(ORDERS.itemId(), 1, SURVEY.itemId(), 2, TERMINUS.itemId(), 3));
        CraftingMaterials materials = new CraftingMaterials(null, stored, true);

        assertEquals(List.of(new AtlasCatalystItems.Carried(TERMINUS, 3), new AtlasCatalystItems.Carried(SURVEY, 2),
                new AtlasCatalystItems.Carried(ORDERS, 1)), AtlasCatalystItems.carried(materials));
        assertEquals(6, AtlasCatalystItems.carriedTotal(materials));
    }
}
