package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.AtlasRunNode;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
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
            if (node.skirmishAttackerId() != null && sector.getFaction(node.skirmishAttackerId()) != null) {
                captureForWinner(generator, state, cell, node);
            }
            AtlasReach.refreshReveal(generator, state);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Recording the completed atlas system for run " + run.runId() + " failed", e);
        }
    }

    static void captureForWinner(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasRunNode node) {
        if (node.skirmishSide() == SkirmishSide.ATTACKER && node.skirmishAttackerId() != null) {
            state.capture(generator, cell, node.skirmishAttackerId());
        }
    }

    static void complete(AtlasGenerator generator, AtlasState state, AtlasCell cell, int tier) {
        if (state.isCompleted(cell)) {
            return;
        }
        state.add(cell, AtlasState.COMPLETED);
        state.spendInsertedAnomalies(cell);
        state.clearTier(tier);
        AtlasSpecial special = generator.specialAt(cell);
        if (special == null) {
            return;
        }
        if (special.kind() == AtlasSpecial.Kind.SENSOR_ARRAY) {
            state.addAtlasPoints(1);
        } else if (special.infests()) {
            cleanse(generator, state, cell, special);
            state.addAtlasPoints(1);
        } else if (special.kind() == AtlasSpecial.Kind.BASTION) {
            state.addKeyFragment();
            state.addAtlasPoints(1);
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
        if (AtlasSpecial.ofKind(special, AtlasSpecial.Kind.BASTION)) {
            AtlasState.get(sector).add(cell, AtlasState.ATTEMPTED);
        }
    }
}
