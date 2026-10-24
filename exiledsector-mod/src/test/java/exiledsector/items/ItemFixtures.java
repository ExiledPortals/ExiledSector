package exiledsector.items;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static exiledsector.items.HullItemRows.MILITARY;
import static exiledsector.items.HullItemRows.MILITARY_PREFIXES;
import static exiledsector.items.HullItemRows.MILITARY_SECTION;
import static exiledsector.items.HullItemRows.MILITARY_SUFFIXES;
import static exiledsector.items.HullItemRows.row;
import static exiledsector.items.HullItemRows.shippedRows;

final class ItemFixtures {

    static final String SUBROUTINE = "test_subroutine";

    private ItemFixtures() {
    }

    static JSONObject subroutineRow(String id) throws JSONException {
        return subroutineRow(id, SubroutineKind.MODSPEC_CACHE);
    }

    static JSONObject subroutineRow(String id, SubroutineKind subroutineKind) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", "Test Subroutine")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20")
                .put(SubroutineDefinitions.SUBROUTINE_KIND_COLUMN, subroutineKind.id());
    }

    static void registerShippedItems() throws Exception {
        ItemModifiers.register(shippedRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(shippedRows(SubroutineDefinitions.dataPath()), shippedRows(HullItem.definitionsPath()));
    }

    static ItemDefinition registerMilitaryAndSubroutine() throws JSONException {
        ItemDefinitions.register(new JSONArray().put(row(MILITARY, MILITARY_SECTION, MILITARY_PREFIXES, MILITARY_SUFFIXES)).put(subroutineRow(SUBROUTINE)));
        return ItemDefinitions.get(SUBROUTINE);
    }
}
