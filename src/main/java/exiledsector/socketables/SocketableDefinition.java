package exiledsector.socketables;

import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.SkillEffect;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record SocketableDefinition(String id, SocketableKind kind, String name, String icon, String grade, String alignment,
                                   float rarity, List<PoolEntry> pool) {

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
        List<PoolEntry> pool = parsePool(row.optString("pool", ""));
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("the effect pool is empty");
        }
        return new SocketableDefinition(id, SocketableKind.byId(row.optString("kind", "").trim()), row.optString("name", id).trim(),
                icon.isEmpty() ? FALLBACK_ICON : icon, row.optString("grade", "").trim(), row.optString("alignment", "").trim(),
                rarity, pool);
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
                throw new IllegalArgumentException("pool entry \"" + trimmed + "\" is not EFFECT:min:max or EFFECT:min:max:weight");
            }
            String effectName = fields[0].trim();
            SkillEffect.byName(effectName);
            float first = parseNumber(fields[1], trimmed);
            float second = parseNumber(fields[2], trimmed);
            float weight = fields.length == 4 ? parseNumber(fields[3], trimmed) : 1f;
            if (!(weight > 0f)) {
                throw new IllegalArgumentException("pool entry \"" + trimmed + "\" needs a weight above zero");
            }
            pool.add(new PoolEntry(effectName, Math.min(first, second), Math.max(first, second), weight));
        }
        return List.copyOf(pool);
    }

    private static float parseNumber(String text, String entry) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("pool entry \"" + entry + "\" has a value that is not a number: " + text.trim());
        }
    }

    public String displayName() {
        return Translation.data("socketable." + id + ".name", name);
    }

    public String gradeName() {
        return Translation.data("socketable.grade." + grade, grade);
    }

    public String alignmentName() {
        return Translation.data("socketable.alignment." + alignment, alignment);
    }
}
