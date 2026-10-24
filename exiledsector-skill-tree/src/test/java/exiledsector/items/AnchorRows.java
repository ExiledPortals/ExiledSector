package exiledsector.items;

import org.json.JSONException;
import org.json.JSONObject;

public final class AnchorRows {

    private AnchorRows() {
    }

    public static JSONObject row(String id, String volume, String exitNode) throws JSONException {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "Opens a wormhole.");
    }
}
