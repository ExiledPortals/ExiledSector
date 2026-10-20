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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public final class ItemImplicits {

    static final String DATA_PATH = "data/config/exiledSector/hull_item_implicits.csv";
    static final String CORRUPTION_DATA_PATH = "data/config/exiledSector/hull_item_corruption_implicits.csv";
    private static final String ENTRY_SEPARATOR = ";";
    private static final long IMPLICIT_SEED_SALT = 0x5DEECE66DL;
    private static final Logger LOG = Logger.getLogger(ItemImplicits.class);
    private static final AtomicReference<Map<ItemKind, List<Modifier>>> BY_TYPE = new AtomicReference<>();
    private static final AtomicReference<CorruptionPool> CORRUPTION_POOL = new AtomicReference<>();
    private static final CorruptionImplicitPool CORRUPTION_ONLY_IMPLICITS = new CorruptionImplicitPool(CORRUPTION_DATA_PATH, ItemType.HULL_ITEM, LOG);

    private ItemImplicits() {
    }

    public static void load() {
        ModCsv.load("section", DATA_PATH, LOG, ItemImplicits::register);
        CORRUPTION_ONLY_IMPLICITS.load();
    }

    public static void registerCorruptionPool(JSONArray rows) throws JSONException {
        CORRUPTION_ONLY_IMPLICITS.register(rows);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<ItemKind, List<Modifier>> loaded = new EnumMap<>(ItemKind.class);
        ModCsv.forEach(rows, (index, row) -> {
            try {
                ItemKind kind = ItemKind.byId(ModCsv.text(row, "section"));
                List<Modifier> implicits = parseImplicits(kind, ModCsv.text(row, "implicits"));
                if (loaded.put(kind, implicits) != null) {
                    throw new IllegalArgumentException(kind.id() + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        BY_TYPE.set(Collections.unmodifiableMap(loaded));
    }

    static List<Modifier> parseImplicits(ItemKind kind, String implicitsText) {
        if (kind == null || !kind.fitsSection()) {
            throw new IllegalArgumentException("implicits only belong to hull items, not subroutines");
        }
        List<Modifier> implicits = new ArrayList<>();
        for (String entryText : implicitsText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (ItemModifiers.isReference(trimmed)) {
                addImplicit(implicits, referencedImplicit(trimmed, kind.type()));
                continue;
            }
            ModifierLine.Parsed parsed = ModifierLine.parse(trimmed);
            if (parsed.trailingField() != null) {
                throw new IllegalArgumentException("implicit \"" + trimmed + "\" has a weight, but implicits always roll");
            }
            if (parsed.line().scalesWithHullSize()) {
                throw new IllegalArgumentException(parsed.line().effectName() + " must roll a min:max range, not per-hull-size values");
            }
            String effectName = ItemDefinition.currentEffectName(parsed.line().effectName(), ItemEffects.forType(kind.type()));
            if (effectName == null) {
                continue;
            }
            addImplicit(implicits, Modifier.single(ModifierSlot.IMPLICIT, parsed.line().named(effectName)));
        }
        return List.copyOf(implicits);
    }

    private static Modifier referencedImplicit(String entryText, ItemType itemType) {
        String referenceId = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceId.contains(":")) {
            throw new IllegalArgumentException("implicit \"" + entryText + "\" has a weight, but implicits always roll");
        }
        return ItemModifiers.referenced(referenceId, ModifierSlot.IMPLICIT, itemType);
    }

    private static void addImplicit(List<Modifier> implicits, Modifier implicit) {
        for (Modifier existing : implicits) {
            if (existing.id().equals(implicit.id())) {
                throw new IllegalArgumentException(implicit.id() + " is listed more than once");
            }
            for (ModifierLine line : implicit.lines()) {
                if (existing.line(line.effectName()) != null) {
                    throw new IllegalArgumentException(line.effectName() + " is in both " + existing.id() + " and " + implicit.id());
                }
            }
        }
        implicits.add(implicit);
    }

    static List<RolledModifier> withCurrentIds(ItemKind kind, List<RolledModifier> savedImplicits, long seed) {
        List<Modifier> implicits = forType(kind);
        if (implicits.isEmpty() || matchesIds(implicits, savedImplicits)) {
            return savedImplicits;
        }
        Map<String, RolledEffect> savedLines = new HashMap<>();
        RolledModifier.lines(savedImplicits).forEach(line -> savedLines.putIfAbsent(line.effectName(), line));
        List<RolledModifier> freshRolls = roll(kind, seed);
        List<RolledModifier> resolved = new ArrayList<>(implicits.size());
        for (int index = 0; index < implicits.size(); index++) {
            Modifier implicit = implicits.get(index);
            List<RolledEffect> keptLines = new ArrayList<>(implicit.lines().size());
            for (ModifierLine line : implicit.lines()) {
                RolledEffect savedLine = savedLines.get(line.effectName());
                if (savedLine != null) {
                    keptLines.add(savedLine);
                }
            }
            resolved.add(keptLines.size() == implicit.lines().size()
                    ? new RolledModifier(implicit.id(), ModifierSlot.IMPLICIT, keptLines) : freshRolls.get(index));
        }
        return resolved;
    }

    private static boolean matchesIds(List<Modifier> implicits, List<RolledModifier> savedImplicits) {
        if (implicits.size() != savedImplicits.size()) {
            return false;
        }
        for (int index = 0; index < implicits.size(); index++) {
            if (!implicits.get(index).id().equals(savedImplicits.get(index).modifierId())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isLoaded() {
        return BY_TYPE.get() != null;
    }

    public static List<Modifier> forType(ItemKind kind) {
        Map<ItemKind, List<Modifier>> byType = BY_TYPE.get();
        return byType == null || kind == null ? List.of() : byType.getOrDefault(kind, List.of());
    }

    public static List<Modifier> corruptionPool() {
        Map<ItemKind, List<Modifier>> byType = BY_TYPE.get();
        if (byType == null) {
            return List.of();
        }
        List<Modifier> corruptionOnly = CORRUPTION_ONLY_IMPLICITS.implicits();
        CorruptionPool cached = CORRUPTION_POOL.get();
        if (cached != null && cached.source() == byType && cached.corruptionOnly() == corruptionOnly) {
            return cached.implicits();
        }
        List<Modifier> pool = buildCorruptionPool(byType, corruptionOnly);
        CORRUPTION_POOL.set(new CorruptionPool(byType, corruptionOnly, pool));
        return pool;
    }

    private record CorruptionPool(Map<ItemKind, List<Modifier>> source, List<Modifier> corruptionOnly, List<Modifier> implicits) {
    }

    private static List<Modifier> buildCorruptionPool(Map<ItemKind, List<Modifier>> byType, List<Modifier> corruptionOnly) {
        Map<String, Modifier> distinctById = new LinkedHashMap<>();
        corruptionOnly.forEach(implicit -> distinctById.putIfAbsent(implicit.id(), implicit));
        for (ItemKind kind : ItemKind.values()) {
            if (kind.type() == ItemType.HULL_ITEM) {
                byType.getOrDefault(kind, List.of()).forEach(implicit -> distinctById.putIfAbsent(implicit.id(), implicit));
            }
        }
        return List.copyOf(distinctById.values());
    }

    public static Modifier anyById(String modifierId) {
        for (Modifier implicit : corruptionPool()) {
            if (implicit.id().equals(modifierId)) {
                return implicit;
            }
        }
        return null;
    }

    public static ModifierLine line(ItemKind kind, String effectName) {
        for (Modifier implicit : forType(kind)) {
            ModifierLine line = implicit.line(effectName);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    static List<RolledModifier> roll(ItemKind kind, long seed) {
        List<Modifier> implicits = kind == null ? List.of() : kind.implicits();
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
        BY_TYPE.set(null);
        CORRUPTION_ONLY_IMPLICITS.clear();
    }
}
