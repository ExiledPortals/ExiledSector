package exiledsector.charts;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ChartPois {

    static final String PATH = "data/config/exiledSector/chart_poi.csv";
    static final String ID_COLUMN = "id";
    static final Set<String> DERELICT_TYPES = Set.of("CIVILIAN", "SMALL", "MEDIUM", "LARGE");
    static final List<String> KIND_NAMES = Arrays.stream(ChartPoi.Kind.values()).map(kind -> kind.name().toLowerCase(Locale.ROOT)).toList();
    private static final Logger LOG = Logger.getLogger(ChartPois.class);
    private static volatile List<ChartPoi> pois = List.of();

    private ChartPois() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartPois::register);
    }

    static void register(JSONArray rows) throws JSONException {
        Map<String, ChartPoi> loaded = new LinkedHashMap<>();
        ChartCsv csv = new ChartCsv(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (id.isEmpty()) {
                    return;
                }
                ChartPoi poi = parse(csv, row, id);
                if (poi != null) {
                    loaded.put(id, poi);
                }
            });
        }
        csv.report(LOG);
        pois = List.copyOf(loaded.values());
        LOG.info("[ExiledSector] Star Chart systems can place " + pois.size() + " kinds of point of interest");
    }

    private static ChartPoi parse(ChartCsv csv, JSONObject row, String id) {
        ChartPoi.Kind kind = ChartPoi.Kind.parse(ModCsv.text(row, "kind"));
        String entityType = ModCsv.text(row, "entity");
        if (kind == null) {
            csv.skipRow(id, "kind must be one of " + KIND_NAMES);
            return null;
        }
        if (entityType.isEmpty()) {
            csv.skipRow(id, "no entity type");
            return null;
        }
        String derelictType = ModCsv.text(row, "derelict_type").toUpperCase(Locale.ROOT);
        if (kind == ChartPoi.Kind.DERELICT && !DERELICT_TYPES.contains(derelictType)) {
            csv.skipRow(id, "a derelict needs derelict_type " + DERELICT_TYPES);
            return null;
        }
        int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
        int maxTier = ChartTiers.clamp(csv.integer(row, "max_tier", ChartTiers.HIGHEST_TIER, id));
        return new ChartPoi(id, kind, entityType, ModCsv.text(row, "salvage_spec"), derelictType, csv.number(row, "cost", 1f, id),
                csv.number(row, "weight", 1f, id), Math.min(minTier, maxTier), Math.max(minTier, maxTier), csv.flag(row, "guarded", false, id));
    }

    public static List<ChartPoi> all() {
        return pois;
    }

    static List<ChartPoi> lootAt(List<ChartPoi> candidates, int tier) {
        List<ChartPoi> eligible = new ArrayList<>();
        for (ChartPoi poi : candidates) {
            if (poi.allowedAt(tier) && poi.cost() > 0f && poi.weight() > 0f) {
                eligible.add(poi);
            }
        }
        return eligible;
    }

    static List<ChartPoi> ofKind(List<ChartPoi> candidates, ChartPoi.Kind kind) {
        List<ChartPoi> members = new ArrayList<>();
        for (ChartPoi poi : candidates) {
            if (poi.kind() == kind) {
                members.add(poi);
            }
        }
        return members;
    }

    static void resetForTests() {
        pois = List.of();
    }
}
