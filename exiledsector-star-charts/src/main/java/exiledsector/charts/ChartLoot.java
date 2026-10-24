package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.procgen.DropGroupRow;
import exiledsector.data.CsvRows;
import exiledlib.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ChartLoot {

    static final String PATH = "data/config/exiledSector/chart_loot.csv";
    static final String ID_COLUMN = "id";
    static final String FLEET_SCOPE = "fleet";
    static final String BOSS_SCOPE = "boss";
    static final List<String> SCOPES = scopes();
    static final Set<String> COLONY_ITEM_GROUPS = Set.of("rare_tech", "rare_tech_low", "rare_tech_mothership", "nanoforge_always");
    static final List<String> VANILLA_TYPE_NAMES = Arrays.stream(DropType.values()).filter(DropType::isVanilla)
            .map(type -> type.name().toLowerCase(Locale.ROOT)).toList();
    private static final Logger LOG = Logger.getLogger(ChartLoot.class);
    private static volatile Table table = Table.EMPTY;

    public enum Mode {
        RANDOM,
        VALUE;

        static Mode parse(String text) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(text)) {
                    return mode;
                }
            }
            return null;
        }
    }

    record Entry(DropType type, String scope, String group, Mode mode, float amount, int minTier) {
    }

    record Table(Map<String, DropType> groupTypes, List<Entry> entries) {

        static final Table EMPTY = new Table(Map.of(), List.of());

        Table {
            groupTypes = Map.copyOf(groupTypes);
            entries = List.copyOf(entries);
        }
    }

    private ChartLoot() {
    }

    private static List<String> scopes() {
        List<String> scopes = new ArrayList<>(ChartPois.KIND_NAMES);
        scopes.add(FLEET_SCOPE);
        scopes.add(BOSS_SCOPE);
        return List.copyOf(scopes);
    }

    public static void load() {
        Set<String> knownGroups = vanillaGroups();
        ModCsv.load(ID_COLUMN, PATH, LOG, rows -> register(rows, knownGroups::contains));
    }

    static Set<String> vanillaGroups() {
        Set<String> groups = new HashSet<>();
        for (DropGroupRow row : Global.getSettings().getAllSpecs(DropGroupRow.class)) {
            groups.add(row.getGroup());
        }
        return groups;
    }

    static List<String> register(JSONArray rows, Predicate<String> groupExists) throws JSONException {
        Map<String, DropType> groupTypes = new LinkedHashMap<>();
        List<Entry> entries = new ArrayList<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (!id.isEmpty()) {
                    parse(csv, row, id, groupExists, groupTypes, entries);
                }
            });
        }
        csv.report(LOG);
        table = new Table(groupTypes, entries);
        return csv.problems();
    }

    private static void parse(CsvRows csv, JSONObject row, String id, Predicate<String> groupExists, Map<String, DropType> groupTypes,
                              List<Entry> entries) {
        DropType type = vanillaType(ModCsv.text(row, "drop_type"));
        String group = ModCsv.text(row, "group");
        String scope = ModCsv.text(row, "scope").toLowerCase(Locale.ROOT);
        if (type == null) {
            csv.skipRow(id, "drop_type must be one of " + VANILLA_TYPE_NAMES);
            return;
        }
        if (group.isEmpty() || !groupExists.test(group)) {
            csv.skipRow(id, "group '" + group + "' is not a loaded drop group");
            return;
        }
        if (COLONY_ITEM_GROUPS.contains(group)) {
            csv.skipRow(id, "group " + group + " holds colony items, which only come from the run's single capped roll");
            return;
        }
        if (scope.isEmpty()) {
            DropType mapped = groupTypes.putIfAbsent(group, type);
            if (mapped != null && mapped != type) {
                csv.skipRow(id, "group " + group + " already belongs to " + mapped.name().toLowerCase(Locale.ROOT));
            }
            return;
        }
        if (!SCOPES.contains(scope)) {
            csv.skipRow(id, "scope must be empty or one of " + SCOPES);
            return;
        }
        Mode mode = Mode.parse(ModCsv.text(row, "mode"));
        if (mode == null) {
            csv.skipRow(id, "mode must be random or value");
            return;
        }
        float amount = csv.number(row, "amount", 0f, id);
        if (!(amount > 0f)) {
            csv.skipRow(id, "amount must be above 0");
            return;
        }
        entries.add(new Entry(type, scope, group, mode, amount, ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id))));
    }

    private static DropType vanillaType(String text) {
        for (DropType type : DropType.values()) {
            if (type.isVanilla() && type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }

    public static void clear() {
        table = Table.EMPTY;
    }

    static DropType typeOfGroup(String group) {
        return group == null ? null : table.groupTypes().get(group);
    }

    static List<Entry> entriesFor(String scope, int tier) {
        List<Entry> matching = new ArrayList<>();
        for (Entry entry : table.entries()) {
            if (entry.scope().equals(scope) && entry.minTier() <= tier) {
                matching.add(entry);
            }
        }
        return matching;
    }

    static Map<String, DropType> groupTypes() {
        return table.groupTypes();
    }
}
