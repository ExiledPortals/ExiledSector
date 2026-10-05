package exiledsector.socketables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class SocketableFixtures {

    static final String MILITARY = "military_subroutine";
    static final String MILITARY_PREFIXES = "FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6; BEAM_WEAPON_DAMAGE_PERCENT:10:15";
    static final String MILITARY_SUFFIXES = "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; ARMOR_PERCENT:6:9; HULL_MULT:4:6";

    private SocketableFixtures() {
    }

    static JSONObject row(String id, String kind, String prefixes) throws JSONException {
        return row(id, kind, prefixes, "");
    }

    static JSONObject row(String id, String kind, String prefixes, String suffixes) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("kind", kind)
                .put("name", "Military-grade Domain Subroutine")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("grade", "military")
                .put("alignment", "high_tech")
                .put("rarity", "20")
                .put("prefixes", prefixes)
                .put("suffixes", suffixes);
    }

    static SocketableDefinition registerMilitary() throws JSONException {
        SocketableDefinitions.register(new JSONArray().put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES)));
        return SocketableDefinitions.get(MILITARY);
    }
}
