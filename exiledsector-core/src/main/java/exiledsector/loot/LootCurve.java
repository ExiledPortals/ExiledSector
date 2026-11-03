package exiledsector.loot;

import java.util.Locale;

public enum LootCurve {

    LINEAR,
    DIMINISHING;

    public static final float KNEE_DEPLOYMENT_POINTS = 2000f;

    public float apply(float deploymentPoints) {
        if (!(deploymentPoints > 0f)) {
            return 0f;
        }
        if (this == LINEAR || deploymentPoints <= KNEE_DEPLOYMENT_POINTS) {
            return deploymentPoints;
        }
        return KNEE_DEPLOYMENT_POINTS * (2f * (float) Math.sqrt(deploymentPoints / KNEE_DEPLOYMENT_POINTS) - 1f);
    }

    public float factor(float deploymentPoints) {
        return deploymentPoints > 0f ? apply(deploymentPoints) / deploymentPoints : 0f;
    }

    static LootCurve byIdOrNull(String id) {
        for (LootCurve curve : values()) {
            if (curve.name().toLowerCase(Locale.ROOT).equals(id)) {
                return curve;
            }
        }
        return null;
    }
}
