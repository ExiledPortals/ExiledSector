package exiledsector.socketables;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public final class HullItemImplicits {

    static final String DATA_PATH = "data/config/exiledSector/hull_item_implicits.csv";
    private static final String TYPE_COLUMN = "type";
    private static final String IMPLICITS_COLUMN = "implicits";
    private static final long IMPLICIT_SEED_SALT = 0x5DEECE66DL;
    private static final Logger LOG = Logger.getLogger(HullItemImplicits.class);
    private static final AtomicReference<Map<SocketType, List<PoolEntry>>> BY_TYPE = new AtomicReference<>();

    private HullItemImplicits() {
    }

    public static void load() {
        ModCsv.load(TYPE_COLUMN, DATA_PATH, LOG, HullItemImplicits::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<SocketType, List<PoolEntry>> loaded = new EnumMap<>(SocketType.class);
        ModCsv.forEach(rows, (index, row) -> {
            try {
                SocketType type = SocketType.byId(ModCsv.text(row, TYPE_COLUMN));
                List<PoolEntry> implicits = parseImplicits(type, ModCsv.text(row, IMPLICITS_COLUMN));
                if (loaded.put(type, implicits) != null) {
                    throw new IllegalArgumentException(type.id() + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        BY_TYPE.set(Collections.unmodifiableMap(loaded));
    }

    static List<PoolEntry> parseImplicits(SocketType type, String implicitsText) {
        if (type == null || !type.isFramework()) {
            throw new IllegalArgumentException("implicits only belong to hull socket types, not subroutines");
        }
        List<PoolEntry> implicits = SocketableDefinition.parsePool(implicitsText);
        for (PoolEntry implicit : implicits) {
            if (implicit.scalesWithHullSize()) {
                throw new IllegalArgumentException(implicit.effectName() + " must roll a min:max range, not per-hull-size values");
            }
        }
        return implicits;
    }

    public static boolean isLoaded() {
        return BY_TYPE.get() != null;
    }

    public static List<PoolEntry> forType(SocketType type) {
        Map<SocketType, List<PoolEntry>> byType = BY_TYPE.get();
        return byType == null || type == null ? List.of() : byType.getOrDefault(type, List.of());
    }

    public static PoolEntry entry(SocketType type, String effectName) {
        for (PoolEntry implicit : forType(type)) {
            if (implicit.effectName().equals(effectName)) {
                return implicit;
            }
        }
        return null;
    }

    static List<RolledEffect> roll(SocketType type, long seed) {
        List<PoolEntry> implicits = forType(type);
        if (implicits.isEmpty()) {
            return List.of();
        }
        Random random = new Random(seed ^ IMPLICIT_SEED_SALT);
        List<RolledEffect> rolled = new ArrayList<>(implicits.size());
        for (PoolEntry implicit : implicits) {
            rolled.add(new RolledEffect(implicit.effectName(), implicit.roll(random)));
        }
        return List.copyOf(rolled);
    }

    public static void clear() {
        BY_TYPE.set(null);
    }
}
