package exiledsector.charts.content;

import exiledsector.data.EnumKeys;
import java.util.List;

public record ChartPoi(String id, Kind kind, String entityType, String salvageSpec, String derelictType, float cost, float weight, int minTier,
                       int maxTier, boolean guarded, String requiredMod, List<String> planetCategories) {

    public enum Kind {
        CACHE,
        DOMAIN,
        STATION,
        DERELICT;

        static Kind parse(String text) {
            return EnumKeys.parse(Kind.class, text);
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
