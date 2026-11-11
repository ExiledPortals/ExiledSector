package exiledsector.items;

import exiledsector.charts.effect.ChartEffects;
import exiledsector.modifiers.Modifier;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartModifierTagsTest {

    @BeforeEach
    void registerEffects() {
        ItemModifiers.clear();
        ChartEffects.register();
    }

    @AfterEach
    void clear() {
        ItemModifiers.clear();
    }

    @Test
    void chartModifiersReadTheirTagsAndNone() throws Exception {
        ItemModifiers.registerPart(ModifierScope.STAR_CHART, new JSONArray()
                .put(HullItemRows.modifierRow("TAGGED_FLEET_SIZE", "prefix", "star_chart", "CHART_FLEET_SIZE_PERCENT:low=4-5/mid=12-18/high=38-50", "Fortified")
                        .put(ItemModifiers.TAGS_COLUMN, "enemy;fleet_size"))
                .put(HullItemRows.modifierRow("TAGLESS_EXTRA_FLEETS", "prefix", "star_chart", "CHART_EXTRA_FLEETS:low=1/mid=2/high=3", "Crowded")
                        .put(ItemModifiers.TAGS_COLUMN, "none")), StarChartItems.MODIFIERS_PATH);

        Modifier tagged = ItemModifiers.get("TAGGED_FLEET_SIZE");
        assertEquals(List.of("enemy", "fleet_size"), List.copyOf(tagged.tags()));
        assertFalse(tagged.tagless());
        assertTrue(ItemModifiers.get("TAGLESS_EXTRA_FLEETS").tagless());
    }

    @Test
    void theShippedFileStillLoadsEveryRowWithoutATagsColumn() throws Exception {
        JSONArray rows = HullItemRows.shippedRows(StarChartItems.MODIFIERS_PATH);
        for (int i = 0; i < rows.length(); i++) {
            rows.getJSONObject(i).remove(ItemModifiers.TAGS_COLUMN);
        }
        ItemModifiers.registerPart(ModifierScope.STAR_CHART, rows, StarChartItems.MODIFIERS_PATH);

        assertEquals(rows.length(), ItemModifiers.all().size());
        assertTrue(ItemModifiers.all().stream().noneMatch(Modifier::tagless));
        assertTrue(ItemModifiers.all().stream().allMatch(modifier -> modifier.tags().isEmpty()));
    }
}
