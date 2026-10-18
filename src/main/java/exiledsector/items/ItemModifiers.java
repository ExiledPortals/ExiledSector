package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class ItemModifiers {

    static final String DATA_PATH = "data/config/exiledSector/item_modifiers.csv";
    static final String REFERENCE_MARK = "@";
    static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final String LINE_SEPARATOR = ";";
    private static final Logger LOG = Logger.getLogger(ItemModifiers.class);
    private static final AtomicReference<Catalogue> LOADED = new AtomicReference<>(Catalogue.EMPTY);

    private record Catalogue(Map<String, Modifier> modifiersById, Map<String, String> namesById, Map<String, Set<ItemType>> itemTypesById) {
        static final Catalogue EMPTY = new Catalogue(Map.of(), Map.of(), Map.of());
    }

    private ItemModifiers() {
    }

    public static void load() {
        ModCsv.load("id", DATA_PATH, LOG, ItemModifiers::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Modifier> modifiersById = new LinkedHashMap<>();
        Map<String, String> namesById = new LinkedHashMap<>();
        Map<String, Set<ItemType>> itemTypesById = new LinkedHashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            try {
                Modifier modifier = parse(row);
                Set<ItemType> itemTypes = parseItemTypes(modifier.id(), ModCsv.text(row, "item_types"));
                if (modifiersById.putIfAbsent(modifier.id(), modifier) != null) {
                    throw new IllegalArgumentException(modifier.id() + " is listed more than once");
                }
                itemTypesById.put(modifier.id(), itemTypes);
                String englishName = ModCsv.text(row, "name");
                if (!englishName.isEmpty()) {
                    namesById.put(modifier.id(), englishName);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        LOADED.set(new Catalogue(Collections.unmodifiableMap(modifiersById), Collections.unmodifiableMap(namesById),
                Collections.unmodifiableMap(itemTypesById)));
    }

    static Modifier parse(JSONObject row) {
        String modifierId = ModCsv.text(row, "id");
        if (!ID_PATTERN.matcher(modifierId).matches()) {
            throw new IllegalArgumentException("id \"" + modifierId + "\" must be letters, digits and underscores");
        }
        String slotText = ModCsv.text(row, "slot");
        ModifierSlot slot = ModifierSlot.byIdOrNull(slotText);
        if (slot == null) {
            throw new IllegalArgumentException(modifierId + " has slot \"" + slotText + "\"; expected implicit, prefix or suffix");
        }
        return new Modifier(modifierId, slot, parseLines(modifierId, slot, ModCsv.text(row, "lines")));
    }

    static Set<ItemType> parseItemTypes(String modifierId, String itemTypesText) {
        Set<ItemType> itemTypes = EnumSet.noneOf(ItemType.class);
        for (String itemTypeText : itemTypesText.split(LINE_SEPARATOR)) {
            String trimmed = itemTypeText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ItemType itemType = ItemType.byIdOrNull(trimmed);
            if (itemType == null) {
                throw new IllegalArgumentException(modifierId + " lists item type \"" + trimmed + "\"; expected " + ItemType.ids());
            }
            itemTypes.add(itemType);
        }
        if (itemTypes.isEmpty()) {
            throw new IllegalArgumentException(modifierId + " needs at least one item type in item_types");
        }
        return Collections.unmodifiableSet(itemTypes);
    }

    private static List<ModifierLine> parseLines(String modifierId, ModifierSlot slot, String linesText) {
        List<ModifierLine> lines = new ArrayList<>();
        for (String lineText : linesText.split(LINE_SEPARATOR)) {
            String trimmed = lineText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ModifierLine.Parsed parsed = ModifierLine.parse(trimmed);
            if (parsed.trailingField() != null) {
                throw new IllegalArgumentException(modifierId + " line \"" + trimmed + "\" has a weight; weights belong on the pool entry");
            }
            if (slot == ModifierSlot.IMPLICIT && parsed.line().scalesWithHullSize()) {
                throw new IllegalArgumentException(modifierId + " is an implicit, so " + parsed.line().effectName() + " must roll a min:max range");
            }
            String effectName = ItemDefinition.currentEffectName(parsed.line().effectName());
            if (effectName == null) {
                throw new IllegalArgumentException(modifierId + " uses " + parsed.line().effectName() + ", which is not a skill effect");
            }
            if (lines.stream().anyMatch(existing -> existing.effectName().equals(effectName))) {
                throw new IllegalArgumentException(modifierId + " lists " + effectName + " more than once");
            }
            lines.add(parsed.line().named(effectName));
        }
        return lines;
    }

    static boolean isReference(String entryText) {
        return entryText.startsWith(REFERENCE_MARK);
    }

    static Modifier referenced(String referenceId, ModifierSlot expectedSlot, ItemType itemType) {
        Modifier modifier = get(referenceId);
        if (modifier == null) {
            throw new IllegalArgumentException("no modifier " + referenceId + " in " + DATA_PATH);
        }
        if (modifier.slot() != expectedSlot) {
            throw new IllegalArgumentException("modifier " + referenceId + " is in the " + modifier.slot().id() + " slot, not the " + expectedSlot.id() + " slot");
        }
        if (itemType != null && !itemTypes(referenceId).contains(itemType)) {
            throw new IllegalArgumentException("modifier " + referenceId + " does not roll on the " + itemType.id() + " item type");
        }
        return modifier;
    }

    public static Modifier get(String modifierId) {
        return modifierId == null ? null : LOADED.get().modifiersById().get(modifierId);
    }

    public static Set<ItemType> itemTypes(String modifierId) {
        return modifierId == null ? Set.of() : LOADED.get().itemTypesById().getOrDefault(modifierId, Set.of());
    }

    public static Collection<Modifier> all() {
        return LOADED.get().modifiersById().values();
    }

    static boolean hasName(String modifierId) {
        return modifierId != null && LOADED.get().namesById().containsKey(modifierId);
    }

    static String name(String modifierId) {
        String englishName = modifierId == null ? null : LOADED.get().namesById().get(modifierId);
        return englishName == null ? null : Translation.data("socketable.modifier." + modifierId, englishName);
    }

    static Modifier namedForEffect(String effectName, ModifierSlot slot) {
        Catalogue catalogue = LOADED.get();
        Modifier sameId = catalogue.modifiersById().get(effectName);
        if (sameId != null && sameId.slot() == slot && catalogue.namesById().containsKey(effectName)) {
            return sameId;
        }
        for (Modifier modifier : catalogue.modifiersById().values()) {
            if (modifier.slot() == slot && !modifier.isHybrid() && modifier.line().effectName().equals(effectName)
                    && catalogue.namesById().containsKey(modifier.id())) {
                return modifier;
            }
        }
        return null;
    }

    public static void clear() {
        LOADED.set(Catalogue.EMPTY);
    }
}
