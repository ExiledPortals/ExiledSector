package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public final class RandomAssortmentCompat {

    public static final String MOD_ID = "assortment_of_things";
    public static final List<String> TESTED_VERSIONS = List.of("3.3.1");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("rat_exogrid", "rat_hypatia_hullmod", "rat_abyssal_grid",
            "rat_abyssal_core", "rat_seraphs_grace", "rat_raphaels_grace");
    public static final CompatTarget TARGET = new CompatTarget("Random Assortment of Things areas", List.of(MOD_ID), TESTED_VERSIONS,
            RandomAssortmentCompat::missingFeatures);

    static final String EXO_DATA_MEMORY_KEY = "$rat_exo_data";
    static final String EXO_DATA_CLASS = "assortment_of_things.exotech.ExoData";

    private static MethodHandle recoveredExoshipHandle;
    private static MethodHandle playerExoshipHandle;
    private static boolean exoshipLookupUnavailable;

    private RandomAssortmentCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and any failure inside RAT must keep the node locked rather than crash the game
    @SuppressWarnings("java:S1181")
    public static boolean playerOwnsExoship() {
        SectorAPI sector = Global.getSector();
        if (exoshipLookupUnavailable || sector == null) {
            return false;
        }
        Object exoData = sector.getMemoryWithoutUpdate().get(EXO_DATA_MEMORY_KEY);
        if (exoData == null) {
            return false;
        }
        try {
            resolveExoshipMethods();
            return (boolean) recoveredExoshipHandle.invoke(exoData) || playerExoshipHandle.invoke(exoData) != null;
        } catch (Throwable e) {
            exoshipLookupUnavailable = true;
            Logger.getLogger(RandomAssortmentCompat.class).error("Random Assortment of Things Exoship lookup failed; the ExoTech beacon stays locked", e);
            return false;
        }
    }

    static void clearCachedLookups() {
        recoveredExoshipHandle = null;
        playerExoshipHandle = null;
        exoshipLookupUnavailable = false;
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));
        try {
            resolveExoshipMethods();
        } catch (ReflectiveOperationException | LinkageError e) {
            missing.add("ExoData.getRecoveredExoship and getPlayerExoship, used to unlock the ExoTech beacon");
        }
        return missing;
    }

    private static void resolveExoshipMethods() throws ReflectiveOperationException {
        if (recoveredExoshipHandle != null) {
            return;
        }
        Class<?> exoDataClass = Class.forName(EXO_DATA_CLASS, true, Global.getSettings().getScriptClassLoader());
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        recoveredExoshipHandle = lookup.findVirtual(exoDataClass, "getRecoveredExoship", MethodType.methodType(boolean.class));
        playerExoshipHandle = lookup.findVirtual(exoDataClass, "getPlayerExoship",
                MethodType.methodType(SectorEntityToken.class));
    }
}
