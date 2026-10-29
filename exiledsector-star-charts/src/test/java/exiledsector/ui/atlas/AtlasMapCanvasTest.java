package exiledsector.ui.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.relay.RelayNetwork;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMarkerRule;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void aRebuiltMapKeepsItsOrbitsWhereTheyWere() {
        AtlasState state = new AtlasState(SEED);
        AtlasMapCanvas before = new AtlasMapCanvas(generator(state), state);
        before.advance(3f);

        assertTrue(before.elapsedSeconds() > 0f);
        assertEquals(before.elapsedSeconds(), before.rebuilt().elapsedSeconds());
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

    @Test
    void eachAnomalyOrbitsItsSystemAsAnIconThatCanBeHovered() throws Exception {
        ChartTables.loadAnomalies();
        try {
            AtlasState state = new AtlasState(SEED);
            AtlasGenerator generator = generator(state);
            AtlasCell bastion = AtlasDevTools.nearestOpenSite(generator, state, "bastion");
            AtlasDevTools.reach(generator, state, bastion);
            AtlasAnomalies.insert(generator, state, bastion, SpatialBreach.ID, AtlasAnomalyRecord.Source.WORLD, null);
            AtlasMapCanvas canvas = new AtlasMapCanvas(generator, state);
            TreeViewport view = new TreeViewport(640f, 360f, AtlasMapCanvas.MIN_ZOOM);

            AtlasMapCanvas.AnomalyIcon icon = canvas.anomalyIcons().stream().filter(candidate -> candidate.cell().equals(bastion)).findFirst()
                    .orElseThrow();
            assertEquals(SpatialBreach.ID, icon.anomalyId());
            assertEquals("graphics/icons/anomalies/spatial_breach.png", icon.iconPath());

            float orbit = AtlasMapCanvas.footprint(view) * AtlasMapCanvas.ANOMALY_ORBIT_SHARE;
            float iconX = canvas.screenX(view, bastion) + (float) Math.cos(AtlasMapCanvas.ANOMALY_FIRST_ANGLE) * orbit;
            float iconY = canvas.screenY(view, bastion) + (float) Math.sin(AtlasMapCanvas.ANOMALY_FIRST_ANGLE) * orbit;
            assertEquals(icon, canvas.anomalyAt(view, iconX, iconY));
            assertNull(canvas.anomalyAt(view, canvas.screenX(view, bastion), canvas.screenY(view, bastion)));
        } finally {
            ChartTables.resetAnomalies();
        }
    }

    @Test
    void aChainedAnomalyShowsEvenInFogWithATraceFromTheSystemItLeft() throws Exception {
        ChartTables.loadAnomalies();
        try {
            AtlasState state = new AtlasState(SEED);
            AtlasGenerator generator = generator(state);
            AtlasCell left = generator.lanes(AtlasCell.ORIGIN).get(0);
            state.add(left, AtlasState.COMPLETED);
            AtlasCell fogged = AtlasCell.ORIGIN.around(12).stream()
                    .filter(cell -> !generator.isVoid(cell) && !AtlasReach.visible(generator, state).contains(cell))
                    .findFirst().orElseThrow();
            AtlasAnomalies.insert(generator, state, fogged, RelayNetwork.ID, AtlasAnomalyRecord.Source.WORLD, null,
                    new AtlasAnomalyRecord.Chain(2, 3, left));
            AtlasMapCanvas canvas = new AtlasMapCanvas(generator, state);
            TreeViewport view = new TreeViewport(640f, 360f, AtlasMapCanvas.MIN_ZOOM);

            assertEquals(List.of(new AtlasMapCanvas.Trace(left, fogged)), canvas.traces());
            AtlasMapCanvas.AnomalyIcon icon = canvas.anomalyIcons().stream().filter(candidate -> candidate.cell().equals(fogged)).findFirst()
                    .orElseThrow();
            assertTrue(icon.fogged());
            assertEquals(RelayNetwork.ID, icon.anomalyId());
            assertEquals(icon, canvas.anomalyAt(view, canvas.screenX(view, fogged), canvas.screenY(view, fogged)));
            assertEquals(generator.node(fogged), canvas.nodeAt(fogged));
        } finally {
            ChartTables.resetAnomalies();
        }
    }
}
