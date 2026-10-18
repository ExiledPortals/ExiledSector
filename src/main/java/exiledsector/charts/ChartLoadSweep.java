package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.IntelDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ChartLoadSweep {

    private static final Logger LOG = Logger.getLogger(ChartLoadSweep.class);

    public enum PocketDecision {
        KEEP,
        ADOPT,
        PURGE,
        LEAVE_FOR_NOW
    }

    private ChartLoadSweep() {
    }

    public static void sweep(SectorAPI sector) {
        runSafely("repair the run record", () -> repairRunRecord(sector));
        runSafely("purge orphan pockets", () -> purgeOrphanPockets(sector));
        runSafely("remove leftover chart entities", () -> removeLeftoverEntities(sector));
        runSafely("remove orphan chart intel", () -> removeOrphanIntel(sector));
        runSafely("catch up on the objective", () -> ChartObjectives.reconcile(sector));
    }

    static PocketDecision decide(StarSystemAPI pocket, ChartRun run, boolean playerInside) {
        boolean ownedByRun = run != null && run.pocketSystemId().equals(pocket.getOptionalUniqueId());
        if (ownedByRun && run.runState().isLive()) {
            return PocketDecision.KEEP;
        }
        if (playerInside) {
            return run == null || ownedByRun || !run.runState().isLive() ? PocketDecision.ADOPT : PocketDecision.LEAVE_FOR_NOW;
        }
        return PocketDecision.PURGE;
    }

    static void repairRunRecord(SectorAPI sector) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null) {
            return;
        }
        StarSystemAPI pocket = sector.getStarSystem(run.pocketSystemId());
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        boolean playerInside = pocket != null && playerFleet != null && playerFleet.getContainingLocation() == pocket;
        if (pocket == null) {
            LOG.warn("[ExiledSector] Star Chart run " + run.runId() + " lost its pocket; closing it");
            run.beginClosing();
            ChartRunCloser.close(sector, run.runId(), null);
            return;
        }
        if (playerInside) {
            run.resumeInside();
        } else if (run.runState() == ChartRunState.OPEN && sector.getEntityById(run.outerWormholeId()) == null) {
            LOG.warn("[ExiledSector] Star Chart run " + run.runId() + " lost its outer wormhole; closing it");
            run.collapseOuter();
        }
    }

    static void purgeOrphanPockets(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        LocationAPI playerLocation = playerFleet == null ? null : playerFleet.getContainingLocation();
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (!system.hasTag(ChartIds.POCKET_TAG)) {
                continue;
            }
            ChartRun run = ChartRuns.currentOrNull();
            PocketDecision decision = decide(system, run, playerLocation == system);
            String runId = ChartIds.runIdOf(system);
            switch (decision) {
                case PURGE -> {
                    LOG.info("[ExiledSector] Purging Star Chart pocket " + system.getOptionalUniqueId() + " with no live run");
                    ChartRunCloser.close(sector, runId, system);
                }
                case ADOPT -> {
                    ChartPocketFactory.ensureIsolationTags(system);
                    adopt(sector, system, runId, run);
                }
                case KEEP -> ChartPocketFactory.ensureIsolationTags(system);
                case LEAVE_FOR_NOW -> LOG.warn("[ExiledSector] The player is inside Star Chart pocket " + system.getOptionalUniqueId()
                        + " while another run is open; leaving it until they leave");
                default -> {
                }
            }
        }
    }

    private static void adopt(SectorAPI sector, StarSystemAPI pocket, String runId, ChartRun staleRun) {
        if (runId == null) {
            return;
        }
        if (staleRun != null && staleRun.runId().equals(runId)) {
            staleRun.resumeInside();
            ensureInnerWormhole(sector, pocket, runId);
            return;
        }
        LocationAPI origin = sector.getRespawnLocation();
        Vector2f originCoordinates = sector.getRespawnCoordinates();
        if (origin == null || origin == pocket || originCoordinates == null) {
            origin = sector.getHyperspace();
            originCoordinates = new Vector2f();
        }
        ChartRun adopted = ChartRun.adoptedWhileInside(runId, ChartIds.locationIdOf(origin), originCoordinates.x, originCoordinates.y);
        if (!adopted.pocketSystemId().equals(pocket.getOptionalUniqueId())) {
            return;
        }
        LOG.warn("[ExiledSector] The player is inside Star Chart pocket " + pocket.getOptionalUniqueId() + " with no run record; adopting it");
        ChartRuns.replace(adopted);
        ensureInnerWormhole(sector, pocket, runId);
    }

    private static void ensureInnerWormhole(SectorAPI sector, StarSystemAPI pocket, String runId) {
        if (pocket.getEntityById(ChartIds.innerWormholeId(runId)) == null) {
            Vector2f clearSpot = ChartWormholePlacement.choose(pocket, new Random(runId.hashCode()));
            ChartPocketFactory.addInnerWormhole(pocket, runId, clearSpot);
        }
    }

    static void removeLeftoverEntities(SectorAPI sector) {
        ChartRun run = ChartRuns.currentOrNull();
        String liveRunId = run != null && run.runState().isLive() ? run.runId() : null;
        List<LocationAPI> locations = new ArrayList<>(sector.getStarSystems());
        if (sector.getHyperspace() != null) {
            locations.add(sector.getHyperspace());
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        LocationAPI playerLocation = playerFleet == null ? null : playerFleet.getContainingLocation();
        for (LocationAPI location : locations) {
            if (location == playerLocation && location.hasTag(ChartIds.POCKET_TAG)) {
                continue;
            }
            for (SectorEntityToken entity : new ArrayList<>(location.getEntitiesWithTag(ChartIds.ENTITY_TAG))) {
                if (entity != playerFleet && !ChartIds.belongsToRun(entity, liveRunId)) {
                    location.removeEntity(entity);
                }
            }
        }
    }

    static int removeOrphanIntel(SectorAPI sector) {
        IntelManagerAPI intelManager = sector.getIntelManager();
        if (intelManager == null) {
            return 0;
        }
        ChartRun run = ChartRuns.currentOrNull();
        String liveRunId = run != null && run.runState().isLive() ? run.runId() : null;
        IntelDataAPI intelTab = sector.getIntel();
        if (intelTab != null && intelTab.getSelectedIntel() instanceof ChartObjectiveIntel selected && !selected.belongsToRun(liveRunId)) {
            intelTab.setSelectedIntel(null);
        }
        int removed = 0;
        for (IntelInfoPlugin intel : new ArrayList<>(intelManager.getIntel(ChartObjectiveIntel.class))) {
            if (intel instanceof ChartObjectiveIntel objectiveIntel && !objectiveIntel.belongsToRun(liveRunId)) {
                objectiveIntel.endImmediately();
                intelManager.removeIntel(intel);
                removed++;
            }
        }
        return removed;
    }

    private static void runSafely(String stepName, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[ExiledSector] The Star Chart load sweep could not " + stepName, e);
        }
    }
}
