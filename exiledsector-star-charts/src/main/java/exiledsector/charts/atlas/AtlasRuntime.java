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
        AtlasRules rules = AtlasData.rules();
        AtlasUnlocks unlocks = AtlasData.unlocksFor(sector, rules);
        if (cachedGenerator == null || cachedState != state || !unlocks.unlockedFlags().equals(cachedFlags)) {
            cachedGenerator = new AtlasGenerator(state.seed(), rules, unlocks, state);
            cachedState = state;
            cachedFlags = unlocks.unlockedFlags();
        }
        return cachedGenerator;
    }

    public static synchronized void invalidate() {
        cachedGenerator = null;
        cachedState = null;
        cachedFlags = null;
    }
}
