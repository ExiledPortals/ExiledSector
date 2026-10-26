package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMarkerRule;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasMapCanvasTest {

    private static final long SEED = 0xa71a5L;

    private static AtlasGenerator generator(AtlasState state) {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        List<AtlasSpecial> specials = List.of(
                new AtlasSpecial("bastion", AtlasSpecial.Kind.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", "", ""));
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER,
                AtlasMarkerRule.NEVER, specials, List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    @Test
    void aCompletedSystemCollapsesIntoADormantWormholeWhileOpenOnesStayOpen() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell bastion = AtlasDevTools.nearestOpenSite(generator, state, "bastion");
        AtlasDevTools.reach(generator, state, bastion);
        AtlasMapCanvas before = new AtlasMapCanvas(generator, state);

        assertEquals(1f, before.openness(bastion));
        assertEquals(0f, before.openness(AtlasCell.ORIGIN));

        state.add(bastion, AtlasState.COMPLETED);
        AtlasMapCanvas after = new AtlasMapCanvas(generator, state);
        assertEquals(0f, after.openness(bastion));

        after.collapse(bastion);
        assertEquals(1f, after.openness(bastion));
        after.advance(AtlasMapCanvas.COLLAPSE_SECONDS / 2f);
        assertEquals(0.25f, after.openness(bastion), 0.001f);
        after.advance(AtlasMapCanvas.COLLAPSE_SECONDS);
        assertEquals(0f, after.openness(bastion));
    }
}
