package exiledsector.items;

import exiledsector.items.hull.HullItem;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.subroutine.SubroutineDefinitions;
import exiledsector.items.subroutine.SubroutineKind;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static exiledsector.items.HullItemRows.MILITARY;
import static exiledsector.items.HullItemRows.MILITARY_PREFIXES;
import static exiledsector.items.HullItemRows.MILITARY_SECTION;
import static exiledsector.items.HullItemRows.MILITARY_SUFFIXES;
import static exiledsector.items.HullItemRows.row;
import static exiledsector.items.HullItemRows.shippedRows;

public final class ItemFixtures {

    public static final String SUBROUTINE = "test_subroutine";

    private ItemFixtures() {
    }

    public static JSONObject subroutineRow(String id) throws JSONException {
        return subroutineRow(id, SubroutineKind.MODSPEC_CACHE);
    }

    public static JSONObject subroutineRow(String id, SubroutineKind subroutineKind) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", "Test Subroutine")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20")
                .put(SubroutineDefinitions.SUBROUTINE_KIND_COLUMN, subroutineKind.id());
    }

    public static void registerShippedItems() throws Exception {
        ItemModifiers.register(shippedRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(shippedRows(SubroutineDefinitions.dataPath()), shippedRows(HullItem.definitionsPath()));
    }

    public static ItemDefinition registerMilitaryAndSubroutine() throws JSONException {
        ItemDefinitions.register(new JSONArray().put(row(MILITARY, MILITARY_SECTION, MILITARY_PREFIXES, MILITARY_SUFFIXES)).put(subroutineRow(SUBROUTINE)));
        return ItemDefinitions.get(SUBROUTINE);
    }
}
