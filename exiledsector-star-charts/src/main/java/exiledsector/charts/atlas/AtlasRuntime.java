package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.tree.AtlasTree;

import java.util.Set;

public final class AtlasRuntime {

    private static AtlasGenerator cachedGenerator;
    private static AtlasState cachedState;
    private static Set<String> cachedFlags;
    private static Set<String> cachedTreeNodeIds;

    private AtlasRuntime() {
    }

    public static synchronized AtlasGenerator generator(SectorAPI sector) {
        AtlasState state = AtlasState.get(sector);
        AtlasUnlocks unlocks = AtlasData.unlocksFor(sector);
        boolean sameInputs = cachedGenerator != null && cachedState == state && unlocks.unlockedFlags().equals(cachedFlags);
        if (sameInputs && state.atlasTreeNodeIds().equals(cachedTreeNodeIds)) {
            return cachedGenerator;
        }
        AtlasEffects effects = AtlasTree.atlasEffects(state);
        if (!sameInputs || !effects.equals(cachedGenerator.effects())) {
            cachedGenerator = new AtlasGenerator(state.seed(), AtlasData.rules(), unlocks, effects, state);
            cachedState = state;
            cachedFlags = unlocks.unlockedFlags();
        }
        cachedTreeNodeIds = Set.copyOf(state.atlasTreeNodeIds());
        return cachedGenerator;
    }

    public static String regionOf(SectorAPI sector, AtlasRunNode node) {
        return sector == null || node == null ? null : generator(sector).regionOf(AtlasCell.unpack(node.cell())).id();
    }

    public static synchronized void invalidate() {
        cachedGenerator = null;
        cachedState = null;
        cachedFlags = null;
        cachedTreeNodeIds = null;
    }
}
