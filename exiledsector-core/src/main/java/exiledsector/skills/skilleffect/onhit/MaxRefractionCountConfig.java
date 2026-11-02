package exiledsector.skills.skilleffect.onhit;

import exiledsector.ExiledSector;
import exiledsector.ModSettings;

public final class MaxRefractionCountConfig {

    public static final int DEFAULT = 5;

    private MaxRefractionCountConfig() {
    }

    public static String fieldId() {
        return ExiledSector.key("maxChainCount");
    }

    public static int get() {
        return ModSettings.intOr(fieldId(), DEFAULT);
    }
}
