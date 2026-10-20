package exiledsector.items;

import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public record ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique,
                                   String description, List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock, ItemType type,
                                   SubroutineKind subroutineKind, List<UniqueSource> uniqueSources, int socketLimit) {

    public static final String FALLBACK_ICON = "graphics/icons/cargo/chip1.png";
    public static final int NO_SOCKET_LIMIT = 0;
    private static final Logger LOG = Logger.getLogger(ItemDefinition.class);
    private static final String ENTRY_SEPARATOR = ";";
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");
    static final String SECTION_COLUMN = "section";
    static final String SUBROUTINE_KIND_COLUMN = "kind";
    static final String UNIQUE_LINES_COLUMN = "lines";
    static final String UNIQUE_SOURCE_COLUMN = "source";
    static final String SOCKET_LIMIT_COLUMN = "socket_limit";

    public ItemDefinition {
        if (kind != null) {
            type = kind.type();
        }
        if (type != ItemType.SUBROUTINE) {
            subroutineKind = null;
        }
        uniqueSources = uniqueSources == null ? List.of() : List.copyOf(uniqueSources);
        socketLimit = Math.max(NO_SOCKET_LIMIT, socketLimit);
    }

    public ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique, String description,
                          List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock) {
        this(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, null, null, List.of(), NO_SOCKET_LIMIT);
    }

    public boolean limitsSocketsPerShip() {
        return socketLimit > NO_SOCKET_LIMIT;
    }

    static Integer parsedSocketLimit(String socketLimitText) {
        String trimmed = socketLimitText == null ? "" : socketLimitText.trim();
        if (trimmed.isEmpty()) {
            return NO_SOCKET_LIMIT;
        }
        return WHOLE_NUMBER.matcher(trimmed).matches() ? Integer.valueOf(trimmed) : null;
    }

    private ItemDefinition withSocketLimit(int limit) {
        return new ItemDefinition(id, kind, name, icon, rarity, unique, description, prefixes, suffixes, unlock, type, subroutineKind,
                uniqueSources, limit);
    }

    static ItemDefinition parseSubroutine(JSONObject row) {
        String definitionId = definitionId(row);
        String kindText = row.optString(SUBROUTINE_KIND_COLUMN, "").trim();
        SubroutineKind subroutineKind = SubroutineKind.byIdOrNull(kindText);
        if (subroutineKind == null) {
            throw new IllegalArgumentException(definitionId + " has kind \"" + kindText + "\"; expected one of " + SubroutineKind.ids());
        }
        String linesText = row.optString(UNIQUE_LINES_COLUMN, "").trim();
        List<PoolEntry> uniqueLines = List.of();
        if (subroutineKind == SubroutineKind.UNIQUE) {
            if (!"true".equalsIgnoreCase(row.optString("unique", "").trim())) {
                throw new IllegalArgumentException(definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine, so unique must be true");
            }
            uniqueLines = parsePool(definitionId, linesText, ModifierSlot.PREFIX, ModifierScope.UNIQUE_SUBROUTINE);
            rejectRepeats(uniqueLines, List.of());
        } else if (!linesText.isEmpty()) {
            throw new IllegalArgumentException(definitionId + " lists lines, but only " + SubroutineKind.UNIQUE.id() + " subroutines have them");
        }
        List<UniqueSource> sources = UniqueSource.parseList(row.optString(UNIQUE_SOURCE_COLUMN, ""));
        String socketLimitText = row.optString(SOCKET_LIMIT_COLUMN, "");
        Integer socketLimit = parsedSocketLimit(socketLimitText);
        if (socketLimit == null) {
            throw new IllegalArgumentException(definitionId + " has socket limit \"" + socketLimitText.trim()
                    + "\"; expected a whole number of 0 or more, or blank for no limit");
        }
        return parseShared(row, ItemKind.SUBROUTINE, ItemType.SUBROUTINE, new Pools(uniqueLines, List.of()), subroutineKind, sources)
                .withSocketLimit(socketLimit);
    }

    static ItemDefinition parseHullItem(JSONObject row) {
        String sectionId = row.optString("section", "").trim();
        ItemKind kind = ItemKind.byIdOrNull(sectionId);
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            throw new IllegalArgumentException("section \"" + sectionId + "\" is not a ship section");
        }
        return parse(row, kind);
    }

    static ItemDefinition parseStarChart(JSONObject row) {
        return parse(row, null, ItemType.STAR_CHART);
    }

    static ItemDefinition parseWormholeAnchor(JSONObject row) {
        return parseCommon(row, null, ItemType.WORMHOLE_ANCHOR, new Pools(List.of(), List.of()), null, false, List.of());
    }

    static ItemDefinition parse(JSONObject row, ItemKind kind) {
        return parse(row, kind, kind.type());
    }

    static ItemDefinition parse(JSONObject row, ItemKind kind, ItemType itemType) {
        if (itemType == ItemType.SUBROUTINE) {
            return parseSubroutine(row);
        }
        if (itemType == ItemType.WORMHOLE_ANCHOR) {
            return parseWormholeAnchor(row);
        }
        String definitionId = definitionId(row);
        List<PoolEntry> prefixes = parsePool(definitionId, row.optString("prefixes", ""), ModifierSlot.PREFIX, itemType);
        List<PoolEntry> suffixes = parsePool(definitionId, row.optString("suffixes", ""), ModifierSlot.SUFFIX, itemType);
        if (prefixes.isEmpty() && suffixes.isEmpty()) {
            throw new IllegalArgumentException("it has no prefixes or suffixes");
        }
        rejectRepeats(prefixes, suffixes);
        return parseShared(row, kind, itemType, new Pools(prefixes, suffixes), null, List.of());
    }

    private record Pools(List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
    }

    private static String definitionId(JSONObject row) {
        String definitionId = row.optString("id", "").trim();
        if (definitionId.isEmpty()) {
            throw new IllegalArgumentException("a row has no id");
        }
        return definitionId;
    }

    private static ItemDefinition parseShared(JSONObject row, ItemKind kind, ItemType itemType, Pools pools, SubroutineKind subroutineKind,
                                              List<UniqueSource> uniqueSources) {
        boolean unique = "true".equalsIgnoreCase(row.optString("unique", "").trim());
        return parseCommon(row, kind, itemType, pools, subroutineKind, unique, uniqueSources);
    }

    private static ItemDefinition parseCommon(JSONObject row, ItemKind kind, ItemType itemType, Pools pools, SubroutineKind subroutineKind,
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
                row.optString("unlock", "").trim(), itemType, subroutineKind, uniqueSources, NO_SOCKET_LIMIT);
    }

    public boolean isUniqueSubroutine() {
        return subroutineKind == SubroutineKind.UNIQUE;
    }

    public boolean hasUniqueRarity() {
        return unique || type == ItemType.WORMHOLE_ANCHOR || subroutineKind != null && subroutineKind.uniqueRarity();
    }

    private static void rejectRepeats(List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
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

    List<RolledModifier> withCurrentIds(List<RolledModifier> modifiers) {
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

    static List<PoolEntry> parsePool(String poolText, ModifierSlot slot) {
        return parsePool(null, poolText, slot, (ModifierScope) null);
    }

    private static List<PoolEntry> parsePool(String definitionId, String poolText, ModifierSlot slot, ItemType itemType) {
        return parsePool(definitionId, poolText, slot, ModifierScope.of(itemType));
    }

    private static List<PoolEntry> parsePool(String definitionId, String poolText, ModifierSlot slot, ModifierScope scope) {
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
