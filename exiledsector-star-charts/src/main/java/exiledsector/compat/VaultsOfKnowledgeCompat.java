package exiledsector.compat;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.ModSettings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class VaultsOfKnowledgeCompat {

    public static final String MOD_ID = "aotd_vok";
    public static final String GATEWAY_RESEARCH_ID = "exiledSector_tech_wormhole_gateway";
    public static final String GATEWAY_ADVANCED_RESEARCH_ID = "exiledSector_tech_wormhole_gateway_advanced";
    public static final String GATEWAY_PERFECTED_RESEARCH_ID = "exiledSector_tech_wormhole_gateway_perfected";
    public static final String DATABANK_DROP_GROUP = "ashes_research";
    static final String DEFENCE_MIN_SETTING = "aotd_defence_min_val";
    static final String DEFENCE_MAX_SETTING = "aotd_defence_max_val";
    static final int DEFAULT_DEFENCE_MIN = 140;
    static final int DEFAULT_DEFENCE_MAX = 180;
    public static final List<String> TESTED_VERSIONS = List.of("5.0.7");
    public static final CompatTarget TARGET = new CompatTarget("Ashes of the Domain: Vaults of Knowledge", List.of(MOD_ID), TESTED_VERSIONS,
            VaultsOfKnowledgeCompat::missingFeatures);

    private static final String RESEARCH_MANAGER_CLASS = "data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager";

    static final ApiMember GET_INSTANCE = ApiMember.staticMethod(RESEARCH_MANAGER_CLASS, "getInstance", RESEARCH_MANAGER_CLASS);
    static final ApiMember IS_AVAILABLE_FOR_MARKET = ApiMember.instanceMethod(RESEARCH_MANAGER_CLASS, "isAvailableForThisMarket",
            ApiMember.BOOLEAN, "java.lang.String", "com.fs.starfarer.api.campaign.econ.MarketAPI");
    private static final OptionalApi RESEARCH_API = new OptionalApi(List.of(GET_INSTANCE, IS_AVAILABLE_FOR_MARKET),
            Logger.getLogger(VaultsOfKnowledgeCompat.class), Level.ERROR,
            "Vaults of Knowledge research lookup failed; the Wormhole Gateway is treated as researched", OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);
    static final ApiMember IS_RESEARCHED_FOR_PLAYER = ApiMember.instanceMethod(RESEARCH_MANAGER_CLASS, "isResearchedForPlayer",
            ApiMember.BOOLEAN, "java.lang.String");
    private static final OptionalApi PLAYER_RESEARCH_API = new OptionalApi(List.of(GET_INSTANCE, IS_RESEARCHED_FOR_PLAYER),
            Logger.getLogger(VaultsOfKnowledgeCompat.class), Level.ERROR,
            "Vaults of Knowledge player research lookup failed; the Wormhole Gateway upgrades are treated as researched",
            OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);

    private VaultsOfKnowledgeCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static boolean isGatewayResearched(MarketAPI market) {
        if (!isModEnabled()) {
            return true;
        }
        return RESEARCH_API.call(api -> {
            Object manager = api.handle(GET_INSTANCE).invoke();
            return manager == null || (boolean) api.handle(IS_AVAILABLE_FOR_MARKET).invoke(manager, GATEWAY_RESEARCH_ID, market);
        }, true);
    }

    public static boolean isResearchedForPlayer(String researchId) {
        if (!isModEnabled()) {
            return true;
        }
        return PLAYER_RESEARCH_API.call(api -> {
            Object manager = api.handle(GET_INSTANCE).invoke();
            return manager == null || (boolean) api.handle(IS_RESEARCHED_FOR_PLAYER).invoke(manager, researchId);
        }, true);
    }

    public static int defenceFleetPoints(Random random) {
        return defenceFleetPoints(ModSettings.otherModIntOr(MOD_ID, DEFENCE_MIN_SETTING, DEFAULT_DEFENCE_MIN),
                ModSettings.otherModIntOr(MOD_ID, DEFENCE_MAX_SETTING, DEFAULT_DEFENCE_MAX), random);
    }

    static int defenceFleetPoints(int min, int max, Random random) {
        int low = Math.max(0, Math.min(min, max));
        int high = Math.max(low, Math.max(min, max));
        return low + random.nextInt(high - low + 1);
    }

    static void clearCachedLookups() {
        RESEARCH_API.reset();
        PLAYER_RESEARCH_API.reset();
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (!RESEARCH_API.missingMembers().isEmpty()) {
            missing.add("AoTDMainResearchManager.getInstance and isAvailableForThisMarket, used to gate the Wormhole Gateway behind research");
        }
        if (!PLAYER_RESEARCH_API.missingMembers().isEmpty()) {
            missing.add("AoTDMainResearchManager.getInstance and isResearchedForPlayer, used to raise the Wormhole Gateway tier limit");
        }
        return missing;
    }
}
