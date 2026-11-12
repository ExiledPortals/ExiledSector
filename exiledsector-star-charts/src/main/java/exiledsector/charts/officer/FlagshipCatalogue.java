package exiledsector.charts.officer;

import java.util.Random;

@FunctionalInterface
public interface FlagshipCatalogue {

    FlagshipCatalogue NONE = (factionId, rankOrder, rankCount, random) -> null;

    record Flagship(String hullId, String variantId) {
    }

    Flagship pick(String factionId, int rankOrder, int rankCount, Random random);
}
