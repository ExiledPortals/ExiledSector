package exiledlib.compat;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.PlayerFleet;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class SecondInCommandCompat {

    public static final String MOD_ID = "second_in_command";
    public static final String CONTROLLER_HULLMOD_ID = "sc_skill_controller";
    public static final String RECONFIGURATION_SKILL_ID = "sc_strikecraft_reconfiguration";
    public static final List<String> TESTED_VERSIONS = List.of("2.0.0");
    public static final CompatTarget TARGET = new CompatTarget("Second-in-Command", List.of(MOD_ID), TESTED_VERSIONS,
            SecondInCommandCompat::missingFeatures);
    private static final String INACTIVE_SMOD_TAG_PREFIX = "sc_inactive_smods_";

    private static final String SC_UTILS_CLASS = "second_in_command.SCUtils";
    private static final String SC_DATA_CLASS = "second_in_command.SCData";

    static final ApiMember GET_FLEET_DATA = ApiMember.staticMethod(SC_UTILS_CLASS, "getFleetData", SC_DATA_CLASS,
            "com.fs.starfarer.api.campaign.CampaignFleetAPI");
    static final ApiMember IS_SKILL_ACTIVE = ApiMember.instanceMethod(SC_DATA_CLASS, "isSkillActive", ApiMember.BOOLEAN, "java.lang.String");
    private static final OptionalApi SKILL_API = new OptionalApi(List.of(IS_SKILL_ACTIVE, GET_FLEET_DATA),
            Logger.getLogger(SecondInCommandCompat.class), Level.ERROR,
            "Second-in-Command skill lookup failed; disabling compatibility checks", OptionalApi.RuntimeFailure.DISABLE);

    private SecondInCommandCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static boolean isSkillActive(FleetMemberAPI member, String skillId) {
        if (member == null || !isModEnabled()) {
            return false;
        }
        CampaignFleetAPI commandingFleet = fleetFor(member);
        if (commandingFleet == null) {
            return false;
        }
        return SKILL_API.call(api -> {
            Object scData = api.handle(GET_FLEET_DATA).invoke(commandingFleet);
            return scData != null && (boolean) api.handle(IS_SKILL_ACTIVE).invoke(scData, skillId);
        }, false);
    }

    public static void clearCachedLookups() {
        SKILL_API.reset();
    }

    public static boolean hasDeactivatedSMod(ShipVariantAPI variant, String hullModId) {
        return variant != null && variant.hasTag(INACTIVE_SMOD_TAG_PREFIX + hullModId);
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(List.of(CONTROLLER_HULLMOD_ID)));
        if (!SKILL_API.missingMembers().isEmpty()) {
            missing.add("SCUtils.getFleetData and SCData.isSkillActive, used to check its skills");
        }
        return missing;
    }

    private static CampaignFleetAPI fleetFor(FleetMemberAPI member) {
        FleetDataAPI fleetData = member.getFleetData();
        CampaignFleetAPI memberFleet = fleetData == null ? null : fleetData.getFleet();
        if (memberFleet == null) {
            return null;
        }
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        boolean joinedAllyFleet = playerFleet != null && memberFleet != playerFleet
                && playerFleet.getFleetData() != null
                && playerFleet.getFleetData().getMembersListCopy().contains(member);
        return joinedAllyFleet ? playerFleet : memberFleet;
    }
}
