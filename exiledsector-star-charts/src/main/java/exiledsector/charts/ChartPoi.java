package exiledsector.charts;

import java.util.Locale;

public record ChartPoi(String id, Kind kind, String entityType, String salvageSpec, String derelictType, float cost, float weight, int minTier,
                       int maxTier, boolean guarded, String requiredMod) {

    public enum Kind {
        CACHE,
        DOMAIN,
        STATION,
        DERELICT;

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
        requiredMod = requiredMod == null ? "" : requiredMod;
        cost = Math.max(0f, cost);
        weight = Math.max(0f, weight);
    }

    public boolean allowedAt(int tier) {
        return tier >= minTier && tier <= maxTier;
    }

    public String specForSalvage() {
        return salvageSpec.isEmpty() ? entityType : salvageSpec;
    }
}
