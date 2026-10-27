package exiledsector.items;

import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique,
                                   String description, List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock, ItemType type,
                                   Enum<?> subtype, List<UniqueSource> uniqueSources, int socketLimit, String mechanic,
                                   String area) {

    public static final String FALLBACK_ICON = "graphics/icons/cargo/chip1.png";
    public static final int NO_SOCKET_LIMIT = 0;
    public static final String ONE_OF_MECHANIC = "one_of";
    private static final Logger LOG = Logger.getLogger(ItemDefinition.class);
    private static final String ENTRY_SEPARATOR = ";";
    static final String SECTION_COLUMN = "section";

    public ItemDefinition {
        if (kind != null) {
            type = kind.type();
        }
        if (type != ItemType.SUBROUTINE) {
            subtype = null;
        }
        uniqueSources = uniqueSources == null ? List.of() : List.copyOf(uniqueSources);
        socketLimit = Math.max(NO_SOCKET_LIMIT, socketLimit);
        mechanic = mechanic == null || mechanic.isBlank() ? null : mechanic.trim();
        area = area == null || area.isBlank() ? null : area.trim();
    }

    public ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique, String description,
                          List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock, ItemType type, Enum<?> subtype,
                          List<UniqueSource> uniqueSources, int socketLimit, String mechanic) {
        this(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, type, subtype, uniqueSources, socketLimit, mechanic, null);
    }

    public ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique, String description,
                          List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock, ItemType type, Enum<?> subtype,
                          List<UniqueSource> uniqueSources, int socketLimit) {
        this(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, type, subtype, uniqueSources, socketLimit, null);
    }

    public ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique, String description,
                          List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock) {
        this(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, null, null, List.of(), NO_SOCKET_LIMIT);
    }

    public boolean keepsOneModifier() {
        return unique && ONE_OF_MECHANIC.equals(mechanic);
    }

    public boolean limitsSocketsPerShip() {
        return socketLimit > NO_SOCKET_LIMIT;
    }

    public ItemDefinition withSocketLimit(int limit) {
        return new ItemDefinition(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, type, subtype,
                uniqueSources, limit, mechanic, area);
    }

    public ItemDefinition withMechanic(String itemMechanic) {
        return new ItemDefinition(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, type, subtype,
                uniqueSources, socketLimit, itemMechanic, area);
    }

    public ItemDefinition withArea(String itemArea) {
        return new ItemDefinition(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, type, subtype,
                uniqueSources, socketLimit, mechanic, itemArea);
    }

    public static ItemDefinition parseStarChart(JSONObject row) {
        return parse(row, null, ItemType.STAR_CHART);
    }

    public static ItemDefinition parse(JSONObject row, ItemKind kind, List<UniqueSource> uniqueSources) {
        return parse(row, kind, kind.type(), uniqueSources);
    }

    public static ItemDefinition parse(JSONObject row, ItemKind kind, ItemType itemType) {
        return parse(row, kind, itemType, List.of());
    }

    private static ItemDefinition parse(JSONObject row, ItemKind kind, ItemType itemType, List<UniqueSource> uniqueSources) {
        String definitionId = definitionId(row);
        List<PoolEntry> prefixes = parsePool(definitionId, row.optString("prefixes", ""), ModifierSlot.PREFIX, itemType);
        List<PoolEntry> suffixes = parsePool(definitionId, row.optString("suffixes", ""), ModifierSlot.SUFFIX, itemType);
        if (prefixes.isEmpty() && suffixes.isEmpty()) {
            throw new IllegalArgumentException("it has no prefixes or suffixes");
        }
        rejectRepeats(prefixes, suffixes);
        return parseShared(row, kind, itemType, new Pools(prefixes, suffixes), null, uniqueSources);
    }

    public record Pools(List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
    }

    public static String definitionId(JSONObject row) {
        String definitionId = row.optString("id", "").trim();
        if (definitionId.isEmpty()) {
            throw new IllegalArgumentException("a row has no id");
        }
        return definitionId;
    }

    public static ItemDefinition parseShared(JSONObject row, ItemKind kind, ItemType itemType, Pools pools, Enum<?> subtype,
                                              List<UniqueSource> uniqueSources) {
        boolean unique = "true".equalsIgnoreCase(row.optString("unique", "").trim());
        return parseCommon(row, kind, itemType, pools, subtype, unique, uniqueSources);
    }

    public static ItemDefinition parseCommon(JSONObject row, ItemKind kind, ItemType itemType, Pools pools, Enum<?> subtype,
                                              boolean unique, List<UniqueSource> uniqueSources) {
        String definitionId = definitionId(row);
        String icon = row.optString("icon", "").trim();
        float rarity = (float) row.optDouble("rarity", 1.0);
        if (!(rarity >= 0f)) {
            throw new IllegalArgumentException("rarity must be zero or more");
        }
        return new ItemDefinition(definitionId, kind,
                nameOrId(row.optString("name", "").trim(), definitionId), icon.isEmpty() ? FALLBACK_ICON : icon, rarity, unique,
                row.optString("description", "").trim(), pools.prefixes(), pools.suffixes(),
                row.optString("unlock", "").trim(), itemType, subtype, uniqueSources, NO_SOCKET_LIMIT);
    }

    public boolean hasUniqueRarity() {
        return unique || ItemTypes.uniqueRarity(this);
    }

    public static void rejectRepeats(List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        List<PoolEntry> combinedPool = new ArrayList<>(prefixes);
        combinedPool.addAll(suffixes);
        for (int index = 0; index < combinedPool.size(); index++) {
            PoolEntry entry = combinedPool.get(index);
            for (PoolEntry laterEntry : combinedPool.subList(index + 1, combinedPool.size())) {
                if (laterEntry.modifier().id().equals(entry.modifier().id())) {
                    throw new IllegalArgumentException(entry.modifier().id() + " is listed more than once");
                }
                if (entry.isSingle() && laterEntry.isSingle() && entry.effectName().equals(laterEntry.effectName())) {
                    throw new IllegalArgumentException(entry.effectName() + " is listed more than once, as "
                            + entry.modifier().id() + " and " + laterEntry.modifier().id());
                }
            }
        }
    }

    public String noun() {
        if (kind != null) {
            return kind.noun();
        }
        return type == null ? Translation.text("socketable.unknown") : Translation.text("socketable.noun." + type.id());
    }

    public List<PoolEntry> pool() {
        List<PoolEntry> combinedPool = new ArrayList<>(prefixes);
        combinedPool.addAll(suffixes);
        return combinedPool;
    }

    public PoolEntry poolEntryFor(String modifierId) {
        for (List<PoolEntry> side : List.of(prefixes, suffixes)) {
            for (PoolEntry entry : side) {
                if (entry.modifier().id().equals(modifierId)) {
                    return entry;
                }
            }
        }
        return null;
    }

    PoolEntry poolEntryFor(String modifierId, ModifierSlot slot) {
        PoolEntry listedEntry = poolEntryFor(modifierId);
        return listedEntry != null ? listedEntry : singleEntryFor(modifierId, slot);
    }

    private PoolEntry singleEntryFor(String effectName, ModifierSlot slot) {
        for (PoolEntry entry : side(slot)) {
            if (entry.isSingle() && entry.effectName().equals(effectName)) {
                return entry;
            }
        }
        return null;
    }

    public List<RolledModifier> withCurrentIds(List<RolledModifier> modifiers) {
        List<RolledModifier> resolved = null;
        for (int index = 0; index < modifiers.size(); index++) {
            RolledModifier modifier = modifiers.get(index);
            String currentId = currentId(modifier);
            if (!currentId.equals(modifier.modifierId())) {
                if (resolved == null) {
                    resolved = new ArrayList<>(modifiers);
                }
                resolved.set(index, new RolledModifier(currentId, modifier.slot(), modifier.lines(), modifier.locked()));
            }
        }
        return resolved == null ? modifiers : resolved;
    }

    public List<RolledModifier> withAddedFixedModifiers(List<RolledModifier> modifiers) {
        if (!unique || keepsOneModifier()) {
            return modifiers;
        }
        List<RolledModifier> added = addMissingFixed(prefixes, modifiers, null);
        added = addMissingFixed(suffixes, modifiers, added);
        return added == null ? modifiers : added;
    }

    private static List<RolledModifier> addMissingFixed(List<PoolEntry> entries, List<RolledModifier> modifiers, List<RolledModifier> added) {
        if (entries == null) {
            return added;
        }
        for (PoolEntry entry : entries) {
            String modifierId = entry.modifier().id();
            boolean fixed = entry.modifier().lines().stream().noneMatch(ModifierLine::canVary);
            if (fixed && modifiers.stream().noneMatch(modifier -> modifier.modifierId().equals(modifierId))) {
                if (added == null) {
                    added = new ArrayList<>(modifiers);
                }
                added.add(RolledModifier.roll(entry.modifier(), new Random(0L), true));
            }
        }
        return added;
    }

    private String currentId(RolledModifier modifier) {
        if (poolEntryFor(modifier.modifierId()) != null || modifier.lines().size() != 1) {
            return modifier.modifierId();
        }
        PoolEntry entry = singleEntryFor(modifier.lines().get(0).effectName(), modifier.slot());
        return entry == null ? modifier.modifierId() : entry.modifier().id();
    }

    private List<PoolEntry> side(ModifierSlot slot) {
        return switch (slot) {
            case PREFIX -> prefixes;
            case SUFFIX -> suffixes;
            case IMPLICIT -> List.of();
        };
    }

    public PoolEntry poolEntry(String effectName) {
        PoolEntry hybridEntry = null;
        for (List<PoolEntry> side : List.of(prefixes, suffixes)) {
            for (PoolEntry entry : side) {
                if (entry.isSingle() && entry.effectName().equals(effectName)) {
                    return entry;
                }
                if (hybridEntry == null && entry.hasLine(effectName)) {
                    hybridEntry = entry;
                }
            }
        }
        return hybridEntry;
    }

    public boolean isPrefix(String effectName) {
        return prefixes.stream().anyMatch(entry -> entry.hasLine(effectName));
    }

    public boolean isSuffix(String effectName) {
        return suffixes.stream().anyMatch(entry -> entry.hasLine(effectName));
    }

    private static String nameOrId(String name, String id) {
        return name.isEmpty() ? id : name;
    }

    public static List<PoolEntry> parsePool(String poolText, ModifierSlot slot) {
        return parsePool(null, poolText, slot, (ModifierScope) null);
    }

    private static List<PoolEntry> parsePool(String definitionId, String poolText, ModifierSlot slot, ItemType itemType) {
        return parsePool(definitionId, poolText, slot, ModifierScope.of(itemType));
    }

    public static List<PoolEntry> parsePool(String definitionId, String poolText, ModifierSlot slot, ModifierScope scope) {
        List<PoolEntry> pool = new ArrayList<>();
        for (String entryText : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            PoolEntry entry = parseEntry(definitionId, trimmed, slot, scope);
            if (entry != null) {
                pool.add(entry);
            }
        }
        return List.copyOf(pool);
    }

    private static PoolEntry parseEntry(String definitionId, String entryText, ModifierSlot slot, ModifierScope scope) {
        if (!ItemModifiers.isReference(entryText)) {
            return PoolEntry.parse(entryText, slot, scope);
        }
        try {
            return PoolEntry.parse(entryText, slot, scope);
        } catch (IllegalArgumentException e) {
            LOG.error("Skipping pool entry " + entryText + (definitionId == null ? "" : " of " + definitionId) + ": " + e.getMessage());
            return null;
        }
    }

    static String currentEffectName(String listedName, ModifierEffects effects) {
        String currentName = effects.currentName(listedName);
        if (currentName == null) {
            LOG.warn("Skipping pool entry " + listedName + ": it is not a skill effect");
        }
        return currentName;
    }

    public String displayName() {
        return Translation.data("socketable." + id + ".name", name);
    }

    public StyledText descriptionText() {
        return description.isEmpty() ? null : Translation.dataStyled("socketable." + id + ".description", description);
    }
}
