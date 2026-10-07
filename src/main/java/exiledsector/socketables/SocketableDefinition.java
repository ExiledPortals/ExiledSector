package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.SkillEffect;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record SocketableDefinition(String id, SocketableKind kind, String name, String icon, float rarity, boolean unique,
                                   String description, List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock) {

    public static final String FALLBACK_ICON = "graphics/icons/cargo/chip1.png";
    private static final Logger LOG = Logger.getLogger(SocketableDefinition.class);
    private static final String ENTRY_SEPARATOR = ";";

    static SocketableDefinition parse(JSONObject row) {
        String definitionId = row.optString("id", "").trim();
        if (definitionId.isEmpty()) {
            throw new IllegalArgumentException("a row has no id");
        }
        String icon = row.optString("icon", "").trim();
        float rarity = (float) row.optDouble("rarity", 1.0);
        if (!(rarity >= 0f)) {
            throw new IllegalArgumentException("rarity must be zero or more");
        }
        List<PoolEntry> prefixes = parsePool(row.optString("prefixes", ""));
        List<PoolEntry> suffixes = parsePool(row.optString("suffixes", ""));
        if (prefixes.isEmpty() && suffixes.isEmpty()) {
            throw new IllegalArgumentException("it has no prefixes or suffixes");
        }
        for (PoolEntry prefix : prefixes) {
            if (suffixes.stream().anyMatch(suffix -> suffix.effectName().equals(prefix.effectName()))) {
                throw new IllegalArgumentException(prefix.effectName() + " is listed as both a prefix and a suffix");
            }
        }
        boolean unique = "true".equalsIgnoreCase(row.optString("unique", "").trim());
        return new SocketableDefinition(definitionId, SocketableKind.byId(row.optString("kind", "").trim()),
                nameOrId(row.optString("name", "").trim(), definitionId), icon.isEmpty() ? FALLBACK_ICON : icon, rarity, unique,
                row.optString("description", "").trim(), prefixes, suffixes,
                row.optString("unlock", "").trim());
    }

    public List<PoolEntry> pool() {
        List<PoolEntry> combinedPool = new ArrayList<>(prefixes);
        combinedPool.addAll(suffixes);
        return combinedPool;
    }

    public PoolEntry poolEntry(String effectName) {
        for (PoolEntry entry : prefixes) {
            if (entry.effectName().equals(effectName)) {
                return entry;
            }
        }
        for (PoolEntry entry : suffixes) {
            if (entry.effectName().equals(effectName)) {
                return entry;
            }
        }
        return null;
    }

    public boolean isPrefix(String effectName) {
        return prefixes.stream().anyMatch(entry -> entry.effectName().equals(effectName));
    }

    public boolean isSuffix(String effectName) {
        return suffixes.stream().anyMatch(entry -> entry.effectName().equals(effectName));
    }

    private static String nameOrId(String name, String id) {
        return name.isEmpty() ? id : name;
    }

    private static List<PoolEntry> parsePool(String poolText) {
        List<PoolEntry> pool = new ArrayList<>();
        for (String entryText : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            PoolEntry listedEntry = PoolEntry.parse(trimmed);
            String effectName = currentEffectName(listedEntry.effectName());
            if (effectName == null) {
                continue;
            }
            if (pool.stream().anyMatch(existing -> existing.effectName().equals(effectName))) {
                throw new IllegalArgumentException(effectName + " is listed more than once");
            }
            pool.add(listedEntry.named(effectName));
        }
        return List.copyOf(pool);
    }

    private static String currentEffectName(String listedName) {
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
