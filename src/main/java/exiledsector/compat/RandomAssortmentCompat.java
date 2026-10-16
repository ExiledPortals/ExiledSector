package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
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

    static final String CATALYST_ENTITY_TYPE = "rat_abyss_primordial_activator";
    static final String CATALYST_COMPLETED_TAG = "completed";
    private static final long CATALYST_SEARCH_INTERVAL_NANOS = 10_000_000_000L;

    private static WeakReference<SectorEntityToken> cachedCatalyst = new WeakReference<>(null);
    private static WeakReference<SectorAPI> cachedCatalystSector = new WeakReference<>(null);
    private static long lastCatalystSearchNanos;
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

    public static boolean primordialCatalystActivated() {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return false;
        }
        SectorEntityToken catalyst = catalystIn(sector);
        return catalyst != null && catalyst.hasTag(CATALYST_COMPLETED_TAG);
    }

    private static SectorEntityToken catalystIn(SectorAPI sector) {
        SectorEntityToken cached = cachedCatalyst.get();
        if (cached != null && cachedCatalystSector.get() == sector) {
            return cached;
        }
        long now = System.nanoTime();
        if (cachedCatalystSector.get() == sector && now - lastCatalystSearchNanos < CATALYST_SEARCH_INTERVAL_NANOS) {
            return null;
        }
        lastCatalystSearchNanos = now;
        cachedCatalystSector = new WeakReference<>(sector);
        cachedCatalyst = new WeakReference<>(null);
        for (StarSystemAPI system : sector.getStarSystems()) {
            for (CustomCampaignEntityAPI entity : system.getCustomEntities()) {
                if (CATALYST_ENTITY_TYPE.equals(entity.getCustomEntityType())) {
                    cachedCatalyst = new WeakReference<>(entity);
                    return entity;
                }
            }
        }
        return null;
    }

    static void clearCachedLookups() {
        recoveredExoshipHandle = null;
        playerExoshipHandle = null;
        exoshipLookupUnavailable = false;
        cachedCatalyst = new WeakReference<>(null);
        cachedCatalystSector = new WeakReference<>(null);
        lastCatalystSearchNanos = 0L;
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
