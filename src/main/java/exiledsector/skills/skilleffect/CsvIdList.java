package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class CsvIdList {

    private static final List<CsvIdList> ALL = new ArrayList<>();

    public static final CsvIdList SPLIT_BEAM_EFFECTS =
            register("data/config/exiledSector/split_beam_effect_blocklist.csv", "plugin");
    public static final CsvIdList ENERGY_CHAIN_WEAPONS =
            register("data/config/exiledSector/energy_chain_blocklist.csv", "weapon");
    public static final CsvIdList BALLISTIC_PIERCE_WEAPONS =
            register("data/config/exiledSector/ballistic_pierce_blocklist.csv", "weapon");
    public static final CsvIdList DRONE_MARKER_HULLMODS =
            register("data/config/exiledSector/drone_marker_hullmods.csv", "hullmod");

    private final String path;
    private final String idColumn;
    private final AtomicReference<Set<String>> ids = new AtomicReference<>(Set.of());

    private CsvIdList(String path, String idColumn) {
        this.path = path;
        this.idColumn = idColumn;
    }

    private static CsvIdList register(String path, String idColumn) {
        CsvIdList blocklist = new CsvIdList(path, idColumn);
        ALL.add(blocklist);
        return blocklist;
    }

    public static void loadAll() {
        for (CsvIdList blocklist : ALL) {
            blocklist.load();
        }
    }

    private void load() {
        try {
            JSONArray rows = Global.getSettings().getMergedSpreadsheetDataForMod(idColumn, path, MOD_ID);
            Set<String> loaded = new HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                String id = rows.getJSONObject(i).optString(idColumn, "").trim();
                if (!id.isEmpty()) {
                    loaded.add(id);
                }
            }
            ids.set(Set.copyOf(loaded));
        } catch (IOException | JSONException e) {
            Logger.getLogger(CsvIdList.class).error("Failed to load " + path, e);
        }
    }

    public boolean contains(String id) {
        return id != null && ids.get().contains(id);
    }
}
