package exiledsector.items;

import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

final class ItemFixtures {

    static final String MILITARY = "military_bridge";
    static final String MILITARY_SECTION = "bridge";
    static final String MILITARY_PREFIXES = "FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6; BEAM_WEAPON_DAMAGE_PERCENT:10:15";
    static final String MILITARY_SUFFIXES = "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; ARMOR_PERCENT:6:9; HULL_MULT:4:6";
    static final String SUBROUTINE = "test_subroutine";

    private ItemFixtures() {
    }

    static ItemData data(String definitionId, long seed, List<RolledEffect> effects, FrozenName frozenName) {
        return new ItemData(definitionId, seed, RolledModifier.fromLegacyEffects(ItemDefinitions.get(definitionId), effects), frozenName);
    }

    static JSONObject row(String id, String kind, String prefixes) throws JSONException {
        return row(id, kind, prefixes, "");
    }

    static JSONObject row(String id, String kind, String prefixes, String suffixes) throws JSONException {
        return withKind(new JSONObject()
                .put("id", id)
                .put("name", "Military-grade Bridge")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20")
                .put("prefixes", prefixes)
                .put("suffixes", suffixes), kind);
    }

    static JSONObject withKind(JSONObject row, String kind) throws JSONException {
        return ItemKind.SUBROUTINE.id().equals(kind) ? row.put(ItemDefinition.SUBROUTINE_KIND_COLUMN, SubroutineKind.VOLUME.id()) : row.put("section", kind);
    }

    static JSONObject subroutineRow(String id) throws JSONException {
        return subroutineRow(id, SubroutineKind.VOLUME);
    }

    static JSONObject subroutineRow(String id, SubroutineKind subroutineKind) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", "Test Subroutine")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20")
                .put(ItemDefinition.SUBROUTINE_KIND_COLUMN, subroutineKind.id());
    }

    static final String EVERY_ITEM_TYPE = "hull_item";

    static JSONObject modifierRow(String id, String slot, String lines, String name) throws JSONException {
        return modifierRow(id, slot, EVERY_ITEM_TYPE, lines, name);
    }

    static JSONObject modifierRow(String id, String slot, String itemTypes, String lines, String name) throws JSONException {
        return new JSONObject().put("id", id).put("slot", slot).put("item_types", itemTypes).put("lines", lines).put("name", name);
    }

    static JSONArray shippedRows(String dataPath) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    static void registerShippedItems() throws Exception {
        ItemModifiers.register(shippedRows(ItemModifiers.DATA_PATH));
        ItemDefinitions.register(shippedRows(ItemDefinitions.SUBROUTINES_PATH), shippedRows(ItemDefinitions.HULL_ITEMS_PATH));
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

    static ItemDefinition registerMilitaryAndSubroutine() throws JSONException {
        ItemDefinitions.register(new JSONArray().put(row(MILITARY, MILITARY_SECTION, MILITARY_PREFIXES, MILITARY_SUFFIXES)).put(subroutineRow(SUBROUTINE)));
        return ItemDefinitions.get(SUBROUTINE);
    }
}
