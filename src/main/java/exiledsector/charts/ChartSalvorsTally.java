package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.CoreDiscoverEntityPlugin.SalvorsTallyAdder;
import com.fs.starfarer.api.impl.campaign.intel.misc.SalvorsTallyIntel;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

final class ChartSalvorsTally {

    static final String INTEL_FIELD = "intel";
    private static final Logger LOG = Logger.getLogger(ChartSalvorsTally.class);
    private static MethodHandle intelGetter;
    private static boolean unreachable;

    private ChartSalvorsTally() {
    }

    static int removePendingFor(SectorAPI sector, StarSystemAPI pocket) {
        if (pocket == null) {
            return 0;
        }
        int removed = 0;
        for (EveryFrameScript script : new ArrayList<>(sector.getScripts())) {
            if (script instanceof SalvorsTallyAdder adder && pendingIntelSystem(adder) == pocket) {
                sector.removeScript(script);
                removed++;
            }
        }
        return removed;
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost this safety net, never the close
    @SuppressWarnings("java:S1181")
    private static StarSystemAPI pendingIntelSystem(SalvorsTallyAdder adder) {
        if (unreachable) {
            return null;
        }
        try {
            if (intelGetter == null) {
                intelGetter = MethodHandles.privateLookupIn(SalvorsTallyAdder.class, MethodHandles.lookup())
                        .findGetter(SalvorsTallyAdder.class, INTEL_FIELD, SalvorsTallyIntel.class);
            }
            return intelGetter.invoke(adder) instanceof SalvorsTallyIntel intel ? intel.getSystem() : null;
        } catch (Throwable e) {
            unreachable = true;
            LOG.warn("[ExiledSector] A pending Salvor's Tally can't be read, so one queued for a closed Star Chart pocket may keep it in the save", e);
            return null;
        }
    }
}
