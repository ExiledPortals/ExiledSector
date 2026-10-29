package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static WeakReference<FleetDataAPI> cachedPlayerFleetData = new WeakReference<>(null);
    private static long cachedPlayerFleetAt;
    private static int cachedPlayerMemberCount = -1;
    private static Set<String> cachedPlayerMemberIds = Set.of();

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
        cachedPlayerFleetData = new WeakReference<>(null);
        cachedPlayerMemberIds = Set.of();
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
                && inPlayerFleet(playerFleet.getFleetData(), member);
        return joinedAllyFleet ? playerFleet : memberFleet;
    }

    private static boolean inPlayerFleet(FleetDataAPI playerFleetData, FleetMemberAPI member) {
        CampaignClockAPI clock = Global.getSector().getClock();
        long now = clock == null ? Long.MIN_VALUE : clock.getTimestamp();
        int memberCount = playerFleetData.getNumMembers();
        if (cachedPlayerFleetData.get() != playerFleetData || now != cachedPlayerFleetAt || memberCount != cachedPlayerMemberCount) {
            Set<String> memberIds = new HashSet<>();
            for (FleetMemberAPI playerMember : playerFleetData.getMembersListCopy()) {
                memberIds.add(playerMember.getId());
            }
            cachedPlayerFleetData = new WeakReference<>(playerFleetData);
            cachedPlayerFleetAt = now;
            cachedPlayerMemberCount = memberCount;
            cachedPlayerMemberIds = memberIds;
        }
        return cachedPlayerMemberIds.contains(member.getId());
    }
}
