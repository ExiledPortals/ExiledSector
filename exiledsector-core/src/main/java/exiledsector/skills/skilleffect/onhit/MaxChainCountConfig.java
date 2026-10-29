package exiledsector.skills.skilleffect.onhit;

import exiledsector.ExiledSector;
import exiledsector.ModSettings;

public final class MaxChainCountConfig {

    public static final int DEFAULT = 5;

    private MaxChainCountConfig() {
    }

    public static String fieldId() {
        return ExiledSector.key("maxChainCount");
    }

    public static int get() {
        return ModSettings.intOr(fieldId(), DEFAULT);
    }
}
