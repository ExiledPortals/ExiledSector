package exiledsector.charts.anomaly.forge;

import exiledsector.ModCsv;
import exiledsector.charts.table.BuiltInTable;
import exiledsector.data.CsvRows;
import exiledsector.items.ItemType;
import exiledsector.items.crafting.ForgeOperation;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ForgePrograms {

    public static final String PATH = "data/config/exiledSector/chart_forge_programs.csv";
    static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(ForgePrograms.class);
    private static volatile Map<String, Program> programs = Map.of();

    public record Program(String id, ForgeOperation operation, Set<ItemType> itemTypes, int feedstock, int infusedFeedstockEach) {

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
        Map<String, Program> loaded = BuiltInTable.rows(rows, csv, "forge program", id -> ForgeOperation.byIdOrNull(id) != null, ForgePrograms::parse);
        csv.report(LOG);
        programs = Collections.unmodifiableMap(loaded);
        LOG.info("[ExiledSector] The Derelict Forge knows " + loaded.size() + " program(s)");
        return csv.problems();
    }

    private static Program parse(CsvRows csv, JSONObject row, String id) {
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
            return null;
        }
        return new Program(id, ForgeOperation.byIdOrNull(id), itemTypes, Math.max(0, csv.integer(row, "feedstock", 0, id)),
                Math.max(0, csv.integer(row, "infused_feedstock_each", 0, id)));
    }

    public static Collection<Program> all() {
        return programs.values();
    }

    public static Program byId(String id) {
        return id == null ? null : programs.get(id);
    }

    public static Program of(ForgeOperation operation) {
        return operation == null ? null : byId(operation.id());
    }

    static void resetForTests() {
        programs = Map.of();
    }
}
