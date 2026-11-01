package exiledsector.compat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class NexerelinCompat {

    public static final String MOD_ID = "nexerelin";
    public static final List<String> TESTED_VERSIONS = List.of("0.12.2d");
    public static final CompatTarget TARGET = new CompatTarget("Nexerelin", List.of(MOD_ID), TESTED_VERSIONS, NexerelinCompat::missingFeatures);

    private static final String ALLIANCE_MANAGER_CLASS = "exerelin.campaign.AllianceManager";
    private static final String SECTOR_MANAGER_CLASS = "exerelin.campaign.SectorManager";
    private static final String NEX_CONFIG_CLASS = "exerelin.utilities.NexConfig";
    private static final String FACTION_CONFIG_CLASS = "exerelin.utilities.NexFactionConfig";
    private static final String STRING_CLASS = "java.lang.String";

    static final ApiMember ARE_FACTIONS_ALLIED = ApiMember.staticMethod(ALLIANCE_MANAGER_CLASS, "areFactionsAllied", ApiMember.BOOLEAN,
            STRING_CLASS, STRING_CLASS, ApiMember.BOOLEAN);
    static final ApiMember IS_FACTION_ALIVE = ApiMember.staticMethod(SECTOR_MANAGER_CLASS, "isFactionAlive", ApiMember.BOOLEAN, STRING_CLASS);
    static final ApiMember GET_FACTION_CONFIG = ApiMember.staticMethod(NEX_CONFIG_CLASS, "getFactionConfig", FACTION_CONFIG_CLASS,
            STRING_CLASS, ApiMember.BOOLEAN);
    static final ApiMember PLAYABLE_FACTION = ApiMember.instanceGetter(FACTION_CONFIG_CLASS, "playableFaction", ApiMember.BOOLEAN);
    private static final OptionalApi ALLIANCE_API = new OptionalApi(List.of(ARE_FACTIONS_ALLIED), Logger.getLogger(NexerelinCompat.class),
            Level.WARN, "Nexerelin alliance lookup failed; faction fleets on the skill tree stay in their own volumes",
            OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);
    private static final OptionalApi FACTION_API = new OptionalApi(List.of(IS_FACTION_ALIVE, GET_FACTION_CONFIG, PLAYABLE_FACTION),
            Logger.getLogger(NexerelinCompat.class), Level.WARN,
            "Nexerelin faction lookup failed; no faction counts as eliminated, so anchors keep their battle and market sources",
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

    public static boolean isPlayable(String factionId) {
        if (factionId == null || !isModEnabled()) {
            return false;
        }
        return FACTION_API.call(api -> {
            Object factionConfig = api.handle(GET_FACTION_CONFIG).invoke(factionId, false);
            return factionConfig != null && (boolean) api.handle(PLAYABLE_FACTION).invoke(factionConfig);
        }, false);
    }

    public static boolean isEliminated(String factionId) {
        return isPlayable(factionId) && !FACTION_API.call(api -> (boolean) api.handle(IS_FACTION_ALIVE).invoke(factionId), true);
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (!ALLIANCE_API.missingMembers().isEmpty()) {
            missing.add("AllianceManager.areFactionsAllied, used to send faction fleets to allied volumes");
        }
        if (!FACTION_API.missingMembers().isEmpty()) {
            missing.add("SectorManager.isFactionAlive and NexFactionConfig.playableFaction, used to move eliminated factions' anchors to salvage");
        }
        return missing;
    }
}
