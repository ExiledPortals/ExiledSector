package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
import exiledsector.compat.CompatTarget;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.anomaly.skirmish.SkirmishFactions;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class ModdedChartFactions {

    static final String CONFIG_FOLDER = "data/config/exerelinFactionConfig/";
    static final String MOD_FACTIONS_PATH = CONFIG_FOLDER + "mod_factions.csv";
    static final String FACTION_COLUMN = "faction";
    static final String PLAYABLE_KEY = "playableFaction";
    static final Set<String> OWN_FACTIONS = Set.of(SkirmishFactions.ALLY_FACTION_ID, DormantFleets.FACTION_ID);
    private static final Logger LOG = Logger.getLogger(ModdedChartFactions.class);

    private ModdedChartFactions() {
    }

    public static List<String> detect(Set<String> listedFactions) {
        try {
            JSONArray rows = Global.getSettings().getMergedSpreadsheetData(FACTION_COLUMN, MOD_FACTIONS_PATH);
            Set<String> found = new TreeSet<>();
            ModCsv.forEach(rows, (index, row) -> {
                String factionId = ModCsv.text(row, FACTION_COLUMN);
                if (!factionId.isEmpty() && !listedFactions.contains(factionId) && !OWN_FACTIONS.contains(factionId) && isPlayable(factionId)) {
                    found.add(factionId);
                }
            });
            LOG.info("[ExiledSector] The atlas found " + found.size() + " modded faction(s): " + String.join(", ", found));
            return List.copyOf(found);
        } catch (IOException | JSONException | RuntimeException e) {
            return List.of();
        }
    }

    static boolean isPlayable(String factionId) {
        if (!CompatTarget.specExists(() -> Global.getSettings().getFactionSpec(factionId))) {
            return false;
        }
        try {
            return Global.getSettings().getMergedJSON(CONFIG_FOLDER + factionId + ".json").optBoolean(PLAYABLE_KEY, true);
        } catch (IOException | JSONException | RuntimeException e) {
            return false;
        }
    }
}
