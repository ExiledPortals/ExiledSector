package exiledsector.charts;

import java.util.List;
import java.util.Locale;

public record ChartPoi(String id, Kind kind, String entityType, String salvageSpec, String derelictType, float cost, float weight, int minTier,
                       int maxTier, boolean guarded, String requiredMod, List<String> planetCategories) {

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
        planetCategories = planetCategories == null ? List.of() : List.copyOf(planetCategories);
        cost = Math.max(0f, cost);
        weight = Math.max(0f, weight);
    }

    public ChartPoi(String id, Kind kind, String entityType, String salvageSpec, String derelictType, float cost, float weight, int minTier,
                    int maxTier, boolean guarded, String requiredMod) {
        this(id, kind, entityType, salvageSpec, derelictType, cost, weight, minTier, maxTier, guarded, requiredMod, List.of());
    }

    public boolean allowedAt(int tier) {
        return tier >= minTier && tier <= maxTier;
    }

    public String specForSalvage() {
        return salvageSpec.isEmpty() ? entityType : salvageSpec;
    }
}
