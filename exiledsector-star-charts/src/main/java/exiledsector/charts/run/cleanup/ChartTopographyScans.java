package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTFactorTracker;

import java.util.LinkedHashSet;
import java.util.List;

public final class ChartTopographyScans {

    static final int UNKNOWN = -1;
    static final String SCANNED_FIELD = "scanned";
    private static final PrivateField<HTFactorTracker> SCANNED = new PrivateField<>(HTFactorTracker.class, SCANNED_FIELD, LinkedHashSet.class,
            "The Hyperspace Topography scan list can't be read, so the Star Chart readout shows it as unknown");

    private ChartTopographyScans() {
    }

    public static int count(SectorAPI sector) {
        if (!SCANNED.reachable()) {
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

    private static int scannedCount(HTFactorTracker tracker) {
        return SCANNED.read(tracker) instanceof LinkedHashSet<?> scanned ? scanned.size() : UNKNOWN;
    }
}
