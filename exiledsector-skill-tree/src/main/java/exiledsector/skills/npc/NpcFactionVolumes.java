package exiledsector.skills.npc;

import exiledsector.ModCsv;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NpcFactionVolumes {

    static final String DATA_PATH = "data/config/exiledSector/npc_faction_volumes.csv";
    private static final String FACTION_COLUMN = "faction";
    private static final String VOLUME_COLUMN = "volume";
    private static final Logger LOG = Logger.getLogger(NpcFactionVolumes.class);
    private static final AtomicReference<Map<String, String>> VOLUME_BY_FACTION = new AtomicReference<>(Map.of());

    private NpcFactionVolumes() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, DATA_PATH, LOG, NpcFactionVolumes::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String faction = ModCsv.text(row, FACTION_COLUMN);
            String volume = ModCsv.text(row, VOLUME_COLUMN);
            if (faction.isEmpty()) {
                return;
            }
            if (!SkillTags.FACTION_VOLUMES.contains(volume)) {
                LOG.warn("Ignoring faction " + faction + " in " + DATA_PATH + ": '" + volume
                        + "' is not a vanilla faction volume " + SkillTags.FACTION_VOLUMES);
                return;
            }
            loaded.put(faction, volume);
        });
        VOLUME_BY_FACTION.set(Map.copyOf(loaded));
    }

    public static String volumeFor(String factionId) {
        return factionId == null ? null : VOLUME_BY_FACTION.get().get(factionId);
    }

    public static List<String> factionsIn(String volume) {
        return VOLUME_BY_FACTION.get().entrySet().stream().filter(entry -> entry.getValue().equals(volume)).map(Map.Entry::getKey)
                .sorted().toList();
    }

    public static void clear() {
        VOLUME_BY_FACTION.set(Map.of());
    }
}
