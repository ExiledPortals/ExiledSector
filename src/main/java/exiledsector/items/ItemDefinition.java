package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.SkillEffect;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record ItemDefinition(String id, ItemKind kind, String name, String icon, float rarity, boolean unique,
                                   String description, List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock) {

    public static final String FALLBACK_ICON = "graphics/icons/cargo/chip1.png";
    private static final Logger LOG = Logger.getLogger(ItemDefinition.class);
    private static final String ENTRY_SEPARATOR = ";";
    static final String SECTION_COLUMN = "section";

    static ItemDefinition parseSubroutine(JSONObject row) {
        return parse(row, ItemKind.SUBROUTINE);
    }

    static ItemDefinition parseHullItem(JSONObject row) {
        String sectionId = row.optString("section", "").trim();
        ItemKind kind = ItemKind.byIdOrNull(sectionId);
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            throw new IllegalArgumentException("section \"" + sectionId + "\" is not a ship section");
        }
        return parse(row, kind);
    }

    static ItemDefinition parse(JSONObject row, ItemKind kind) {
        String definitionId = row.optString("id", "").trim();
        if (definitionId.isEmpty()) {
            throw new IllegalArgumentException("a row has no id");
        }
        String icon = row.optString("icon", "").trim();
        float rarity = (float) row.optDouble("rarity", 1.0);
        if (!(rarity >= 0f)) {
            throw new IllegalArgumentException("rarity must be zero or more");
        }
        List<PoolEntry> prefixes = parsePool(definitionId, row.optString("prefixes", ""), ModifierSlot.PREFIX, kind.type());
        List<PoolEntry> suffixes = parsePool(definitionId, row.optString("suffixes", ""), ModifierSlot.SUFFIX, kind.type());
        if (prefixes.isEmpty() && suffixes.isEmpty()) {
            throw new IllegalArgumentException("it has no prefixes or suffixes");
        }
        rejectRepeats(prefixes, suffixes);
        boolean unique = "true".equalsIgnoreCase(row.optString("unique", "").trim());
        return new ItemDefinition(definitionId, kind,
                nameOrId(row.optString("name", "").trim(), definitionId), icon.isEmpty() ? FALLBACK_ICON : icon, rarity, unique,
                row.optString("description", "").trim(), prefixes, suffixes,
                row.optString("unlock", "").trim());
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

    public ItemType type() {
        return kind == null ? null : kind.type();
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
        return parsePool(null, poolText, slot, null);
    }

    private static List<PoolEntry> parsePool(String definitionId, String poolText, ModifierSlot slot, ItemType itemType) {
        List<PoolEntry> pool = new ArrayList<>();
        for (String entryText : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            PoolEntry entry = parseEntry(definitionId, trimmed, slot, itemType);
            if (entry != null) {
                pool.add(entry);
            }
        }
        return List.copyOf(pool);
    }

    private static PoolEntry parseEntry(String definitionId, String entryText, ModifierSlot slot, ItemType itemType) {
        if (!ItemModifiers.isReference(entryText)) {
            return PoolEntry.parse(entryText, slot, itemType);
        }
        try {
            return PoolEntry.parse(entryText, slot, itemType);
        } catch (IllegalArgumentException e) {
            LOG.error("Skipping pool entry " + entryText + (definitionId == null ? "" : " of " + definitionId) + ": " + e.getMessage());
            return null;
        }
    }

    static String currentEffectName(String listedName) {
        try {
            return SkillEffect.byName(listedName).name();
        } catch (IllegalArgumentException e) {
            LOG.warn("Skipping pool entry " + listedName + ": it is not a skill effect");
            return null;
        }
    }

    public String displayName() {
        return Translation.data("socketable." + id + ".name", name);
    }

    public StyledText descriptionText() {
        return description.isEmpty() ? null : Translation.dataStyled("socketable." + id + ".description", description);
    }
}
