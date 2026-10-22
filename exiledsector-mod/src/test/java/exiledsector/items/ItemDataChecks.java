package exiledsector.items;

import exiledlib.items.ItemImplicits;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemType;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

final class ItemDataChecks {

    private ItemDataChecks() {
    }

    static List<ItemDataIntegrity.Violation> check(JSONArray modifierRows, JSONArray subroutineRows, JSONArray hullItemRows, JSONArray implicitRows)
            throws JSONException {
        return checkWithStarCharts(modifierRows, subroutineRows, hullItemRows, implicitRows, new JSONArray(), new JSONArray(), new JSONArray());
    }

    static List<ItemDataIntegrity.Violation> checkWithStarCharts(JSONArray modifierRows, JSONArray subroutineRows, JSONArray hullItemRows,
                                                                 JSONArray implicitRows, JSONArray chartModifierRows, JSONArray chartRows,
                                                                 JSONArray chartImplicitRows) throws JSONException {
        return checkAll(modifierRows, subroutineRows, hullItemRows, implicitRows, chartModifierRows, chartRows, chartImplicitRows, new JSONArray(),
                new JSONArray());
    }

    static List<ItemDataIntegrity.Violation> checkAll(JSONArray modifierRows, JSONArray subroutineRows, JSONArray hullItemRows,
                                                      JSONArray implicitRows, JSONArray chartModifierRows, JSONArray chartRows,
                                                      JSONArray chartImplicitRows, JSONArray subroutineImplicitRows,
                                                      JSONArray hullItemCorruptionRows) throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(modifierRows, ItemModifiers.dataPath());
        integrity.checkModifiers(chartModifierRows, StarChartItems.MODIFIERS_PATH);
        integrity.checkDefinitions(ItemDataIntegrity.orEmpty(subroutineRows), SubroutineDefinitions.dataPath(), ItemType.SUBROUTINE);
        integrity.checkDefinitions(hullItemRows, HullItem.definitionsPath(), ItemType.HULL_ITEM);
        integrity.checkDefinitions(chartRows, StarChartDefinitions.DATA_PATH, ItemType.STAR_CHART);
        integrity.checkKindImplicits(implicitRows, ItemImplicits.dataPath());
        StarChartDataRules.checkImplicits(integrity, chartImplicitRows);
        integrity.checkCorruptionPool(subroutineImplicitRows, SubroutineImplicits.DATA_PATH, "the subroutine corruption pool", ItemType.SUBROUTINE);
        integrity.checkCorruptionPool(hullItemCorruptionRows, ItemImplicits.corruptionDataPath(), "the hull item corruption pool",
                ItemType.HULL_ITEM);
        return integrity.finish();
    }
}
