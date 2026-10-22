package exiledsector.compat;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledlib.compat.ApiMember;
import exiledlib.compat.CompatTarget;
import exiledlib.compat.OptionalApi;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class VaultsOfKnowledgeCompat {

    public static final String MOD_ID = "aotd_vok";
    public static final String GATEWAY_RESEARCH_ID = "exiledSector_tech_wormhole_gateway";
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

    static void clearCachedLookups() {
        RESEARCH_API.reset();
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (!RESEARCH_API.missingMembers().isEmpty()) {
            missing.add("AoTDMainResearchManager.getInstance and isAvailableForThisMarket, used to gate the Wormhole Gateway behind research");
        }
        return missing;
    }
}
