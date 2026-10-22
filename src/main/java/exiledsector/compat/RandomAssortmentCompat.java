package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class RandomAssortmentCompat {

    public static final String MOD_ID = "assortment_of_things";
    public static final List<String> TESTED_VERSIONS = List.of("3.3.1");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("rat_exogrid", "rat_hypatia_hullmod", "rat_abyssal_grid",
            "rat_abyssal_core", "rat_seraphs_grace", "rat_raphaels_grace");
    public static final List<String> REQUIRED_COMMODITY_IDS = List.of("rat_abyssal_matter", "rat_chronos_core");
    public static final CompatTarget TARGET = new CompatTarget("Random Assortment of Things areas", List.of(MOD_ID), TESTED_VERSIONS,
            RandomAssortmentCompat::missingFeatures);

    static final String GENESIS_HULL_ID = "rat_genesis";
    static final String GENESIS_WRECK_VARIANT_ID = "rat_genesis_Hull";
    static final String REQUIEM_SKILL_ID = "rat_abyssal_requiem";
    static final String EXO_UTILS_CLASS = "assortment_of_things.exotech.ExoUtils";
    static final String EXO_DATA_CLASS = "assortment_of_things.exotech.ExoData";
    static final ApiMember EXO_UTILS_INSTANCE = ApiMember.staticGetter(EXO_UTILS_CLASS, "INSTANCE", EXO_UTILS_CLASS);
    static final ApiMember GET_EXO_DATA = ApiMember.instanceMethod(EXO_UTILS_CLASS, "getExoData", EXO_DATA_CLASS);
    static final ApiMember GET_RECOVERED_EXOSHIP = ApiMember.instanceMethod(EXO_DATA_CLASS, "getRecoveredExoship", ApiMember.BOOLEAN);
    static final ApiMember GET_PLAYER_EXOSHIP = ApiMember.instanceMethod(EXO_DATA_CLASS, "getPlayerExoship",
            "com.fs.starfarer.api.campaign.SectorEntityToken");
    private static final OptionalApi EXOSHIP_API = new OptionalApi(
            List.of(EXO_UTILS_INSTANCE, GET_EXO_DATA, GET_RECOVERED_EXOSHIP, GET_PLAYER_EXOSHIP),
            Logger.getLogger(RandomAssortmentCompat.class), Level.WARN,
            "Random Assortment of Things' Exoship state can't be read, probably because RAT changed it; "
                    + "the Exoship node stays hidden and the Hyper-Nav Beacon anchor stays inert",
            OptionalApi.RuntimeFailure.RETRY_NEXT_CALL);

    private RandomAssortmentCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static boolean exoshipConfirmedRecovered() {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return false;
        }
        return EXOSHIP_API.call(api -> {
            Object exoData = api.handle(GET_EXO_DATA).invoke(api.handle(EXO_UTILS_INSTANCE).invoke());
            return exoData != null && ((boolean) api.handle(GET_RECOVERED_EXOSHIP).invoke(exoData)
                    || api.handle(GET_PLAYER_EXOSHIP).invoke(exoData) != null);
        }, false);
    }

    static void clearCachedLookups() {
        EXOSHIP_API.reset();
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));
        if (!EXOSHIP_API.missingMembers().isEmpty()) {
            missing.add("ExoUtils.getExoData and ExoData.getRecoveredExoship/getPlayerExoship, used to unlock the Exoship and its "
                    + "Hyper-Nav Beacon (both stay locked without them)");
        }
        SettingsAPI settings = Global.getSettings();
        if (!CompatTarget.specExists(() -> settings.getHullSpec(GENESIS_HULL_ID))) {
            missing.add("the Genesis-class hull " + GENESIS_HULL_ID
                    + ", used to unlock the Abyss when the Singularity is beaten (the Abyss unlocks without it)");
        }
        if (!CompatTarget.specExists(() -> settings.doesVariantExist(GENESIS_WRECK_VARIANT_ID) ? Boolean.TRUE : null)) {
            missing.add("the variant " + GENESIS_WRECK_VARIANT_ID + ", used to spot saves that already beat the Singularity");
        }
        if (!CompatTarget.specExists(() -> settings.getSkillSpec(REQUIEM_SKILL_ID))) {
            missing.add("the skill " + REQUIEM_SKILL_ID + ", used to spot saves that already beat the Singularity");
        }
        for (String commodityId : REQUIRED_COMMODITY_IDS) {
            if (!CompatTarget.specExists(() -> settings.getCommoditySpec(commodityId))) {
                missing.add("the commodity " + commodityId + ", charged by Abyss nodes (their cost is waived without it)");
            }
        }
        return missing;
    }
}
