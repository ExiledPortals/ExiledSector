package exiledsector.skills;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HullModConflicts {

    public static final String DATA_PATH = ExiledSector.configPath("hullmod_conflicts.csv");
    public static final String HULL_MOD_COLUMN = "hullmod";
    public static final String CONFLICTS_COLUMN = "conflicts";
    public static final HullModConflicts NONE = new HullModConflicts(Map.of());
    private static final Logger LOG = Logger.getLogger(HullModConflicts.class);

    private final Map<String, Set<String>> partnersByHullMod;

    private HullModConflicts(Map<String, Set<String>> partnersByHullMod) {
        this.partnersByHullMod = partnersByHullMod;
    }

    public static HullModConflicts load() {
        try {
            return fromRows(ModCsv.rows(HULL_MOD_COLUMN, DATA_PATH));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + DATA_PATH, e);
            return NONE;
        }
    }

    public static HullModConflicts fromRows(JSONArray rows) throws JSONException {
        Map<String, Set<String>> partners = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, HULL_MOD_COLUMN, (hullModId, row) -> {
            for (String conflictingId : CsvRows.entries(ModCsv.text(row, CONFLICTS_COLUMN))) {
                if (!conflictingId.equals(hullModId)) {
                    partners.computeIfAbsent(hullModId, id -> new LinkedHashSet<>()).add(conflictingId);
                    partners.computeIfAbsent(conflictingId, id -> new LinkedHashSet<>()).add(hullModId);
                }
            }
        });
        Map<String, Set<String>> frozen = new LinkedHashMap<>();
        partners.forEach((hullModId, ids) -> frozen.put(hullModId, Collections.unmodifiableSet(ids)));
        return new HullModConflicts(Collections.unmodifiableMap(frozen));
    }

    public Set<String> conflictsOf(String hullModId) {
        return partnersByHullMod.getOrDefault(hullModId, Set.of());
    }

    public List<String> conflictsOfAny(Collection<String> hullModIds) {
        Set<String> conflicts = new LinkedHashSet<>();
        for (String hullModId : hullModIds) {
            conflicts.addAll(conflictsOf(hullModId));
        }
        conflicts.removeAll(hullModIds);
        return List.copyOf(conflicts);
    }
}
