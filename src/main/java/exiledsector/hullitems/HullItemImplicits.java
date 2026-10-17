package exiledsector.hullitems;

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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public final class HullItemImplicits {

    static final String DATA_PATH = "data/config/exiledSector/hull_item_implicits.csv";
    private static final String ENTRY_SEPARATOR = ";";
    private static final long IMPLICIT_SEED_SALT = 0x5DEECE66DL;
    private static final Logger LOG = Logger.getLogger(HullItemImplicits.class);
    private static final AtomicReference<Map<HullItemType, List<Modifier>>> BY_TYPE = new AtomicReference<>();

    private HullItemImplicits() {
    }

    public static void load() {
        ModCsv.load("type", DATA_PATH, LOG, HullItemImplicits::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<HullItemType, List<Modifier>> loaded = new EnumMap<>(HullItemType.class);
        ModCsv.forEach(rows, (index, row) -> {
            try {
                HullItemType type = HullItemType.byId(ModCsv.text(row, "type"));
                List<Modifier> implicits = parseImplicits(type, ModCsv.text(row, "implicits"));
                if (loaded.put(type, implicits) != null) {
                    throw new IllegalArgumentException(type.id() + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        BY_TYPE.set(Collections.unmodifiableMap(loaded));
    }

    static List<Modifier> parseImplicits(HullItemType type, String implicitsText) {
        if (type == null || !type.fitsSection()) {
            throw new IllegalArgumentException("implicits only belong to hull socket types, not subroutines");
        }
        List<Modifier> implicits = new ArrayList<>();
        for (String entryText : implicitsText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (HullItemModifiers.isReference(trimmed)) {
                addImplicit(implicits, referencedImplicit(trimmed));
                continue;
            }
            ModifierLine.Parsed parsed = ModifierLine.parse(trimmed);
            if (parsed.trailingField() != null) {
                throw new IllegalArgumentException("implicit \"" + trimmed + "\" has a weight, but implicits always roll");
            }
            if (parsed.line().scalesWithHullSize()) {
                throw new IllegalArgumentException(parsed.line().effectName() + " must roll a min:max range, not per-hull-size values");
            }
            String effectName = HullItemDefinition.currentEffectName(parsed.line().effectName());
            if (effectName == null) {
                continue;
            }
            addImplicit(implicits, Modifier.single(ModifierSlot.IMPLICIT, parsed.line().named(effectName)));
        }
        return List.copyOf(implicits);
    }

    private static Modifier referencedImplicit(String entryText) {
        String referenceId = entryText.substring(HullItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceId.contains(":")) {
            throw new IllegalArgumentException("implicit \"" + entryText + "\" has a weight, but implicits always roll");
        }
        return HullItemModifiers.referenced(referenceId, ModifierSlot.IMPLICIT);
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

    static List<RolledModifier> withCurrentIds(HullItemType type, List<RolledModifier> savedImplicits, long seed) {
        List<Modifier> implicits = forType(type);
        if (implicits.isEmpty() || matchesIds(implicits, savedImplicits)) {
            return savedImplicits;
        }
        Map<String, RolledEffect> savedLines = new HashMap<>();
        RolledModifier.lines(savedImplicits).forEach(line -> savedLines.putIfAbsent(line.effectName(), line));
        List<RolledModifier> freshRolls = roll(type, seed);
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

    public static List<Modifier> forType(HullItemType type) {
        Map<HullItemType, List<Modifier>> byType = BY_TYPE.get();
        return byType == null || type == null ? List.of() : byType.getOrDefault(type, List.of());
    }

    public static ModifierLine line(HullItemType type, String effectName) {
        for (Modifier implicit : forType(type)) {
            ModifierLine line = implicit.line(effectName);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    static List<RolledModifier> roll(HullItemType type, long seed) {
        List<Modifier> implicits = type == null ? List.of() : type.implicits();
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
    }
}
