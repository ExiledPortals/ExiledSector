package exiledsector.compat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;

public final class NexerelinCompat {

    public static final String MOD_ID = "nexerelin";
    public static final List<String> TESTED_VERSIONS = List.of("0.12.2d");
    public static final CompatTarget TARGET = new CompatTarget("Nexerelin", List.of(MOD_ID), TESTED_VERSIONS, NexerelinCompat::missingFeatures);

    private static final String ALLIANCE_MANAGER_CLASS = "exerelin.campaign.AllianceManager";
    private static final String STRING_CLASS = "java.lang.String";

    static final ApiMember ARE_FACTIONS_ALLIED = ApiMember.staticMethod(ALLIANCE_MANAGER_CLASS, "areFactionsAllied", ApiMember.BOOLEAN,
            STRING_CLASS, STRING_CLASS, ApiMember.BOOLEAN);
    private static final OptionalApi ALLIANCE_API = new OptionalApi(List.of(ARE_FACTIONS_ALLIED), Logger.getLogger(NexerelinCompat.class),
            Level.WARN, "Nexerelin alliance lookup failed; faction fleets on the skill tree stay in their own volumes",
            OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);

    private NexerelinCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static boolean areAllied(String factionId, String otherFactionId) {
        if (factionId == null || otherFactionId == null || factionId.equals(otherFactionId) || !isModEnabled()) {
            return false;
        }
        return ALLIANCE_API.call(api -> (boolean) api.handle(ARE_FACTIONS_ALLIED).invoke(factionId, otherFactionId, false), false);
    }

    static List<String> missingFeatures() {
        return ALLIANCE_API.missingMembers().isEmpty() ? List.of() : List.of("AllianceManager.areFactionsAllied, used to send faction fleets to allied volumes");
    }
}
