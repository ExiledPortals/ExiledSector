package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public final class PrototypeImplicits {

    static final String DATA_FILE = "prototype_implicits.csv";
    public static final String BASE_COLUMN = "base";
    public static final String IMPLICITS_COLUMN = "implicits";
    private static final Logger LOG = Logger.getLogger(PrototypeImplicits.class);
    private static final AtomicReference<Pools> POOLS = new AtomicReference<>(Pools.EMPTY);

    private PrototypeImplicits() {
    }

    public static String dataPath() {
        return ExiledSector.configPath(DATA_FILE);
    }

    public static void load() {
        ModCsv.load(BASE_COLUMN, dataPath(), LOG, PrototypeImplicits::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, List<Modifier>> byBase = new LinkedHashMap<>();
        Map<String, Modifier> byId = new LinkedHashMap<>();
        CsvRows.forEachRow(rows, dataPath(), LOG, (index, row) -> {
            String baseId = ModCsv.text(row, BASE_COLUMN);
            if (byBase.containsKey(baseId)) {
                throw new IllegalArgumentException(baseId + " is listed more than once");
            }
            List<Modifier> implicits = new ArrayList<>();
            for (String entry : CsvRows.entries(ModCsv.text(row, IMPLICITS_COLUMN))) {
                Modifier implicit = ItemImplicits.referencedImplicit(entry.trim(), ItemType.HULL_ITEM);
                if (byId.containsKey(implicit.id())) {
                    throw new IllegalArgumentException(implicit.id() + " is listed more than once");
                }
                implicits.add(implicit);
            }
            if (implicits.isEmpty()) {
                throw new IllegalArgumentException(baseId + " has no prototype implicits");
            }
            implicits.forEach(implicit -> byId.put(implicit.id(), implicit));
            byBase.put(baseId, List.copyOf(implicits));
        });
        POOLS.set(new Pools(Collections.unmodifiableMap(byBase), Collections.unmodifiableMap(byId)));
    }

    public static List<String> bases() {
        return List.copyOf(POOLS.get().byBase().keySet());
    }

    public static Map<String, List<Modifier>> byBase() {
        return POOLS.get().byBase();
    }

    public static List<Modifier> forBase(String baseId) {
        return baseId == null ? List.of() : POOLS.get().byBase().getOrDefault(baseId, List.of());
    }

    public static Modifier byId(String modifierId) {
        return modifierId == null ? null : POOLS.get().byId().get(modifierId);
    }

    public static boolean isPrototypeImplicit(String modifierId) {
        return byId(modifierId) != null;
    }

    public static ModifierLine line(String baseId, String effectName) {
        return Modifier.lineIn(forBase(baseId), effectName);
    }

    public static ItemData create(ItemDefinition base, long seed, Random random) {
        List<Modifier> pool = base == null ? List.of() : forBase(base.id());
        if (pool.isEmpty()) {
            return null;
        }
        Item item = ItemData.rolled(base, seed).preview();
        if (item == null) {
            return null;
        }
        List<RolledModifier> implicits = new ArrayList<>(item.implicits());
        implicits.add(RolledModifier.roll(pool.get(random.nextInt(pool.size())), random));
        item.replaceImplicits(implicits);
        return ItemData.of(item);
    }

    public static RolledModifier prototypeImplicit(Item item) {
        for (RolledModifier implicit : item.implicits()) {
            if (isPrototypeImplicit(implicit.modifierId())) {
                return implicit;
            }
        }
        return null;
    }

    public static void clear() {
        POOLS.set(Pools.EMPTY);
    }

    private record Pools(Map<String, List<Modifier>> byBase, Map<String, Modifier> byId) {
        static final Pools EMPTY = new Pools(Map.of(), Map.of());
    }
}
