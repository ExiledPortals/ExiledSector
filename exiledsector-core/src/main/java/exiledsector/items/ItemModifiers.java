package exiledsector.items;

import exiledlib.HostMod;
import exiledlib.ModCsv;
import exiledlib.i18n.Translation;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierEffects;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ItemModifiers {

    static final String DATA_FILE = "item_modifiers.csv";
    public static final String REFERENCE_MARK = "@";
    public static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final String LINE_SEPARATOR = ";";
    private static final Logger LOG = Logger.getLogger(ItemModifiers.class);
    private static final AtomicReference<Catalogue> LOADED = new AtomicReference<>(Catalogue.EMPTY);
    private static final AtomicReference<Catalogue> SHIP_ITEM_PART = new AtomicReference<>(Catalogue.EMPTY);
    private static final AtomicReference<Catalogue> STAR_CHART_PART = new AtomicReference<>(Catalogue.EMPTY);

    private record Catalogue(Map<String, Modifier> modifiersById, Map<String, String> namesById, Map<String, Set<ModifierScope>> scopesById) {
        static final Catalogue EMPTY = new Catalogue(Map.of(), Map.of(), Map.of());

        static Catalogue merged(Catalogue first, Catalogue second) {
            if (second.modifiersById().isEmpty()) {
                return first;
            }
            Map<String, Modifier> modifiersById = new LinkedHashMap<>(first.modifiersById());
            Map<String, String> namesById = new LinkedHashMap<>(first.namesById());
            Map<String, Set<ModifierScope>> scopesById = new LinkedHashMap<>(first.scopesById());
            second.modifiersById().forEach((modifierId, modifier) -> {
                if (modifiersById.putIfAbsent(modifierId, modifier) == null) {
                    scopesById.put(modifierId, second.scopesById().get(modifierId));
                    String englishName = second.namesById().get(modifierId);
                    if (englishName != null) {
                        namesById.put(modifierId, englishName);
                    }
                }
            });
            return new Catalogue(Collections.unmodifiableMap(modifiersById), Collections.unmodifiableMap(namesById),
                    Collections.unmodifiableMap(scopesById));
        }
    }

    private ItemModifiers() {
    }

    public static String dataPath() {
        return HostMod.configPath(DATA_FILE);
    }

    public static void load() {
        ModCsv.load("id", dataPath(), LOG, ItemModifiers::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        SHIP_ITEM_PART.set(catalogueOf(rows, dataPath(), itemTypes -> true));
        LOADED.set(Catalogue.merged(SHIP_ITEM_PART.get(), STAR_CHART_PART.get()));
    }

    public static void registerStarCharts(JSONArray rows, String sourcePath) throws JSONException {
        Catalogue shipItemModifiers = SHIP_ITEM_PART.get();
        STAR_CHART_PART.set(catalogueOf(rows == null ? new JSONArray() : rows, sourcePath, scopes -> scopes.equals(Set.of(ModifierScope.STAR_CHART)),
                shipItemModifiers.modifiersById().keySet()));
        LOADED.set(Catalogue.merged(shipItemModifiers, STAR_CHART_PART.get()));
    }

    private static Catalogue catalogueOf(JSONArray rows, String sourcePath, Predicate<Set<ModifierScope>> allowedScopes) throws JSONException {
        return catalogueOf(rows, sourcePath, allowedScopes, Set.of());
    }

    private static Catalogue catalogueOf(JSONArray rows, String sourcePath, Predicate<Set<ModifierScope>> allowedScopes, Set<String> takenIds)
            throws JSONException {
        Map<String, Modifier> modifiersById = new LinkedHashMap<>();
        Map<String, String> namesById = new LinkedHashMap<>();
        Map<String, Set<ModifierScope>> scopesById = new LinkedHashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            try {
                Modifier modifier = parse(row);
                Set<ModifierScope> scopes = parseScopes(modifier.id(), ModCsv.text(row, "item_types"));
                if (!allowedScopes.test(scopes)) {
                    throw new IllegalArgumentException(modifier.id() + " lists item types " + scopes.stream().map(ModifierScope::id).toList()
                            + ", which do not belong in this file");
                }
                if (takenIds.contains(modifier.id()) || modifiersById.putIfAbsent(modifier.id(), modifier) != null) {
                    throw new IllegalArgumentException(modifier.id() + " is listed more than once");
                }
                scopesById.put(modifier.id(), scopes);
                String englishName = ModCsv.text(row, "name");
                if (!englishName.isEmpty()) {
                    namesById.put(modifier.id(), englishName);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + sourcePath + ": " + e.getMessage());
            }
        });
        return new Catalogue(Collections.unmodifiableMap(modifiersById), Collections.unmodifiableMap(namesById),
                Collections.unmodifiableMap(scopesById));
    }

    public static Modifier parse(JSONObject row) {
        String modifierId = ModCsv.text(row, "id");
        if (!ID_PATTERN.matcher(modifierId).matches()) {
            throw new IllegalArgumentException("id \"" + modifierId + "\" must be letters, digits and underscores");
        }
        String slotText = ModCsv.text(row, "slot");
        ModifierSlot slot = ModifierSlot.byIdOrNull(slotText);
        if (slot == null) {
            throw new IllegalArgumentException(modifierId + " has slot \"" + slotText + "\"; expected implicit, prefix or suffix");
        }
        ModifierEffects effects = effectsForListedTypes(ModCsv.text(row, "item_types"));
        return new Modifier(modifierId, slot, parseLines(modifierId, slot, effects, ModCsv.text(row, "lines")));
    }

    public static Set<ModifierScope> parseScopes(String modifierId, String itemTypesText) {
        Set<ModifierScope> scopes = EnumSet.noneOf(ModifierScope.class);
        for (String scopeText : itemTypesText.split(LINE_SEPARATOR)) {
            String trimmed = scopeText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ModifierScope scope = ModifierScope.byIdOrNull(trimmed);
            if (scope == null) {
                throw new IllegalArgumentException(modifierId + " lists item type \"" + trimmed + "\"; expected " + ModifierScope.ids());
            }
            scopes.add(scope);
        }
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException(modifierId + " needs at least one item type in item_types");
        }
        return Collections.unmodifiableSet(scopes);
    }

    public static Set<ItemType> itemTypesOf(Set<ModifierScope> scopes) {
        Set<ItemType> itemTypes = EnumSet.noneOf(ItemType.class);
        scopes.forEach(scope -> itemTypes.add(scope.itemType()));
        return Collections.unmodifiableSet(itemTypes);
    }

    private static ModifierEffects effectsForListedTypes(String itemTypesText) {
        Set<ModifierScope> scopes;
        try {
            scopes = parseScopes("", itemTypesText);
        } catch (IllegalArgumentException e) {
            return ItemEffects.SKILL_EFFECTS;
        }
        return ItemEffects.forTypes(itemTypesOf(scopes));
    }

    private static List<ModifierLine> parseLines(String modifierId, ModifierSlot slot, ModifierEffects effects, String linesText) {
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
            String effectName = ItemDefinition.currentEffectName(parsed.line().effectName(), effects);
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

    public static boolean isReference(String entryText) {
        return entryText.startsWith(REFERENCE_MARK);
    }

    public static Modifier referenced(String referenceId, ModifierSlot expectedSlot, ItemType itemType) {
        return referenced(referenceId, expectedSlot, ModifierScope.of(itemType));
    }

    public static Modifier referenced(String referenceId, ModifierSlot expectedSlot, ModifierScope scope) {
        Modifier modifier = get(referenceId);
        if (modifier == null) {
            throw new IllegalArgumentException("no modifier " + referenceId + " in " + dataPath());
        }
        if (modifier.slot() != expectedSlot) {
            throw new IllegalArgumentException("modifier " + referenceId + " is in the " + modifier.slot().id() + " slot, not the " + expectedSlot.id() + " slot");
        }
        if (scope != null && !scopes(referenceId).contains(scope)) {
            throw new IllegalArgumentException("modifier " + referenceId + " does not roll on the " + scope.id() + " item type");
        }
        return modifier;
    }

    public static Modifier get(String modifierId) {
        return modifierId == null ? null : LOADED.get().modifiersById().get(modifierId);
    }

    public static Set<ModifierScope> scopes(String modifierId) {
        return modifierId == null ? Set.of() : LOADED.get().scopesById().getOrDefault(modifierId, Set.of());
    }

    public static Set<ItemType> itemTypes(String modifierId) {
        return itemTypesOf(scopes(modifierId));
    }

    public static Collection<Modifier> all() {
        return LOADED.get().modifiersById().values();
    }

    public static boolean hasName(String modifierId) {
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

    public static void clearStarCharts() {
        STAR_CHART_PART.set(Catalogue.EMPTY);
        LOADED.set(SHIP_ITEM_PART.get());
    }

    public static void clear() {
        LOADED.set(Catalogue.EMPTY);
        SHIP_ITEM_PART.set(Catalogue.EMPTY);
        STAR_CHART_PART.set(Catalogue.EMPTY);
    }
}
