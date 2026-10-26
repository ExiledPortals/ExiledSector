package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarAge;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class ChartDeepSpace {

    public static final Color AMBIENT_LIGHT = new Color(100, 100, 100);
    static final String CENTRE_DATA_PREFIX = "nebula_center_";
    static final String DEEP_SPACE_BARREN_DESCRIPTION = "barren_deep_space";
    static final String BARREN_DESCRIPTION = "barren";
    static final float PLANETOID_WEIGHT = 100f;
    static final float GIANT_WEIGHT = 20f;
    static final Map<String, Float> PLANETOIDS = ordered(
            "cryovolcanic", 5f, "frozen", 2.5f, "frozen1", 2.5f, "frozen2", 2.5f, "frozen3", 2.5f, "barren", 2.5f, "barren2", 2.5f,
            "barren3", 2.5f, "barren_castiron", 2.5f, "barren-bombarded", 5f, "rocky_metallic", 5f, "rocky_ice", 5f);
    static final Map<String, Float> GIANTS = ordered("ice_giant", 10f, "gas_giant", 5f);

    private ChartDeepSpace() {
    }

    public static String pickCentre(Random random) {
        boolean giant = random.nextFloat() * (PLANETOID_WEIGHT + GIANT_WEIGHT) >= PLANETOID_WEIGHT;
        return pick(giant ? GIANTS : PLANETOIDS, random);
    }

    static String centreDataId(StarAge age) {
        return CENTRE_DATA_PREFIX + switch (age) {
            case YOUNG -> "young";
            case AVERAGE -> "average";
            default -> "old";
        };
    }

    private static String pick(Map<String, Float> weights, Random random) {
        float total = 0f;
        for (float weight : weights.values()) {
            total += weight;
        }
        float roll = random.nextFloat() * total;
        String last = null;
        for (Map.Entry<String, Float> entry : weights.entrySet()) {
            last = entry.getKey();
            roll -= entry.getValue();
            if (roll < 0f) {
                return last;
            }
        }
        return last;
    }

    private static Map<String, Float> ordered(Object... typesAndWeights) {
        Map<String, Float> weights = new LinkedHashMap<>();
        for (int i = 0; i < typesAndWeights.length; i += 2) {
            weights.put((String) typesAndWeights[i], (Float) typesAndWeights[i + 1]);
        }
        return Collections.unmodifiableMap(weights);
    }
}
