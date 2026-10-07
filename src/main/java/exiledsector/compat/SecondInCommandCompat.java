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

    private static MethodHandle getFleetDataHandle;
    private static MethodHandle isSkillActiveHandle;
    private static boolean lookupUnavailable;

    private SecondInCommandCompat() {
    }

    public static boolean isModEnabled() {
        SettingsAPI settings = Global.getSettings();
        return settings != null && settings.getModManager().isModEnabled(MOD_ID);
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and any failure inside Second-in-Command must disable the checks rather than crash the game
    @SuppressWarnings("java:S1181")
    public static boolean isSkillActive(FleetMemberAPI member, String skillId) {
        if (member == null || lookupUnavailable || !isModEnabled()) {
            return false;
        }
        CampaignFleetAPI commandingFleet = fleetFor(member);
        if (commandingFleet == null) {
            return false;
        }
        try {
            resolveMethods();
            Object scData = getFleetDataHandle.invoke(commandingFleet);
            return scData != null && (boolean) isSkillActiveHandle.invoke(scData, skillId);
        } catch (Throwable e) {
            lookupUnavailable = true;
            Logger.getLogger(SecondInCommandCompat.class).error("Second-in-Command skill lookup failed; disabling compatibility checks", e);
            return false;
        }
    }

    static void clearCachedLookups() {
        getFleetDataHandle = null;
        isSkillActiveHandle = null;
        lookupUnavailable = false;
    }

    public static boolean hasDeactivatedSMod(ShipVariantAPI variant, String hullModId) {
        return variant != null && variant.hasTag(INACTIVE_SMOD_TAG_PREFIX + hullModId);
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(List.of(CONTROLLER_HULLMOD_ID)));
        try {
            resolveMethods();
        } catch (ReflectiveOperationException | LinkageError e) {
            missing.add("SCUtils.getFleetData and SCData.isSkillActive, used to check its skills");
        }
        return missing;
    }

    private static void resolveMethods() throws ReflectiveOperationException {
        if (getFleetDataHandle != null) {
            return;
        }
        ClassLoader loader = Global.getSettings().getScriptClassLoader();
        Class<?> scUtilsClass = Class.forName(SC_UTILS_CLASS, true, loader);
        Class<?> scDataClass = Class.forName(SC_DATA_CLASS, true, loader);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        isSkillActiveHandle = lookup.findVirtual(scDataClass, "isSkillActive", MethodType.methodType(boolean.class, String.class));
        getFleetDataHandle = lookup.findStatic(scUtilsClass, "getFleetData", MethodType.methodType(scDataClass, CampaignFleetAPI.class));
    }

    private static CampaignFleetAPI fleetFor(FleetMemberAPI member) {
        FleetDataAPI fleetData = member.getFleetData();
        CampaignFleetAPI memberFleet = fleetData == null ? null : fleetData.getFleet();
        if (memberFleet == null) {
            return null;
        }
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector == null ? null : sector.getPlayerFleet();
        boolean joinedAllyFleet = playerFleet != null && memberFleet != playerFleet
                && playerFleet.getFleetData() != null
                && playerFleet.getFleetData().getMembersListCopy().contains(member);
        return joinedAllyFleet ? playerFleet : memberFleet;
    }
}
