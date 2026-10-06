package exiledsector.skills.npc;

import exiledsector.ModCsv;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NpcFactionVolumes {

    static final String DATA_PATH = "data/config/exiledSector/npc_faction_volumes.csv";
    private static final String FACTION_COLUMN = "faction";
    private static final String REGION_COLUMN = "region";
    private static final Logger LOG = Logger.getLogger(NpcFactionVolumes.class);
    private static final AtomicReference<Map<String, String>> REGION_BY_FACTION = new AtomicReference<>(Map.of());

    private NpcFactionVolumes() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, DATA_PATH, LOG, NpcFactionVolumes::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String faction = ModCsv.text(row, FACTION_COLUMN);
            String region = ModCsv.text(row, REGION_COLUMN);
            if (faction.isEmpty()) {
                return;
            }
            if (!SkillTags.FACTION_VOLUMES.contains(region)) {
                LOG.warn("Ignoring faction " + faction + " in " + DATA_PATH + ": '" + region
                        + "' is not a vanilla faction volume " + SkillTags.FACTION_VOLUMES);
                return;
            }
            loaded.put(faction, region);
        });
        REGION_BY_FACTION.set(Map.copyOf(loaded));
    }

    public static String regionFor(String factionId) {
        return factionId == null ? null : REGION_BY_FACTION.get().get(factionId);
    }

    public static void clear() {
        REGION_BY_FACTION.set(Map.of());
    }
}
