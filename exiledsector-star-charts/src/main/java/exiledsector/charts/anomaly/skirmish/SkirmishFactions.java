package exiledsector.charts.anomaly.skirmish;

import exiledlib.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SkirmishFactions {

    static final String PATH = "data/config/exiledSector/chart_skirmish_factions.csv";
    static final String FACTION_COLUMN = "faction";
    private static final Logger LOG = Logger.getLogger(SkirmishFactions.class);
    private static volatile Set<String> neutral = Set.of();

    private SkirmishFactions() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, PATH, LOG, SkirmishFactions::register);
    }

    static void register(JSONArray rows) throws JSONException {
        Set<String> loaded = new LinkedHashSet<>();
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String factionId = ModCsv.text(row, FACTION_COLUMN);
                if (!factionId.isEmpty()) {
                    loaded.add(factionId);
                }
            });
        }
        neutral = Set.copyOf(loaded);
        LOG.info("[ExiledSector] Faction Skirmishes leave out " + neutral.size() + " faction(s): " + neutral);
    }

    public static boolean takesSides(String factionId) {
        return factionId != null && !factionId.isEmpty() && !neutral.contains(factionId);
    }

    static void useForTests(Set<String> factionIds) {
        neutral = Set.copyOf(factionIds);
    }
}
