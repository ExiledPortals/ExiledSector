package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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
    private static final String FIELD_SEPARATOR = ":";
    private static final String HULL_VALUE_SEPARATOR = "/";
    private static final List<HullSize> HULL_VALUE_ORDER = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    public record PoolEntry(String effectName, float min, float max, float weight, List<Float> hullValues) {

        public PoolEntry {
            hullValues = hullValues == null ? List.of() : List.copyOf(hullValues);
        }

        public PoolEntry(String effectName, float min, float max, float weight) {
            this(effectName, min, max, weight, List.of());
        }

        public static PoolEntry perHullSize(String effectName, float weight, List<Float> hullValues) {
            return new PoolEntry(effectName, hullValues.get(0), hullValues.get(0), weight, hullValues);
        }

        public boolean scalesWithHullSize() {
            return !hullValues.isEmpty();
        }

        public boolean hasHullValueFor(HullSize hullSize) {
            return scalesWithHullSize() && hullSize != null && HULL_VALUE_ORDER.contains(hullSize);
        }

        public PoolEntry rangeFor(HullSize hullSize) {
            if (!hasHullValueFor(hullSize)) {
                return this;
            }
            float hullValue = hullValues.get(HULL_VALUE_ORDER.indexOf(hullSize));
            return new PoolEntry(effectName, hullValue, hullValue, weight);
        }

        public float magnitudeFor(float rolledMagnitude, HullSize hullSize) {
            return hasHullValueFor(hullSize) ? hullValues.get(HULL_VALUE_ORDER.indexOf(hullSize)) : rolledMagnitude;
        }
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
                    Math.max(combinedRange.max(), entry.max()), combinedRange.weight(), combinedRange.hullValues());
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
            boolean perHullSize = entryFields.length >= 2 && entryFields[1].contains(HULL_VALUE_SEPARATOR);
            if (perHullSize ? entryFields.length > 3 : entryFields.length < 3 || entryFields.length > 4) {
                throw new IllegalArgumentException("entry \"" + trimmed + "\" is not EFFECT:min:max, EFFECT:min:max:weight, "
                        + "EFFECT:frigate/destroyer/cruiser/capital or EFFECT:frigate/destroyer/cruiser/capital:weight");
            }
            String effectName = currentEffectName(entryFields[0].trim());
            if (effectName == null) {
                continue;
            }
            if (pool.stream().anyMatch(existing -> existing.effectName().equals(effectName))) {
                throw new IllegalArgumentException("" + effectName + " is listed more than once");
            }
            int weightField = perHullSize ? 2 : 3;
            float weight = entryFields.length > weightField ? parseNumber(entryFields[weightField], trimmed) : 1f;
            if (!(weight > 0f)) {
                throw new IllegalArgumentException("entry \"" + trimmed + "\" needs a weight above zero");
            }
            if (perHullSize) {
                pool.add(PoolEntry.perHullSize(effectName, weight, parseHullValues(entryFields[1], trimmed)));
            } else {
                float firstBound = parseNumber(entryFields[1], trimmed);
                float secondBound = parseNumber(entryFields[2], trimmed);
                pool.add(new PoolEntry(effectName, Math.min(firstBound, secondBound), Math.max(firstBound, secondBound), weight));
            }
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

    private static List<Float> parseHullValues(String hullValuesText, String entry) {
        String[] valueTexts = hullValuesText.split(HULL_VALUE_SEPARATOR, -1);
        if (valueTexts.length != HULL_VALUE_ORDER.size()) {
            throw new IllegalArgumentException("entry \"" + entry + "\" needs exactly four hull size values, frigate/destroyer/cruiser/capital");
        }
        List<Float> hullValues = new ArrayList<>(valueTexts.length);
        for (String valueText : valueTexts) {
            hullValues.add(parseNumber(valueText, entry));
        }
        return hullValues;
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
