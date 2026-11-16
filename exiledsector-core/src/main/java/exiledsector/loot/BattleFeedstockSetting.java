package exiledsector.loot;

import exiledsector.ModSettings;

public final class BattleFeedstockSetting {

    public static final String FIELD_ID = "exiledSector_socketPartsPer100DeploymentPoints";
    public static final int DEFAULT_FEEDSTOCK_PER_100_DEPLOYMENT_POINTS = 5;
    public static final int MAX_FEEDSTOCK_PER_100_DEPLOYMENT_POINTS = 200;
    private static final float DEPLOYMENT_POINTS_PER_SETTING_STEP = 100f;

    private BattleFeedstockSetting() {
    }

    public static float feedstockPerDeploymentPoint() {
        return Math.max(0, ModSettings.intOr(FIELD_ID, DEFAULT_FEEDSTOCK_PER_100_DEPLOYMENT_POINTS)) / DEPLOYMENT_POINTS_PER_SETTING_STEP;
    }
}
