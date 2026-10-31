package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.run.ChartRun;
import org.apache.log4j.Logger;

public final class AtlasProgress {

    private static final Logger LOG = Logger.getLogger(AtlasProgress.class);

    private AtlasProgress() {
    }

    public static void completed(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (sector == null || node == null) {
            return;
        }
        try {
            AtlasState state = AtlasState.get(sector);
            AtlasGenerator generator = AtlasRuntime.generator(sector);
            AtlasCell cell = AtlasCell.unpack(node.cell());
            complete(generator, state, cell, run.tier());
            ChartAnomalies.runCompleted(sector, run);
            AtlasReach.refreshReveal(generator, state);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Recording the completed atlas system for run " + run.runId() + " failed", e);
        }
    }

    static void complete(AtlasGenerator generator, AtlasState state, AtlasCell cell, int tier) {
        if (state.isCompleted(cell)) {
            return;
        }
        state.add(cell, AtlasState.COMPLETED);
        state.spendUnchainedAnomalies(cell);
        state.clearTier(tier);
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            special.kind().onCompleted(generator, state, cell, special);
        }
    }

    static void cleanse(AtlasGenerator generator, AtlasState state, AtlasCell nexus, AtlasSpecial special) {
        int reach = (int) Math.ceil(special.radius()) + 1;
        for (AtlasCell cell : nexus.around(reach)) {
            if (!generator.isVoid(cell) && nexus.equals(generator.infestationSource(cell))) {
                state.add(cell, AtlasState.CLEANSED);
            }
        }
    }

    public static boolean revealLanesAround(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (sector == null || node == null) {
            return false;
        }
        try {
            AtlasState state = AtlasState.get(sector);
            AtlasCell cell = AtlasCell.unpack(node.cell());
            if (state.has(cell, AtlasState.LANES_REVEALED)) {
                return false;
            }
            state.add(cell, AtlasState.LANES_REVEALED);
            AtlasReach.refreshReveal(AtlasRuntime.generator(sector), state);
            return true;
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Revealing the atlas lanes around run " + run.runId() + " failed", e);
            return false;
        }
    }

    public static void closed(SectorAPI sector, ChartRun run) {
        closedWithoutObjective(sector, run);
        if (sector != null && run != null && run.atlasNode() != null) {
            ChartAnomalies.runClosed(sector, run);
        }
    }

    public static void closedWithoutObjective(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (sector == null || node == null || run.isObjectiveComplete()) {
            return;
        }
        fail(AtlasState.get(sector), AtlasCell.unpack(node.cell()));
    }

    static void fail(AtlasState state, AtlasCell cell) {
        state.add(cell, AtlasState.FAILED | AtlasState.MARKERS_SPENT);
        state.spendInsertedAnomalies(cell);
    }

    public static void opened(SectorAPI sector, AtlasGenerator generator, AtlasCell cell) {
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null && special.kind().oneAttempt()) {
            AtlasState.get(sector).add(cell, AtlasState.ATTEMPTED);
        }
    }
}
