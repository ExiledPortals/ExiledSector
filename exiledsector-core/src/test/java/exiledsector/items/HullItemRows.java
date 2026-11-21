package exiledsector.items;

import exiledsector.ProjectFiles;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.name.FrozenName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class HullItemRows {

    public static final String MILITARY = "military_bridge";
    public static final String MILITARY_SECTION = "bridge";
    public static final String MILITARY_PREFIXES = "FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6; BEAM_WEAPON_DAMAGE_PERCENT:10:15";
    public static final String MILITARY_SUFFIXES = "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; ARMOR_PERCENT:6:9; HULL_MULT:4:6";
    static final String EVERY_ITEM_TYPE = "hull_item";

    private HullItemRows() {
    }

    public static ItemData data(String definitionId, long seed, List<RolledLine> effects, FrozenName frozenName) {
        return new ItemData(definitionId, seed, TestItems.explicits(ItemDefinitions.get(definitionId), effects), frozenName);
    }

    public static JSONObject row(String id, String section, String prefixes) throws JSONException {
        return row(id, section, prefixes, "");
    }

    public static JSONObject row(String id, String section, String prefixes, String suffixes) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", "Military-grade Bridge")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20")
                .put("prefixes", prefixes)
                .put("suffixes", suffixes)
                .put("section", section);
    }

    public static JSONObject modifierRow(String id, String slot, String lines, String name) throws JSONException {
        return modifierRow(id, slot, EVERY_ITEM_TYPE, lines, name);
    }

    public static JSONObject modifierRow(String id, String slot, String itemTypes, String lines, String name) throws JSONException {
        return new JSONObject().put("id", id).put("affix", slot).put("scopes", itemTypes).put("lines", lines).put("name", name);
    }

    public static JSONArray shippedRows(String dataPath) throws Exception {
        return ProjectFiles.csvRows(dataPath);
    }

    public static void registerModifierNames(String... idSlotLinesNames) throws JSONException {
        JSONArray rows = new JSONArray();
        for (String row : idSlotLinesNames) {
            String[] fields = row.split(",", -1);
            rows.put(modifierRow(fields[0], fields[1], fields[2], fields[3]));
        }
        ItemModifiers.register(rows);
    }

    public static ItemDefinition registerMilitary() throws JSONException {
        ItemDefinitions.register(new JSONArray().put(row(MILITARY, MILITARY_SECTION, MILITARY_PREFIXES, MILITARY_SUFFIXES)));
        return ItemDefinitions.get(MILITARY);
    }
}
