package exiledsector.charts.content;

import exiledsector.data.CsvRows;
import exiledlib.ModCsv;
import exiledsector.items.MaterialItems;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class ChartColonyItems {

    static final String PATH = "data/config/exiledSector/chart_colony_items.csv";
    static final String ID_COLUMN = "id";
    static final String CEILING_ID = "ceiling";
    static final float DEFAULT_CEILING = 0.25f;
    private static final Logger LOG = Logger.getLogger(ChartColonyItems.class);
    private static volatile Table table = Table.EMPTY;

    record Item(String itemId, float weight) {
    }

    record Table(List<Item> items, float ceiling) {

        static final Table EMPTY = new Table(List.of(), DEFAULT_CEILING);

        Table {
            items = List.copyOf(items);
        }
    }

    private ChartColonyItems() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, rows -> register(rows, itemId -> MaterialItems.spec(itemId) != null));
    }

    static List<String> register(JSONArray rows, Predicate<String> itemExists) throws JSONException {
        List<Item> items = new ArrayList<>();
        float[] ceiling = {DEFAULT_CEILING};
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (id.isEmpty()) {
                    return;
                }
                float value = csv.number(row, "value", 0f, id);
                if (CEILING_ID.equals(id)) {
                    if (value > 0f && value < 1f) {
                        ceiling[0] = value;
                    } else {
                        csv.skipRow(id, "the ceiling must be above 0 and below 1, so an item is never certain; using " + DEFAULT_CEILING);
                    }
                } else if (!(value > 0f)) {
                    csv.skipRow(id, "an item needs a weight above 0");
                } else if (!itemExists.test(id)) {
                    csv.skipRow(id, "no special item with this id is loaded");
                } else {
                    items.add(new Item(id, value));
                }
            });
        }
        csv.report(LOG);
        table = new Table(items, ceiling[0]);
        return csv.problems();
    }

    public static void clear() {
        table = Table.EMPTY;
    }

    static float ceiling() {
        return table.ceiling();
    }

    static List<Item> items() {
        return table.items();
    }

    static float chance(float tierChance, float bonusMult) {
        float rawChance = tierChance * Math.max(0f, bonusMult);
        if (!(rawChance > 0f)) {
            return 0f;
        }
        float softCeiling = table.ceiling();
        return softCeiling * (1f - (float) Math.exp(-rawChance / softCeiling));
    }

    static String pick(Random random) {
        Item picked = WeightedPick.pick(table.items(), Item::weight, random);
        return picked == null ? null : picked.itemId();
    }
}
