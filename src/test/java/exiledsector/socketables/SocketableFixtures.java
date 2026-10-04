package exiledsector.socketables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class SocketableFixtures {

    static final String MILITARY = "military_subroutine";
    static final String MILITARY_POOL = "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6;"
            + " BEAM_WEAPON_DAMAGE_PERCENT:10:15; ENERGY_WEAPON_FLUX_COST_PERCENT:-15:-10";

    private SocketableFixtures() {
    }

    static JSONObject row(String id, String kind, String pool) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("kind", kind)
                .put("name", "Military-grade Domain Subroutine")
                .put("icon", "graphics/icons/cargo/chip1.png")
                .put("grade", "military")
                .put("alignment", "high_tech")
                .put("rarity", "20")
                .put("pool", pool);
    }

    static SocketableDefinition registerMilitary() throws JSONException {
        SocketableDefinitions.register(new JSONArray().put(row(MILITARY, "subroutine", MILITARY_POOL)));
        return SocketableDefinitions.get(MILITARY);
    }
}
