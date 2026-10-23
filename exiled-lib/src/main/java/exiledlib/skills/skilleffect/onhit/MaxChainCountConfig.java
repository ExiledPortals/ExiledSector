package exiledlib.skills.skilleffect.onhit;

import exiledlib.HostMod;
import exiledlib.ModSettings;

public final class MaxChainCountConfig {

    public static final int DEFAULT = 5;

    private MaxChainCountConfig() {
    }

    public static String fieldId() {
        return HostMod.settingId("maxChainCount");
    }

    public static int get() {
        return ModSettings.intOr(fieldId(), DEFAULT);
    }
}
