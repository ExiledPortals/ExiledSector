package exiledsector.charts;

import java.util.Locale;

public record ChartPoi(String id, Kind kind, String entityType, String salvageSpec, String derelictType, float cost, float weight, int minTier,
                       int maxTier, boolean guarded) {

    public enum Kind {
        CACHE,
        STATION,
        DERELICT,
        VAULT;

        static Kind parse(String text) {
            if (text == null) {
                return null;
            }
            String wanted = text.trim().toUpperCase(Locale.ROOT);
            for (Kind kind : values()) {
                if (kind.name().equals(wanted)) {
                    return kind;
                }
            }
            return null;
        }
    }

    public ChartPoi {
        salvageSpec = salvageSpec == null ? "" : salvageSpec;
        derelictType = derelictType == null ? "" : derelictType;
        cost = Math.max(0f, cost);
        weight = Math.max(0f, weight);
        guarded = guarded || kind == Kind.VAULT;
    }

    public boolean allowedAt(int tier) {
        return tier >= minTier && tier <= maxTier;
    }

    public String specForSalvage() {
        return salvageSpec.isEmpty() ? entityType : salvageSpec;
    }

    public boolean isVault() {
        return kind == Kind.VAULT;
    }
}
