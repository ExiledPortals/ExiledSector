package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
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

public final class StarChartImplicits {

    static final String DATA_PATH = "data/config/exiledSector/star_chart_implicits.csv";
    static final String SYSTEM_TYPE_COLUMN = "system_type";
    private static final String ENTRY_SEPARATOR = ";";
    private static final long IMPLICIT_SEED_SALT = 0x43484152544D50L;
    private static final Logger LOG = Logger.getLogger(StarChartImplicits.class);
    private static final AtomicReference<Loaded> LOADED = new AtomicReference<>();

    private record Loaded(Map<String, List<Modifier>> bySystemType, List<Modifier> corruptionPool) {
    }

    private StarChartImplicits() {
    }

    public static void load() {
        ModCsv.load(SYSTEM_TYPE_COLUMN, DATA_PATH, LOG, StarChartImplicits::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, List<Modifier>> bySystemType = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                String systemType = StarChartDefinitions.normalisedSystemType(ModCsv.text(row, SYSTEM_TYPE_COLUMN));
                if (systemType == null) {
                    throw new IllegalArgumentException("\"" + ModCsv.text(row, SYSTEM_TYPE_COLUMN) + "\" is not a star chart system type; expected "
                            + StarChartDefinitions.SYSTEM_TYPES + " or " + StarChartDefinitions.ANY_SYSTEM_TYPE);
                }
                List<Modifier> implicits = parseImplicits(ModCsv.text(row, "implicits"));
                if (bySystemType.putIfAbsent(systemType, implicits) != null) {
                    throw new IllegalArgumentException(systemType + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        Map<String, Modifier> distinctById = new LinkedHashMap<>();
        bySystemType.values().forEach(implicits -> implicits.forEach(implicit -> distinctById.putIfAbsent(implicit.id(), implicit)));
        LOADED.set(new Loaded(Collections.unmodifiableMap(bySystemType), List.copyOf(distinctById.values())));
    }

    private static List<Modifier> parseImplicits(String implicitsText) {
        List<Modifier> implicits = new ArrayList<>();
        for (String entryText : implicitsText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ItemModifiers.isReference(trimmed)) {
                throw new IllegalArgumentException("implicit \"" + trimmed + "\" must reference a modifier as @ID");
            }
            String referenceId = trimmed.substring(ItemModifiers.REFERENCE_MARK.length()).trim();
            if (referenceId.contains(":")) {
                throw new IllegalArgumentException("implicit \"" + trimmed + "\" has a weight, but implicits always roll");
            }
            Modifier implicit = ItemModifiers.referenced(referenceId, ModifierSlot.IMPLICIT, ItemType.STAR_CHART);
            for (Modifier existing : implicits) {
                if (existing.id().equals(implicit.id())) {
                    throw new IllegalArgumentException(implicit.id() + " is listed more than once");
                }
            }
            implicits.add(implicit);
        }
        return List.copyOf(implicits);
    }

    public static boolean isLoaded() {
        return LOADED.get() != null;
    }

    public static List<Modifier> forSystemType(String systemType) {
        Loaded loaded = LOADED.get();
        if (loaded == null || systemType == null) {
            return List.of();
        }
        return loaded.bySystemType().getOrDefault(systemType, List.of());
    }

    public static List<Modifier> corruptionPool() {
        Loaded loaded = LOADED.get();
        return loaded == null ? List.of() : loaded.corruptionPool();
    }

    static Modifier anyById(String modifierId) {
        for (Modifier implicit : corruptionPool()) {
            if (implicit.id().equals(modifierId)) {
                return implicit;
            }
        }
        return null;
    }

    static ModifierLine line(String systemType, String effectName) {
        for (Modifier implicit : forSystemType(systemType)) {
            ModifierLine line = implicit.line(effectName);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    static List<RolledModifier> roll(String systemType, long seed) {
        List<Modifier> implicits = forSystemType(systemType);
        if (implicits.isEmpty()) {
            return List.of();
        }
        Random random = new Random(seed ^ IMPLICIT_SEED_SALT);
        List<RolledModifier> rolled = new ArrayList<>(implicits.size());
        for (Modifier implicit : implicits) {
            rolled.add(RolledModifier.roll(implicit, random, false));
        }
        return List.copyOf(rolled);
    }

    public static void clear() {
        LOADED.set(null);
    }
}
