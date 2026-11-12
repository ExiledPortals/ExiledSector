package exiledsector.charts.officer.reward;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OfficerRewardTable {

    public static final String PATH = ExiledSector.configPath("atlas_officer_rewards.csv");
    static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(OfficerRewardTable.class);
    private static volatile Table table = Table.EMPTY;

    record Entry(RewardKind kind, CraftingTier tier) {
    }

    private record Table(Map<RewardKind, Float> shares, Map<String, Entry> items) {

        static final Table EMPTY = new Table(Map.of(), Map.of());
    }

    private OfficerRewardTable() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, OfficerRewardTable::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<RewardKind, Float> shares = new EnumMap<>(RewardKind.class);
        Map<String, Entry> items = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (id.isEmpty()) {
                    return;
                }
                String categoryText = ModCsv.text(row, "category");
                if (categoryText.isEmpty()) {
                    RewardKind kind = RewardKind.byId(id);
                    if (kind == null) {
                        csv.skipRow(id, "not a reward category");
                        return;
                    }
                    shares.put(kind, Math.max(0f, csv.number(row, "share", 0f, id)));
                    return;
                }
                RewardKind category = RewardKind.byId(categoryText);
                CraftingTier tier = CraftingTier.byId(ModCsv.text(row, "tier"));
                if (category != RewardKind.CRAFTING_ITEM && category != RewardKind.INHIBITOR) {
                    csv.skipRow(id, "category must be crafting_item or inhibitor");
                } else if (tier == null) {
                    csv.skipRow(id, "tier must be low, mid or high");
                } else if (!CarriedItem.isSafeId(id)) {
                    csv.skipRow(id, "id may only use letters, digits, '_', '.' and '-'");
                } else {
                    items.put(id, new Entry(category, tier));
                }
            });
        }
        csv.report(LOG);
        table = new Table(Collections.unmodifiableMap(shares), Collections.unmodifiableMap(items));
    }

    public static void clear() {
        table = Table.EMPTY;
    }

    public static Map<RewardKind, Float> shares() {
        return table.shares();
    }

    public static CraftingTier tierOf(String itemId) {
        Entry entry = table.items().get(itemId);
        return entry == null ? null : entry.tier();
    }

    public static RewardKind categoryOf(String itemId) {
        Entry entry = table.items().get(itemId);
        return entry == null ? null : entry.kind();
    }

    public static List<String> itemsIn(RewardKind category) {
        List<String> itemIds = new ArrayList<>();
        table.items().forEach((itemId, entry) -> {
            if (entry.kind() == category) {
                itemIds.add(itemId);
            }
        });
        return List.copyOf(itemIds);
    }
}
