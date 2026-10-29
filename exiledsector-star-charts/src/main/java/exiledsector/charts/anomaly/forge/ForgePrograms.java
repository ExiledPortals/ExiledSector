package exiledsector.charts.anomaly.forge;

import exiledsector.ModCsv;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.items.ForgeOperation;
import exiledsector.items.ItemType;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ForgePrograms {

    public static final String PATH = "data/config/exiledSector/chart_forge_programs.csv";
    static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(ForgePrograms.class);
    private static volatile Map<String, Program> programs = Map.of();

    public record Program(String id, ForgeOperation operation, float weight, int minTier, Set<ItemType> itemTypes, int metals, int rareMetals,
                          int feedstock) {

        public Program {
            itemTypes = itemTypes.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(itemTypes));
        }

        public boolean takes(ItemType type) {
            return itemTypes.contains(type);
        }
    }

    private ForgePrograms() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ForgePrograms::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        Map<String, Program> loaded = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            ForgeOperation operation = ForgeOperation.byIdOrNull(id);
            if (operation == null) {
                csv.skipRow(id, "'" + id + "' is not a forge program");
                return;
            }
            Set<ItemType> itemTypes = EnumSet.noneOf(ItemType.class);
            for (String typeId : CsvRows.entries(ModCsv.text(row, "item_types"))) {
                ItemType type = ItemType.byIdOrNull(typeId);
                if (type == null || type == ItemType.WORMHOLE_ANCHOR) {
                    csv.problem(id, "item_types", typeId);
                } else {
                    itemTypes.add(type);
                }
            }
            if (itemTypes.isEmpty()) {
                csv.skipRow(id, "no item type it can forge");
                return;
            }
            int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
            loaded.put(id, new Program(id, operation, Math.max(0f, csv.number(row, "weight", 1f, id)), minTier, itemTypes,
                    Math.max(0, csv.integer(row, "metals", 0, id)), Math.max(0, csv.integer(row, "rare_metals", 0, id)),
                    Math.max(0, csv.integer(row, "feedstock", 0, id))));
        });
        csv.report(LOG);
        programs = Collections.unmodifiableMap(loaded);
        LOG.info("[ExiledSector] The Derelict Forge knows " + loaded.size() + " program(s)");
        return csv.problems();
    }

    public static Collection<Program> all() {
        return programs.values();
    }

    public static Program byId(String id) {
        return id == null ? null : programs.get(id);
    }

    public static List<Program> roll(int tier, int count, Random random) {
        List<Program> candidates = new ArrayList<>(programs.values().stream()
                .filter(program -> program.weight() > 0f && tier >= program.minTier()).toList());
        List<Program> rolled = new ArrayList<>();
        while (rolled.size() < count && !candidates.isEmpty()) {
            Program picked = WeightedPick.pick(candidates, Program::weight, random);
            rolled.add(picked);
            candidates.remove(picked);
        }
        return rolled;
    }

    static void resetForTests() {
        programs = Map.of();
    }
}
