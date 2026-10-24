package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.AtlasRunNode;
import exiledsector.charts.ChartRun;
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
            complete(generator, state, AtlasCell.unpack(node.cell()), run.tier());
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
        state.clearTier(tier);
        AtlasSpecial special = generator.specialAt(cell);
        if (special == null) {
            return;
        }
        if (AtlasReach.SENSOR_ARRAY.equals(special.kind())) {
            state.addAtlasPoints(1);
        } else if (special.infests()) {
            cleanse(generator, state, cell, special);
            state.addAtlasPoints(1);
        } else if (AtlasReach.BASTION.equals(special.kind())) {
            state.addKeyFragment();
            state.addAtlasPoints(1);
        }
    }

    static void cleanse(AtlasGenerator generator, AtlasState state, AtlasCell nexus, AtlasSpecial special) {
        int reach = (int) Math.ceil(special.radius()) + 1;
        for (AtlasCell cell : nexus.around(reach)) {
            if (special.id().equals(generator.node(cell).infestationId())) {
                state.add(cell, AtlasState.CLEANSED);
            }
        }
    }

    public static void closedWithoutObjective(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (sector == null || node == null || run.isObjectiveComplete()) {
            return;
        }
        AtlasState.get(sector).add(AtlasCell.unpack(node.cell()), AtlasState.FAILED | AtlasState.MARKERS_SPENT);
    }

    public static void opened(SectorAPI sector, AtlasGenerator generator, AtlasCell cell) {
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null && AtlasReach.BASTION.equals(special.kind())) {
            AtlasState.get(sector).add(cell, AtlasState.ATTEMPTED);
        }
    }
}
