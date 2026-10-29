package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import exiledsector.charts.table.BuiltInTable;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.ModCsv;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ChartBosses {

    static final String PATH = "data/config/exiledSector/chart_bosses.csv";
    static final String STATIONS_PATH = "data/config/exiledSector/chart_boss_stations.csv";
    static final String VARIANT_COLUMN = "variant";
    static final Definition BUILT_IN_FLEET = new Definition(ChartBossType.FLEET, 1f, ChartTiers.LOWEST_TIER, ChartTiers.CORRUPTED_TIER, 1f);
    private static final Logger LOG = Logger.getLogger(ChartBosses.class);
    private static volatile Map<ChartBossType, Definition> definitions = Map.of(ChartBossType.FLEET, BUILT_IN_FLEET);
    private static volatile List<Station> stations = List.of();

    public record Definition(ChartBossType type, float weight, int minTier, int maxTier, float fleetShare, Set<String> excludedFactionIds) {

        public Definition {
            weight = Math.max(0f, weight);
            fleetShare = Math.max(0f, fleetShare);
            excludedFactionIds = excludedFactionIds == null ? Set.of() : Set.copyOf(excludedFactionIds);
        }

        public Definition(ChartBossType type, float weight, int minTier, int maxTier, float fleetShare) {
            this(type, weight, minTier, maxTier, fleetShare, Set.of());
        }

        public boolean allowedAt(int tier) {
            return weight > 0f && tier >= minTier && tier <= maxTier;
        }

        public boolean excludes(String factionId) {
            return factionId != null && excludedFactionIds.contains(factionId);
        }
    }

    public record Station(String variantId, Set<String> factionIds, int minTier, int maxTier, float weight) {

        public Station {
            factionIds = Set.copyOf(factionIds);
            weight = Math.max(0f, weight);
        }

        public boolean allowedAt(int tier) {
            return weight > 0f && tier >= minTier && tier <= maxTier;
        }
    }

    private ChartBosses() {
    }

    public static void load() {
        ModCsv.load(BuiltInTable.ID_COLUMN, PATH, LOG, ChartBosses::register);
        ModCsv.load(VARIANT_COLUMN, STATIONS_PATH, LOG, ChartBosses::registerStations);
        keepLoadedStations();
    }

    private static void keepLoadedStations() {
        List<Station> loaded = new ArrayList<>();
        for (Station station : stations) {
            if (Global.getSettings().doesVariantExist(station.variantId())) {
                loaded.add(station);
            } else {
                LOG.info("[ExiledSector] " + STATIONS_PATH + " skips " + station.variantId() + ", a variant the game has not loaded");
            }
        }
        stations = List.copyOf(loaded);
    }

    static List<String> register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        Map<String, Definition> loaded = BuiltInTable.rows(rows, csv, "boss type", id -> ChartBossType.byKey(id) != null, ChartBosses::parse);
        csv.report(LOG);
        Map<ChartBossType, Definition> byType = new EnumMap<>(ChartBossType.class);
        loaded.values().forEach(definition -> byType.put(definition.type(), definition));
        if (byType.isEmpty()) {
            byType.put(ChartBossType.FLEET, BUILT_IN_FLEET);
        }
        definitions = Collections.unmodifiableMap(byType);
        return csv.problems();
    }

    private static Definition parse(CsvRows csv, JSONObject row, String id) {
        BuiltInTable.TierRange tiers = BuiltInTable.TierRange.parse(csv, row, id);
        return new Definition(ChartBossType.byKey(id), csv.number(row, "weight", 1f, id), tiers.minTier(), tiers.maxTier(),
                csv.number(row, "fleet_share", 1f, id), new LinkedHashSet<>(CsvRows.entries(ModCsv.text(row, "excluded_factions"))));
    }

    static List<String> registerStations(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(STATIONS_PATH);
        Map<String, Station> loaded = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, VARIANT_COLUMN, (variantId, row) -> {
            BuiltInTable.TierRange tiers = BuiltInTable.TierRange.parse(csv, row, variantId);
            Set<String> factionIds = new LinkedHashSet<>(CsvRows.entries(ModCsv.text(row, "factions")));
            loaded.put(variantId, new Station(variantId, factionIds, tiers.minTier(), tiers.maxTier(), csv.number(row, "weight", 1f, variantId)));
        });
        csv.report(LOG);
        stations = List.copyOf(loaded.values());
        return csv.problems();
    }

    public static Definition definition(ChartBossType type) {
        Definition definition = type == null ? null : definitions.get(type);
        return definition != null ? definition : type == ChartBossType.FLEET ? BUILT_IN_FLEET : null;
    }

    static ChartBossType pickType(int tier, String factionId, Random random) {
        List<Definition> candidates = new ArrayList<>();
        for (ChartBossType type : ChartBossType.values()) {
            Definition definition = definitions.get(type);
            if (definition != null && definition.allowedAt(tier) && !definition.excludes(factionId)
                    && (definition.type() != ChartBossType.STATION || !stationsFor(factionId, tier).isEmpty())) {
                candidates.add(definition);
            }
        }
        Definition picked = candidates.isEmpty() ? null : WeightedPick.pick(candidates, Definition::weight, random);
        return picked == null ? ChartBossType.FLEET : picked.type();
    }

    static List<Station> stationsFor(String factionId, int tier) {
        Definition stationBoss = definitions.get(ChartBossType.STATION);
        if (stationBoss != null && stationBoss.excludes(factionId)) {
            return List.of();
        }
        List<Station> own = new ArrayList<>();
        List<Station> shared = new ArrayList<>();
        for (Station station : stations) {
            if (!station.allowedAt(tier)) {
                continue;
            }
            if (station.factionIds().isEmpty()) {
                shared.add(station);
            } else if (station.factionIds().contains(factionId)) {
                own.add(station);
            }
        }
        return own.isEmpty() && !ChartThreatFleets.builds(factionId) ? shared : own;
    }

    static String pickStation(String factionId, int tier, Random random) {
        Station picked = WeightedPick.pick(stationsFor(factionId, tier), Station::weight, random);
        return picked == null ? null : picked.variantId();
    }

    static void useForTests(List<Definition> testDefinitions, List<Station> testStations) {
        Map<ChartBossType, Definition> byType = new EnumMap<>(ChartBossType.class);
        testDefinitions.forEach(definition -> byType.put(definition.type(), definition));
        definitions = Collections.unmodifiableMap(byType);
        stations = List.copyOf(testStations);
    }

    static void resetForTests() {
        definitions = Map.of(ChartBossType.FLEET, BUILT_IN_FLEET);
        stations = List.of();
    }
}
