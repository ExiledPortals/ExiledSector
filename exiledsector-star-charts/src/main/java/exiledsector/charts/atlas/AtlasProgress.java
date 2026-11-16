package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.wormhole.GatewayUpgradeRewards;
import exiledsector.items.ItemUnlock;
import org.apache.log4j.Logger;

public final class AtlasProgress {

    private static final Logger LOG = Logger.getLogger(AtlasProgress.class);

    private AtlasProgress() {
    }

    public static void completed(SectorAPI sector, ChartRun run) {
        completed(sector, run, run == null ? null : OfficerFleets.recordIdOf(run.atlasNode()));
    }

    public static void completed(SectorAPI sector, ChartRun run, Integer beatenOfficerId) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (sector == null || node == null) {
            return;
        }
        try {
            AtlasState state = AtlasState.get(sector);
            AtlasGenerator generator = AtlasRuntime.generator(sector);
            AtlasCell cell = AtlasCell.unpack(node.cell());
            boolean newlyCompleted = !state.isCompleted(cell);
            AtlasCell holderBeforeRunEnd = state.commandCentreOf(cell);
            ChartAnomalies.runCompleted(sector, run);
            boolean changedHands = changedHands(holderBeforeRunEnd, state.commandCentreOf(cell));
            complete(generator, state, cell, run.tier(), changedHands);
            tierCleared(sector, run);
            if (newlyCompleted) {
                takeOfficerTurn(generator, state, run, beatenOfficerId);
            }
            AtlasReach.refreshReveal(generator, state);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Recording the completed atlas system for run " + run.runId() + " failed", e);
        }
    }

    private static void tierCleared(SectorAPI sector, ChartRun run) {
        try {
            ItemUnlock.recordChartCleared(sector, run.tier());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Recording the cleared Star Chart tier for run " + run.runId() + " failed", e);
        }
        try {
            GatewayUpgradeRewards.completed(sector, run.tier());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Granting the Wormhole Gateway upgrade for run " + run.runId() + " failed", e);
        }
    }

    static boolean changedHands(AtlasCell holderBefore, AtlasCell holderAfter) {
        return holderAfter != null && !holderAfter.equals(holderBefore);
    }

    private static void takeOfficerTurn(AtlasGenerator generator, AtlasState state, ChartRun run, Integer beatenOfficerId) {
        try {
            OfficerTurns.bossKilled(generator, state, beatenOfficerId);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Taking the Atlas officer turn for run " + run.runId() + " failed", e);
        }
    }

    static void complete(AtlasGenerator generator, AtlasState state, AtlasCell cell, int tier) {
        complete(generator, state, cell, tier, false);
    }

    static void complete(AtlasGenerator generator, AtlasState state, AtlasCell cell, int tier, boolean keepsNewClaim) {
        if (state.isCompleted(cell)) {
            return;
        }
        state.add(cell, AtlasState.COMPLETED);
        state.spendUnchainedAnomalies(cell);
        state.clearTier(tier);
        state.addAtlasPoints(1);
        if (!keepsNewClaim) {
            AtlasTerritoryTransfer.vacate(generator, state, cell);
        }
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            special.kind().onCompleted(generator, state, cell, special);
        }
    }

    static void clearInfestation(AtlasGenerator generator, AtlasState state, AtlasCell infestationSourceCell, AtlasSpecial special) {
        int reach = (int) Math.ceil(special.radius()) + 1;
        for (AtlasCell cell : infestationSourceCell.around(reach)) {
            if (!generator.isVoid(cell) && infestationSourceCell.equals(generator.infestationSource(cell))) {
                state.add(cell, AtlasState.INFESTATION_CLEARED);
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
        boolean atlasRun = sector != null && run != null && run.atlasNode() != null;
        if (atlasRun) {
            ChartAnomalies.runClosed(sector, run);
        }
        closedWithoutObjective(sector, run);
        if (atlasRun) {
            handOverCommission(sector, run);
        }
    }

    private static void handOverCommission(SectorAPI sector, ChartRun run) {
        try {
            AtlasState state = AtlasState.get(sector);
            TerritoryContext context = TerritoryContext.current().withoutOpenRun();
            if (CommissionHandover.isNeeded(state, context.excludedFactionId())) {
                AtlasGenerator generator = AtlasRuntime.generator(sector);
                OfficerTurns.handOverCommission(generator, state, AtlasReach.visible(generator, state), context);
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Handing over the commission faction's Atlas territories after run " + run.runId() + " failed", e);
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
        state.add(cell, AtlasState.FAILED | AtlasState.ANOMALIES_SPENT);
        state.spendInsertedAnomalies(cell);
    }

    public static void opened(SectorAPI sector, AtlasGenerator generator, AtlasCell cell) {
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null && special.kind().oneAttempt()) {
            AtlasState.get(sector).add(cell, AtlasState.ATTEMPTED);
        }
    }
}
