package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class SecondInCommandCompat {

    public static final String MOD_ID = "second_in_command";
    public static final String CONTROLLER_HULLMOD_ID = "sc_skill_controller";
    public static final String REDISTRIBUTION_SKILL_ID = "sc_improvisation_redistribution";
    public static final String ENHANCED_OVERRIDES_SKILL_ID = "sc_improvisation_enhanced_overrides";
    public static final String RECONFIGURATION_SKILL_ID = "sc_strikecraft_reconfiguration";
    private static final String INACTIVE_SMOD_TAG_PREFIX = "sc_inactive_smods_";

    private static final String SC_UTILS_CLASS = "second_in_command.SCUtils";
    private static final String SC_DATA_CLASS = "second_in_command.SCData";

    private static MethodHandle getFleetData;
    private static MethodHandle isSkillActive;
    private static boolean unavailable;

    private SecondInCommandCompat() {
    }

    public static boolean isModEnabled() {
        SettingsAPI settings = Global.getSettings();
        return settings != null && settings.getModManager().isModEnabled(MOD_ID);
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and any failure inside Second-in-Command must disable the checks rather than crash the game
    @SuppressWarnings("java:S1181")
    public static boolean isSkillActive(FleetMemberAPI member, String skillId) {
        if (member == null || unavailable || !isModEnabled()) {
            return false;
        }
        CampaignFleetAPI fleet = fleetFor(member);
        if (fleet == null) {
            return false;
        }
        try {
            resolveMethods();
            Object data = getFleetData.invoke(fleet);
            return data != null && (boolean) isSkillActive.invoke(data, skillId);
        } catch (Throwable e) {
            unavailable = true;
            Logger.getLogger(SecondInCommandCompat.class).error("Second-in-Command skill lookup failed; disabling compatibility checks", e);
            return false;
        }
    }

    static void clearCachedLookups() {
        getFleetData = null;
        isSkillActive = null;
        unavailable = false;
    }

    public static boolean hasDeactivatedSMod(ShipVariantAPI variant, String hullModId) {
        return variant != null && variant.hasTag(INACTIVE_SMOD_TAG_PREFIX + hullModId);
    }

    private static void resolveMethods() throws ReflectiveOperationException {
        if (getFleetData != null) {
            return;
        }
        ClassLoader loader = Global.getSettings().getScriptClassLoader();
        Class<?> utils = Class.forName(SC_UTILS_CLASS, true, loader);
        Class<?> data = Class.forName(SC_DATA_CLASS, true, loader);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        isSkillActive = lookup.findVirtual(data, "isSkillActive", MethodType.methodType(boolean.class, String.class));
        getFleetData = lookup.findStatic(utils, "getFleetData", MethodType.methodType(data, CampaignFleetAPI.class));
    }

    private static CampaignFleetAPI fleetFor(FleetMemberAPI member) {
        FleetDataAPI fleetData = member.getFleetData();
        CampaignFleetAPI fleet = fleetData == null ? null : fleetData.getFleet();
        if (fleet == null) {
            return null;
        }
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector == null ? null : sector.getPlayerFleet();
        boolean joinedAllyFleet = playerFleet != null && fleet != playerFleet
                && playerFleet.getFleetData() != null
                && playerFleet.getFleetData().getMembersListCopy().contains(member);
        return joinedAllyFleet ? playerFleet : fleet;
    }
}
