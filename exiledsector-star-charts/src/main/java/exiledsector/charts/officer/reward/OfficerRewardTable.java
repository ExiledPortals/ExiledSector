package exiledsector.charts.officer.reward;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class OfficerRewardTable {

    public static final String PATH = ExiledSector.configPath("atlas_officer_rewards.csv");
    static final String ID_COLUMN = "id";
    static final String WEIGHT_COLUMN = "weight";
    static final String MIN_COUNT_COLUMN = "min_count";
    static final String MAX_COUNT_COLUMN = "max_count";
    private static final Logger LOG = Logger.getLogger(OfficerRewardTable.class);
    private static volatile Table table = Table.EMPTY;

    record Entry(RewardKind kind, CraftingTier tier, float weight, int minCount, int maxCount) {

        int rollCount(Random random) {
            return minCount + random.nextInt(maxCount - minCount + 1);
        }
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
                if (!CarriedItem.isSafeId(id)) {
                    csv.skipRow(id, "id may only use letters, digits, '_', '.' and '-'");
                    return;
                }
                Entry entry = entry(RewardKind.byId(categoryText), row, csv, id);
                if (entry != null) {
                    items.put(id, entry);
                }
            });
        }
        csv.report(LOG);
        table = new Table(Collections.unmodifiableMap(shares), Collections.unmodifiableMap(items));
    }

    private static Entry entry(RewardKind category, JSONObject row, CsvRows csv, String id) {
        if (category == RewardKind.CRAFTING_ITEM || category == RewardKind.INHIBITOR) {
            CraftingTier tier = CraftingTier.byId(ModCsv.text(row, "tier"));
            if (tier == null) {
                csv.skipRow(id, "tier must be low, mid or high");
                return null;
            }
            return new Entry(category, tier, 0f, 1, tier.maxCount());
        }
        if (category == RewardKind.FEEDSTOCK) {
            float weight = csv.number(row, WEIGHT_COLUMN, 0f, id);
            int minCount = csv.integer(row, MIN_COUNT_COLUMN, 0, id);
            int maxCount = csv.integer(row, MAX_COUNT_COLUMN, minCount, id);
            if (!(weight > 0f) || minCount < 1 || maxCount < minCount) {
                csv.skipRow(id, "a feedstock needs a positive weight and counts from 1 with max_count at least min_count");
                return null;
            }
            return new Entry(category, null, weight, minCount, maxCount);
        }
        csv.skipRow(id, "category must be crafting_item, inhibitor or feedstock");
        return null;
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

    public static float weightOf(String itemId) {
        Entry entry = table.items().get(itemId);
        return entry == null ? 0f : entry.weight();
    }

    public static int rollCount(String itemId, Random random) {
        Entry entry = table.items().get(itemId);
        return entry == null ? 1 : entry.rollCount(random);
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
