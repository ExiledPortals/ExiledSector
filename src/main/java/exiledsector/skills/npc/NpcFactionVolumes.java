package exiledsector.skills.npc;

import com.fs.starfarer.api.Global;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class NpcFactionVolumes {

    static final String DATA_PATH = "data/config/exiledSector/npc_faction_volumes.csv";
    private static final String FACTION_COLUMN = "faction";
    private static final String REGION_COLUMN = "region";
    private static final Logger LOG = Logger.getLogger(NpcFactionVolumes.class);
    private static final AtomicReference<Map<String, String>> REGION_BY_FACTION = new AtomicReference<>(Map.of());

    private NpcFactionVolumes() {
    }

    public static void load() {
        try {
            register(Global.getSettings().getMergedSpreadsheetDataForMod(FACTION_COLUMN, DATA_PATH, MOD_ID));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + DATA_PATH, e);
        }
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String faction = row.optString(FACTION_COLUMN, "").trim();
            String region = row.optString(REGION_COLUMN, "").trim();
            if (faction.isEmpty()) {
                continue;
            }
            if (!SkillTags.FACTION_VOLUMES.contains(region)) {
                LOG.warn("Ignoring faction " + faction + " in " + DATA_PATH + ": '" + region
                        + "' is not a vanilla faction volume " + SkillTags.FACTION_VOLUMES);
                continue;
            }
            loaded.put(faction, region);
        }
        REGION_BY_FACTION.set(Map.copyOf(loaded));
    }

    public static String regionFor(String factionId) {
        return factionId == null ? null : REGION_BY_FACTION.get().get(factionId);
    }

    public static void clear() {
        REGION_BY_FACTION.set(Map.of());
    }
}
