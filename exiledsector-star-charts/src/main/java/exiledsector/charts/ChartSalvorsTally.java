package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.CoreDiscoverEntityPlugin.SalvorsTallyAdder;
import com.fs.starfarer.api.impl.campaign.intel.misc.SalvorsTallyIntel;

import java.util.ArrayList;

final class ChartSalvorsTally {

    static final String INTEL_FIELD = "intel";
    private static final PrivateField<SalvorsTallyAdder> INTEL = new PrivateField<>(SalvorsTallyAdder.class, INTEL_FIELD, SalvorsTallyIntel.class,
            "A pending Salvor's Tally can't be read, so one queued for a closed Star Chart system may keep it in the save");

    private ChartSalvorsTally() {
    }

    static int removePendingFor(SectorAPI sector, StarSystemAPI system) {
        if (system == null) {
            return 0;
        }
        int removed = 0;
        for (EveryFrameScript script : new ArrayList<>(sector.getScripts())) {
            if (script instanceof SalvorsTallyAdder adder && pendingIntelSystem(adder) == system) {
                sector.removeScript(script);
                removed++;
            }
        }
        return removed;
    }

    private static StarSystemAPI pendingIntelSystem(SalvorsTallyAdder adder) {
        return INTEL.read(adder) instanceof SalvorsTallyIntel intel ? intel.getSystem() : null;
    }
}
