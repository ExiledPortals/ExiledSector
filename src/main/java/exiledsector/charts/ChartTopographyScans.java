package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTFactorTracker;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.List;

final class ChartTopographyScans {

    static final int UNKNOWN = -1;
    static final String SCANNED_FIELD = "scanned";
    private static final Logger LOG = Logger.getLogger(ChartTopographyScans.class);
    private static MethodHandle scannedGetter;
    private static boolean unreachable;

    private ChartTopographyScans() {
    }

    static int count(SectorAPI sector) {
        if (!resolve()) {
            return UNKNOWN;
        }
        List<EveryFrameScript> scripts = sector.getScripts();
        if (scripts == null) {
            return 0;
        }
        for (EveryFrameScript script : scripts) {
            if (script instanceof HTFactorTracker tracker) {
                return scannedCount(tracker);
            }
        }
        return 0;
    }

    private static boolean resolve() {
        if (unreachable) {
            return false;
        }
        if (scannedGetter != null) {
            return true;
        }
        try {
            scannedGetter = MethodHandles.privateLookupIn(HTFactorTracker.class, MethodHandles.lookup())
                    .findGetter(HTFactorTracker.class, SCANNED_FIELD, LinkedHashSet.class);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            unreachable = true;
            LOG.warn("[ExiledSector] The Hyperspace Topography scan list can't be read, so the Star Chart readout shows it as unknown", e);
            return false;
        }
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost this dev readout
    @SuppressWarnings("java:S1181")
    private static int scannedCount(HTFactorTracker tracker) {
        try {
            return scannedGetter.invoke(tracker) instanceof LinkedHashSet<?> scanned ? scanned.size() : 0;
        } catch (Throwable e) {
            LOG.debug("The Hyperspace Topography scan list could not be read this time", e);
            return UNKNOWN;
        }
    }
}
