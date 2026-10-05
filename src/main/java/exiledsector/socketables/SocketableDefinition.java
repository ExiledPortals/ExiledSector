package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.SkillEffect;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record SocketableDefinition(String id, SocketableKind kind, String name, String icon, String grade, String alignment,
                                   float rarity, boolean unique, String description, List<PoolEntry> prefixes,
                                   List<PoolEntry> suffixes, String unlock) {

    public static final String FALLBACK_ICON = "graphics/icons/cargo/chip1.png";
    private static final String ENTRY_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = ":";

    public record PoolEntry(String effectName, float min, float max, float weight) {
    }

    static SocketableDefinition parse(JSONObject row) {
        String id = row.optString("id", "").trim();
        if (id.isEmpty()) {
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
        return new SocketableDefinition(id, SocketableKind.byId(row.optString("kind", "").trim()),
                nameOrId(row.optString("name", "").trim(), id), icon.isEmpty() ? FALLBACK_ICON : icon,
                row.optString("grade", "").trim(), row.optString("alignment", "").trim(), rarity, unique,
                row.optString("description", "").trim(), prefixes, suffixes,
                row.optString("unlock", "").trim());
    }

    public List<PoolEntry> pool() {
        List<PoolEntry> pool = new ArrayList<>(prefixes);
        pool.addAll(suffixes);
        return pool;
    }

    public PoolEntry rollRange(String effectName) {
        PoolEntry range = null;
        for (PoolEntry entry : pool()) {
            if (!entry.effectName().equals(effectName)) {
                continue;
            }
            range = range == null ? entry : new PoolEntry(effectName, Math.min(range.min(), entry.min()),
                    Math.max(range.max(), entry.max()), range.weight());
        }
        return range;
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

    private static List<PoolEntry> parsePool(String text) {
        List<PoolEntry> pool = new ArrayList<>();
        for (String entry : text.split(ENTRY_SEPARATOR)) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] fields = trimmed.split(FIELD_SEPARATOR);
            if (fields.length < 3 || fields.length > 4) {
                throw new IllegalArgumentException("entry \"" + trimmed + "\" is not EFFECT:min:max or EFFECT:min:max:weight");
            }
            String effectName = fields[0].trim();
            SkillEffect.byName(effectName);
            if (pool.stream().anyMatch(existing -> existing.effectName().equals(effectName))) {
                throw new IllegalArgumentException("" + effectName + " is listed more than once");
            }
            float first = parseNumber(fields[1], trimmed);
            float second = parseNumber(fields[2], trimmed);
            float weight = fields.length == 4 ? parseNumber(fields[3], trimmed) : 1f;
            if (!(weight > 0f)) {
                throw new IllegalArgumentException("entry \"" + trimmed + "\" needs a weight above zero");
            }
            pool.add(new PoolEntry(effectName, Math.min(first, second), Math.max(first, second), weight));
        }
        return List.copyOf(pool);
    }

    private static float parseNumber(String text, String entry) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("entry \"" + entry + "\" has a value that is not a number: " + text.trim());
        }
    }

    public String displayName() {
        return Translation.data("socketable." + id + ".name", name);
    }

    public String gradeName() {
        return gradeName(grade);
    }

    public String alignmentName() {
        return alignmentName(alignment);
    }

    public static String gradeName(String grade) {
        return Translation.data("socketable.grade." + grade, grade);
    }

    public static String alignmentName(String alignment) {
        return Translation.data("socketable.alignment." + alignment, alignment);
    }

    public StyledText descriptionText() {
        return description.isEmpty() ? null : Translation.dataStyled("socketable." + id + ".description", description);
    }
}
