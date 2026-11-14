package exiledsector.charts.atlas;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.Set;

public final class TerritoryExclusions {

    static final String PATH = "data/config/exiledSector/atlas_territoryless_factions.csv";
    static final String OLD_PATH = "data/config/exiledSector/chart_skirmish_factions.csv";
    static final String FACTION_COLUMN = "faction";
    private static final Logger LOG = Logger.getLogger(TerritoryExclusions.class);
    private static volatile Set<String> excluded = Set.of();

    private TerritoryExclusions() {
    }

    public static void load() {
        Set<String> loaded = new LinkedHashSet<>();
        ModCsv.load(FACTION_COLUMN, PATH, LOG, rows -> loaded.addAll(factionIds(rows)));
        ModCsv.load(FACTION_COLUMN, OLD_PATH, LOG, rows -> loaded.addAll(factionIds(rows)));
        excluded = Set.copyOf(loaded);
        LOG.info("[ExiledSector] " + excluded.size() + " faction(s) never hold Atlas territory: " + excluded);
    }

    static Set<String> factionIds(JSONArray rows) throws JSONException {
        Set<String> factionIds = new LinkedHashSet<>();
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String factionId = ModCsv.text(row, FACTION_COLUMN);
                if (!factionId.isEmpty()) {
                    factionIds.add(factionId);
                }
            });
        }
        return factionIds;
    }

    public static boolean excludes(String factionId) {
        return factionId == null || factionId.isEmpty() || excluded.contains(factionId);
    }

    public static void useForTests(Set<String> factionIds) {
        excluded = Set.copyOf(factionIds);
    }
}
