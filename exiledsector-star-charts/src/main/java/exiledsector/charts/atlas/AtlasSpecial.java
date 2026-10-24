package exiledsector.charts.atlas;

import java.util.Locale;

public record AtlasSpecial(String id, Kind kind, Placement placement, float chance, float minSteps, float radius, int count, float steps,
                           String requiredMod, String unlock, String factionId) {

    public static final float MOST_RADIUS = AtlasCell.CHUNK_SIZE - 1f;

    public enum Placement { CHUNK, FIXED }

    public enum Kind {
        INFESTATION, SENSOR_ARRAY, BASTION, UNIQUE;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kind parse(String text) {
            if (text == null || text.isBlank()) {
                return UNIQUE;
            }
            for (Kind kind : values()) {
                if (kind.key().equalsIgnoreCase(text.trim())) {
                    return kind;
                }
            }
            return null;
        }
    }

    public AtlasSpecial {
        kind = kind == null ? Kind.UNIQUE : kind;
        chance = Math.max(0f, Math.min(1f, chance));
        minSteps = Math.max(1f, minSteps);
        radius = Math.max(0f, Math.min(MOST_RADIUS, radius));
        count = Math.max(1, count);
        steps = Math.max(1f, steps);
        requiredMod = requiredMod == null ? "" : requiredMod;
        unlock = unlock == null ? "" : unlock;
        factionId = factionId == null ? "" : factionId;
    }

    public static boolean ofKind(AtlasSpecial special, Kind kind) {
        return special != null && special.kind == kind;
    }

    public boolean infests() {
        return kind == Kind.INFESTATION;
    }
}
