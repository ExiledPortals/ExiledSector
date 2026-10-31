package exiledsector.compat;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.ModSettings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class VaultsOfKnowledgeCompat {

    public static final String MOD_ID = "aotd_vok";
    public static final String GATEWAY_RESEARCH_ID = "exiledSector_tech_wormhole_gateway";
    public static final String GATEWAY_ADVANCED_RESEARCH_ID = "exiledSector_tech_wormhole_gateway_advanced";
    public static final String GATEWAY_PERFECTED_RESEARCH_ID = "exiledSector_tech_wormhole_gateway_perfected";
    public static final List<String> GATEWAY_RESEARCH_IDS = List.of(GATEWAY_RESEARCH_ID, GATEWAY_ADVANCED_RESEARCH_ID, GATEWAY_PERFECTED_RESEARCH_ID);
    public static final String DATABANK_DROP_GROUP = "ashes_research";
    static final String DEFENCE_MIN_SETTING = "aotd_defence_min_val";
    static final String DEFENCE_MAX_SETTING = "aotd_defence_max_val";
    static final int DEFAULT_DEFENCE_MIN = 140;
    static final int DEFAULT_DEFENCE_MAX = 180;
    public static final List<String> TESTED_VERSIONS = List.of("5.0.7");
    public static final CompatTarget TARGET = new CompatTarget("Ashes of the Domain: Vaults of Knowledge", List.of(MOD_ID), TESTED_VERSIONS,
            VaultsOfKnowledgeCompat::missingFeatures);

    private static final String RESEARCH_MANAGER_CLASS = "data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager";
    private static final String FACTION_MANAGER_CLASS = "data.kaysaar.aotd.vok.scripts.research.AoTDFactionResearchManager";
    private static final String QUEUE_CLASS = "data.kaysaar.aotd.vok.scripts.research.ResearchQueueManager";
    private static final String RESEARCH_OPTION_CLASS = "data.kaysaar.aotd.vok.scripts.research.models.ResearchOption";
    private static final String STRING = "java.lang.String";
    private static final String ARRAY_LIST = "java.util.ArrayList";

    static final ApiMember GET_INSTANCE = ApiMember.staticMethod(RESEARCH_MANAGER_CLASS, "getInstance", RESEARCH_MANAGER_CLASS);
    static final ApiMember IS_AVAILABLE_FOR_MARKET = ApiMember.instanceMethod(RESEARCH_MANAGER_CLASS, "isAvailableForThisMarket",
            ApiMember.BOOLEAN, "java.lang.String", "com.fs.starfarer.api.campaign.econ.MarketAPI");
    private static final OptionalApi RESEARCH_API = new OptionalApi(List.of(GET_INSTANCE, IS_AVAILABLE_FOR_MARKET),
            Logger.getLogger(VaultsOfKnowledgeCompat.class), Level.ERROR,
            "Vaults of Knowledge research lookup failed; the Wormhole Gateway is treated as researched", OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);
    static final ApiMember FACTION_MANAGERS = ApiMember.instanceMethod(RESEARCH_MANAGER_CLASS, "getFactionResearchManagers", ARRAY_LIST);
    static final ApiMember UPDATE_FROM_SPEC = ApiMember.instanceMethod(RESEARCH_MANAGER_CLASS, "updateResearchOptionsFromSpec", ApiMember.VOID);
    static final ApiMember RESEARCH_REPO = ApiMember.instanceMethod(FACTION_MANAGER_CLASS, "getResearchRepoOfFaction", ARRAY_LIST);
    static final ApiMember QUEUE = ApiMember.instanceMethod(FACTION_MANAGER_CLASS, "getQueueManager", QUEUE_CLASS);
    static final ApiMember REMOVE_FROM_QUEUE = ApiMember.instanceMethod(QUEUE_CLASS, "removeFromQueue", ApiMember.BOOLEAN, STRING);
    static final ApiMember FOCUS_ID = ApiMember.instanceGetter(FACTION_MANAGER_CLASS, "currentFocusId", STRING);
    static final ApiMember SET_FOCUS = ApiMember.instanceMethod(FACTION_MANAGER_CLASS, "setCurrentFocus", ApiMember.VOID, STRING);
    static final ApiMember OPTION_ID = ApiMember.instanceGetter(RESEARCH_OPTION_CLASS, "Id", STRING);
    private static final OptionalApi RESEARCH_TREE_API = new OptionalApi(List.of(GET_INSTANCE, FACTION_MANAGERS, UPDATE_FROM_SPEC, RESEARCH_REPO, QUEUE,
            REMOVE_FROM_QUEUE, FOCUS_ID, SET_FOCUS, OPTION_ID), Logger.getLogger(VaultsOfKnowledgeCompat.class), Level.ERROR,
            "Vaults of Knowledge research tree edit failed; the Wormhole Gateway research stays as it was", OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);
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

    public static void showGatewayResearch(boolean shown) {
        if (!isModEnabled()) {
            return;
        }
        RESEARCH_TREE_API.call(api -> {
            Object manager = api.handle(GET_INSTANCE).invoke();
            if (manager == null) {
                return false;
            }
            List<?> factionManagers = (List<?>) api.handle(FACTION_MANAGERS).invoke(manager);
            if (shown) {
                if (anyLacksGatewayResearch(api, factionManagers)) {
                    api.handle(UPDATE_FROM_SPEC).invoke(manager);
                }
            } else {
                for (Object factionManager : factionManagers) {
                    removeGatewayResearch(api, factionManager);
                }
            }
            return true;
        }, false);
    }

    private static boolean anyLacksGatewayResearch(OptionalApi api, List<?> factionManagers) throws Throwable {
        for (Object factionManager : factionManagers) {
            List<?> repo = (List<?>) api.handle(RESEARCH_REPO).invoke(factionManager);
            boolean found = false;
            for (Object option : repo) {
                found |= GATEWAY_RESEARCH_ID.equals(api.handle(OPTION_ID).invoke(option));
            }
            if (!found) {
                return true;
            }
        }
        return false;
    }

    private static void removeGatewayResearch(OptionalApi api, Object factionManager) throws Throwable {
        List<?> repo = (List<?>) api.handle(RESEARCH_REPO).invoke(factionManager);
        Iterator<?> options = repo.iterator();
        while (options.hasNext()) {
            if (GATEWAY_RESEARCH_IDS.contains((String) api.handle(OPTION_ID).invoke(options.next()))) {
                options.remove();
            }
        }
        Object queue = api.handle(QUEUE).invoke(factionManager);
        for (String researchId : GATEWAY_RESEARCH_IDS) {
            if (queue != null) {
                api.handle(REMOVE_FROM_QUEUE).invoke(queue, researchId);
            }
        }
        if (GATEWAY_RESEARCH_IDS.contains((String) api.handle(FOCUS_ID).invoke(factionManager))) {
            api.handle(SET_FOCUS).invoke(factionManager, (String) null);
        }
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
        RESEARCH_TREE_API.reset();
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (!RESEARCH_API.missingMembers().isEmpty()) {
            missing.add("AoTDMainResearchManager.getInstance and isAvailableForThisMarket, used to gate the Wormhole Gateway behind research");
        }
        if (!PLAYER_RESEARCH_API.missingMembers().isEmpty()) {
            missing.add("AoTDMainResearchManager.getInstance and isResearchedForPlayer, used to raise the Wormhole Gateway tier limit");
        }
        if (!RESEARCH_TREE_API.missingMembers().isEmpty()) {
            missing.add("AoTDMainResearchManager, AoTDFactionResearchManager and ResearchQueueManager research lists, used to remove the Wormhole Gateway research while the Atlas needs no colony");
        }
        return missing;
    }
}
