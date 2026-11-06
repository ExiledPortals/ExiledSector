package exiledsector.compat;

import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class RuthlessSectorCompat {

    public static final String MOD_ID = "sun_ruthless_sector";
    public static final List<String> TESTED_VERSIONS = List.of("1.7.0");
    public static final CompatTarget TARGET = new CompatTarget("Ruthless Sector", List.of(MOD_ID), TESTED_VERSIONS, RuthlessSectorCompat::missingFeatures);
    static final String VICTORY_REPUTATION_SETTING = "sun_rs_gainReputationForImpressiveVictories";
    static final ApiMember SET_VICTORY_REPUTATION = ApiMember.staticSetter("ruthless_sector.ModPlugin", "GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES",
            ApiMember.BOOLEAN);
    private static final OptionalApi VICTORY_REPUTATION_API = new OptionalApi(List.of(SET_VICTORY_REPUTATION), Logger.getLogger(RuthlessSectorCompat.class),
            Level.WARN, "Switching off Ruthless Sector's impressive victory reputation failed; hard Star Chart battles may still change reputation",
            OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);
    private static boolean victoryReputationBlocked;

    private RuthlessSectorCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static void matchVictoryReputationTo(boolean playerInChart) {
        if (!isModEnabled() || !playerInChart && !victoryReputationBlocked) {
            return;
        }
        boolean gainReputation = !playerInChart && playerAllowsVictoryReputation();
        if (setVictoryReputation(gainReputation)) {
            victoryReputationBlocked = playerInChart;
        }
    }

    private static boolean setVictoryReputation(boolean gainReputation) {
        return VICTORY_REPUTATION_API.call(api -> {
            api.handle(SET_VICTORY_REPUTATION).invoke(gainReputation);
            return true;
        }, false);
    }

    private static boolean playerAllowsVictoryReputation() {
        try {
            return !Boolean.FALSE.equals(LunaSettings.getBoolean(MOD_ID, VICTORY_REPUTATION_SETTING));
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    static void resetForTests() {
        victoryReputationBlocked = false;
        VICTORY_REPUTATION_API.reset();
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (!VICTORY_REPUTATION_API.missingMembers().isEmpty()) {
            missing.add("ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES, switched off so hard Star Chart battles change no reputation");
        }
        return missing;
    }
}
