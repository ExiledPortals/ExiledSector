package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.SkillEffect;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record SocketableDefinition(String id, SocketableKind kind, String name, String icon, float rarity, boolean unique,
                                   String description, List<PoolEntry> prefixes, List<PoolEntry> suffixes, String unlock) {

    public static final String FALLBACK_ICON = "graphics/icons/cargo/chip1.png";
    private static final String ENTRY_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = ":";

    public record PoolEntry(String effectName, float min, float max, float weight) {
    }

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

    public PoolEntry rollRange(String effectName) {
        PoolEntry combinedRange = null;
        for (PoolEntry entry : pool()) {
            if (!entry.effectName().equals(effectName)) {
                continue;
            }
            combinedRange = combinedRange == null ? entry : new PoolEntry(effectName, Math.min(combinedRange.min(), entry.min()),
                    Math.max(combinedRange.max(), entry.max()), combinedRange.weight());
        }
        return combinedRange;
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
        for (String entry : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] entryFields = trimmed.split(FIELD_SEPARATOR);
            if (entryFields.length < 3 || entryFields.length > 4) {
                throw new IllegalArgumentException("entry \"" + trimmed + "\" is not EFFECT:min:max or EFFECT:min:max:weight");
            }
            String effectName = entryFields[0].trim();
            SkillEffect.byName(effectName);
            if (pool.stream().anyMatch(existing -> existing.effectName().equals(effectName))) {
                throw new IllegalArgumentException("" + effectName + " is listed more than once");
            }
            float firstBound = parseNumber(entryFields[1], trimmed);
            float secondBound = parseNumber(entryFields[2], trimmed);
            float weight = entryFields.length == 4 ? parseNumber(entryFields[3], trimmed) : 1f;
            if (!(weight > 0f)) {
                throw new IllegalArgumentException("entry \"" + trimmed + "\" needs a weight above zero");
            }
            pool.add(new PoolEntry(effectName, Math.min(firstBound, secondBound), Math.max(firstBound, secondBound), weight));
        }
        return List.copyOf(pool);
    }

    private static float parseNumber(String numberText, String entry) {
        try {
            return Float.parseFloat(numberText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("entry \"" + entry + "\" has a value that is not a number: " + numberText.trim());
        }
    }

    public String displayName() {
        return Translation.data("socketable." + id + ".name", name);
    }

    public StyledText descriptionText() {
        return description.isEmpty() ? null : Translation.dataStyled("socketable." + id + ".description", description);
    }
}
