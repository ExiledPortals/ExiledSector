package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RandomAssortmentCompat {

    public static final String MOD_ID = "assortment_of_things";
    public static final List<String> TESTED_VERSIONS = List.of("3.3.1");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("rat_exogrid", "rat_hypatia_hullmod", "rat_abyssal_grid",
            "rat_abyssal_core", "rat_seraphs_grace", "rat_raphaels_grace");
    public static final List<String> REQUIRED_COMMODITY_IDS = List.of("rat_abyssal_matter", "rat_chronos_core");
    public static final CompatTarget TARGET = new CompatTarget("Random Assortment of Things areas", List.of(MOD_ID), TESTED_VERSIONS,
            RandomAssortmentCompat::missingFeatures);

    static final String EXO_UTILS_CLASS = "assortment_of_things.exotech.ExoUtils";
    static final String EXO_DATA_CLASS = "assortment_of_things.exotech.ExoData";
    private static final Logger LOG = Logger.getLogger(RandomAssortmentCompat.class);

    private static MethodHandle exoUtilsInstanceHandle;
    private static MethodHandle exoDataHandle;
    private static MethodHandle recoveredExoshipHandle;
    private static MethodHandle playerExoshipHandle;
    private static boolean exoshipLookupUnavailable;

    private RandomAssortmentCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and any failure inside RAT must open the gate rather than crash the game
    @SuppressWarnings("java:S1181")
    public static boolean exoshipGateOpen() {
        if (exoshipLookupUnavailable) {
            return true;
        }
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return false;
        }
        try {
            resolveExoshipMethods();
            Object exoData = exoDataHandle.invoke(exoUtilsInstanceHandle.invoke());
            return exoData != null && ((boolean) recoveredExoshipHandle.invoke(exoData) || playerExoshipHandle.invoke(exoData) != null);
        } catch (Throwable e) {
            exoshipLookupUnavailable = true;
            LOG.warn("Random Assortment of Things' Exoship state can't be read, probably because RAT changed it; "
                    + "the ExoTech beacon unlocks so its area stays usable", e);
            return true;
        }
    }

    static void clearCachedLookups() {
        exoUtilsInstanceHandle = null;
        exoDataHandle = null;
        recoveredExoshipHandle = null;
        playerExoshipHandle = null;
        exoshipLookupUnavailable = false;
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));
        try {
            resolveExoshipMethods();
        } catch (ReflectiveOperationException | LinkageError e) {
            missing.add("ExoUtils.getExoData and ExoData.getRecoveredExoship/getPlayerExoship, used to unlock the ExoTech beacon "
                    + "(the beacon unlocks without them)");
        }
        SettingsAPI settings = Global.getSettings();
        if (!specExists(() -> settings.getHullSpec(AbyssSingularity.GENESIS_HULL_ID))) {
            missing.add("the Genesis-class hull " + AbyssSingularity.GENESIS_HULL_ID
                    + ", used to unlock the Abyss when the Singularity is beaten (the Abyss unlocks without it)");
        }
        if (!specExists(() -> settings.doesVariantExist(AbyssSingularity.GENESIS_WRECK_VARIANT_ID) ? Boolean.TRUE : null)) {
            missing.add("the variant " + AbyssSingularity.GENESIS_WRECK_VARIANT_ID + ", used to spot saves that already beat the Singularity");
        }
        if (!specExists(() -> settings.getSkillSpec(AbyssSingularity.REQUIEM_SKILL_ID))) {
            missing.add("the skill " + AbyssSingularity.REQUIEM_SKILL_ID + ", used to spot saves that already beat the Singularity");
        }
        for (String commodityId : REQUIRED_COMMODITY_IDS) {
            if (!specExists(() -> settings.getCommoditySpec(commodityId))) {
                missing.add("the commodity " + commodityId + ", charged by Abyss nodes (their cost is waived without it)");
            }
        }
        return missing;
    }

    static boolean specExists(Supplier<Object> spec) {
        try {
            return spec.get() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void resolveExoshipMethods() throws ReflectiveOperationException {
        if (playerExoshipHandle != null) {
            return;
        }
        ClassLoader scriptClassLoader = Global.getSettings().getScriptClassLoader();
        Class<?> exoUtilsClass = Class.forName(EXO_UTILS_CLASS, true, scriptClassLoader);
        Class<?> exoDataClass = Class.forName(EXO_DATA_CLASS, true, scriptClassLoader);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        exoUtilsInstanceHandle = lookup.findStaticGetter(exoUtilsClass, "INSTANCE", exoUtilsClass);
        exoDataHandle = lookup.findVirtual(exoUtilsClass, "getExoData", MethodType.methodType(exoDataClass));
        recoveredExoshipHandle = lookup.findVirtual(exoDataClass, "getRecoveredExoship", MethodType.methodType(boolean.class));
        playerExoshipHandle = lookup.findVirtual(exoDataClass, "getPlayerExoship", MethodType.methodType(SectorEntityToken.class));
    }
}
