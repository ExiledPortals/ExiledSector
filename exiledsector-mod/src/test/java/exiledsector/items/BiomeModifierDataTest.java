package exiledsector.items;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeModifierDataTest {

    private static final String REGIONS_PATH = "data/config/exiledSector/atlas_regions.csv";

    @Test
    void theShippedBiomeModifiersAreReservedHullItemModifiersNoItemPoolLists() throws Exception {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(HullItemRows.shippedRows(ItemModifiers.dataPath()), ItemModifiers.dataPath());
        integrity.checkDefinitions(HullItemRows.shippedRows(SubroutineDefinitions.dataPath()), SubroutineDefinitions.dataPath(), ItemType.SUBROUTINE);
        integrity.checkDefinitions(HullItemRows.shippedRows(HullItem.definitionsPath()), HullItem.definitionsPath(), ItemType.HULL_ITEM);
        integrity.checkKindImplicits(HullItemRows.shippedRows(ItemImplicits.dataPath()), ItemImplicits.dataPath());
        JSONArray biomeRows = HullItemRows.shippedRows(BiomeModifiers.PATH);
        StarChartDataRules.checkBiomeModifiers(integrity, biomeRows);

        List<Violation> violations = integrity.finish();
        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
        List<String> reserved = new ArrayList<>();
        for (int i = 0; i < biomeRows.length(); i++) {
            reserved.add(biomeRows.getJSONObject(i).getString(BiomeModifiers.ID_COLUMN));
        }
        assertFalse(reserved.isEmpty());
        assertEquals(List.of(), violations.stream().filter(violation -> reserved.stream().anyMatch(violation.message()::contains)).toList());
    }

    @Test
    void everyShippedRowNamesAnAtlasRegionAndAHullItemPartOnce() throws Exception {
        Set<String> regionIds = new HashSet<>();
        JSONArray regions = HullItemRows.shippedRows(REGIONS_PATH);
        for (int i = 0; i < regions.length(); i++) {
            regionIds.add(regions.getJSONObject(i).getString("id"));
        }
        Set<String> biomeParts = new HashSet<>();
        JSONArray rows = HullItemRows.shippedRows(BiomeModifiers.PATH);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String biomeId = row.getString("biome");
            ItemKind kind = ItemKind.byIdOrNull(row.getString("item"));
            assertTrue(regionIds.contains(biomeId), biomeId);
            assertNotNull(kind, row.toString());
            assertEquals(ItemType.HULL_ITEM, kind.type(), row.toString());
            assertTrue(biomeParts.add(biomeId + "|" + kind.id()), row.toString());
        }
    }
}
