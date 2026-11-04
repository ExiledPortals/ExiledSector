package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.Set;

public final class AtlasRuntime {

    private static AtlasGenerator cachedGenerator;
    private static AtlasState cachedState;
    private static Set<String> cachedFlags;

    private AtlasRuntime() {
    }

    public static synchronized AtlasGenerator generator(SectorAPI sector) {
        AtlasState state = AtlasState.get(sector);
        AtlasUnlocks unlocks = AtlasData.unlocksFor(sector);
        if (cachedGenerator == null || cachedState != state || !unlocks.unlockedFlags().equals(cachedFlags)) {
            cachedGenerator = new AtlasGenerator(state.seed(), AtlasData.rules(), unlocks, state);
            cachedState = state;
            cachedFlags = unlocks.unlockedFlags();
        }
        return cachedGenerator;
    }

    public static String regionOf(SectorAPI sector, AtlasRunNode node) {
        return sector == null || node == null ? null : generator(sector).regionOf(AtlasCell.unpack(node.cell())).id();
    }

    public static synchronized void invalidate() {
        cachedGenerator = null;
        cachedState = null;
        cachedFlags = null;
    }
}
