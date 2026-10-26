package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public final class ChartUsedNames {

    static final int UNKNOWN = -1;
    static final String NAMES_FIELD = "names";
    private static final Logger LOG = Logger.getLogger(ChartUsedNames.class);
    private static final PrivateField<ProcgenUsedNames> NAMES = new PrivateField<>(ProcgenUsedNames.class, NAMES_FIELD, Set.class,
            "The used procgen name list can't be reached, so names reserved while generating Star Chart systems"
                    + " stay reserved and the readout shows the count as unknown");

    private ChartUsedNames() {
    }

    public static Set<Object> snapshot() {
        Set<?> live = liveNames();
        return live == null ? null : new HashSet<>(live);
    }

    public static int releaseAddedSince(Set<Object> before) {
        if (before == null) {
            return 0;
        }
        Set<?> live = liveNames();
        if (live == null) {
            return 0;
        }
        int sizeBefore = live.size();
        live.removeIf(name -> !before.contains(name));
        return sizeBefore - live.size();
    }

    public static int count() {
        Set<?> live = liveNames();
        return live == null ? UNKNOWN : live.size();
    }

    private static Set<?> liveNames() {
        if (!NAMES.reachable()) {
            return null;
        }
        ProcgenUsedNames usedNames;
        try {
            usedNames = ProcgenUsedNames.getUsed();
        } catch (RuntimeException e) {
            LOG.debug("The used procgen name list could not be read this time", e);
            return null;
        }
        return NAMES.read(usedNames) instanceof Set<?> names ? names : null;
    }
}
