package exiledsector.items;

import exiledlib.ProjectFiles;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

final class HullItemRows {

    static final String MILITARY = "military_bridge";
    static final String MILITARY_SECTION = "bridge";
    static final String MILITARY_PREFIXES = "FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6; BEAM_WEAPON_DAMAGE_PERCENT:10:15";
    static final String MILITARY_SUFFIXES = "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; ARMOR_PERCENT:6:9; HULL_MULT:4:6";
    static final String EVERY_ITEM_TYPE = "hull_item";

    private HullItemRows() {
    }

    static ItemData data(String definitionId, long seed, List<RolledEffect> effects, FrozenName frozenName) {
        return new ItemData(definitionId, seed, RolledModifier.fromLegacyEffects(ItemDefinitions.get(definitionId), effects), frozenName);
    }

    static JSONObject row(String id, String section, String prefixes) throws JSONException {
        return row(id, section, prefixes, "");
    }

    static JSONObject row(String id, String section, String prefixes, String suffixes) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", "Military-grade Bridge")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20")
                .put("prefixes", prefixes)
                .put("suffixes", suffixes)
                .put("section", section);
    }

    static JSONObject modifierRow(String id, String slot, String lines, String name) throws JSONException {
        return modifierRow(id, slot, EVERY_ITEM_TYPE, lines, name);
    }

    static JSONObject modifierRow(String id, String slot, String itemTypes, String lines, String name) throws JSONException {
        return new JSONObject().put("id", id).put("slot", slot).put("item_types", itemTypes).put("lines", lines).put("name", name);
    }

    static JSONArray shippedRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    static void registerModifierNames(String... idSlotLinesNames) throws JSONException {
        JSONArray rows = new JSONArray();
        for (String row : idSlotLinesNames) {
            String[] fields = row.split(",", -1);
            rows.put(modifierRow(fields[0], fields[1], fields[2], fields[3]));
        }
        ItemModifiers.register(rows);
    }

    static ItemDefinition registerMilitary() throws JSONException {
        ItemDefinitions.register(new JSONArray().put(row(MILITARY, MILITARY_SECTION, MILITARY_PREFIXES, MILITARY_SUFFIXES)));
        return ItemDefinitions.get(MILITARY);
    }
}
