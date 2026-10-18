package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

final class ChartUsedNames {

    static final int UNKNOWN = -1;
    static final String NAMES_FIELD = "names";
    private static final Logger LOG = Logger.getLogger(ChartUsedNames.class);
    private static MethodHandle namesGetter;
    private static boolean unreachable;

    private ChartUsedNames() {
    }

    static Set<Object> snapshot() {
        Set<?> live = liveNames();
        return live == null ? null : new HashSet<>(live);
    }

    static int releaseAddedSince(Set<Object> before) {
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

    static int count() {
        Set<?> live = liveNames();
        return live == null ? UNKNOWN : live.size();
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost releasing generated names, never the pocket
    @SuppressWarnings("java:S1181")
    private static Set<?> liveNames() {
        if (unreachable) {
            return null;
        }
        try {
            if (namesGetter == null) {
                namesGetter = MethodHandles.privateLookupIn(ProcgenUsedNames.class, MethodHandles.lookup())
                        .findGetter(ProcgenUsedNames.class, NAMES_FIELD, Set.class);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            unreachable = true;
            LOG.warn("[ExiledSector] The used procgen name list can't be reached, so names reserved while generating Star Chart pockets"
                    + " stay reserved and the readout shows the count as unknown", e);
            return null;
        }
        try {
            ProcgenUsedNames usedNames = ProcgenUsedNames.getUsed();
            return usedNames != null && namesGetter.invoke(usedNames) instanceof Set<?> names ? names : null;
        } catch (Throwable e) {
            LOG.debug("The used procgen name list could not be read this time", e);
            return null;
        }
    }
}
