package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.effect.ChartFleetConditions;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.generation.ChartSystemFactory;
import exiledsector.charts.wormhole.ChartWormholePlacement;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.objective.ChartObjectives;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ChartLoadSweep {

    private static final Logger LOG = Logger.getLogger(ChartLoadSweep.class);

    public enum SystemDecision {
        KEEP,
        ADOPT,
        PURGE,
        LEAVE_FOR_NOW
    }

    private ChartLoadSweep() {
    }

    public static void sweep(SectorAPI sector) {
        runSafely("repair the run record", () -> repairRunRecord(sector));
        runSafely("purge orphan systems", () -> purgeOrphanSystems(sector));
        runSafely("remove leftover chart entities", () -> removeLeftoverEntities(sector));
        runSafely("remove orphan chart intel", () -> removeOrphanIntel(sector));
        runSafely("lift chart effects outside the run's system", () -> liftEffectsOutsideSystem(sector));
        runSafely("catch up on the objective", () -> ChartObjectives.reconcile(sector));
    }

    public static void liftEffectsOutsideSystem(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        ChartFleetConditions.clear(playerFleet);
        if (playerFleet == null) {
            return;
        }
        ChartRun run = ChartRuns.currentOrNull();
        StarSystemAPI system = run == null ? null : sector.getStarSystem(run.systemId());
        boolean insideRunSystem = system != null && run.runState().keepsRunEffects() && playerFleet.getContainingLocation() == system;
        if (!insideRunSystem) {
            ChartRunEffects.removeFrom(playerFleet);
        }
    }

    static SystemDecision decide(StarSystemAPI system, ChartRun run, boolean playerInside) {
        boolean ownedByRun = run != null && run.systemId().equals(system.getOptionalUniqueId());
        if (ownedByRun && run.runState().isLive()) {
            return SystemDecision.KEEP;
        }
        if (playerInside) {
            return run == null || ownedByRun || !run.runState().isLive() ? SystemDecision.ADOPT : SystemDecision.LEAVE_FOR_NOW;
        }
        return SystemDecision.PURGE;
    }

    static void repairRunRecord(SectorAPI sector) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null) {
            return;
        }
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        boolean playerInside = system != null && playerFleet != null && playerFleet.getContainingLocation() == system;
        if (system == null) {
            LOG.warn("[ExiledSector] Star Chart run " + run.runId() + " lost its system; closing it");
            run.beginClosing();
            AtlasProgress.closed(sector, run);
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

    static void purgeOrphanSystems(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        LocationAPI playerLocation = playerFleet == null ? null : playerFleet.getContainingLocation();
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (!system.hasTag(ChartIds.SYSTEM_TAG)) {
                continue;
            }
            ChartRun run = ChartRuns.currentOrNull();
            SystemDecision decision = decide(system, run, playerLocation == system);
            String runId = ChartIds.runIdOf(system);
            switch (decision) {
                case PURGE -> {
                    LOG.info("[ExiledSector] Purging Star Chart system " + system.getOptionalUniqueId() + " with no live run");
                    ChartRunCloser.close(sector, runId, system);
                }
                case ADOPT -> {
                    ChartSystemFactory.ensureIsolationTags(system);
                    adopt(sector, system, runId, run);
                }
                case KEEP -> {
                    ChartSystemFactory.ensureIsolationTags(system);
                    if (runId != null) {
                        ensureInnerWormhole(sector, system, runId);
                    }
                }
                case LEAVE_FOR_NOW -> LOG.warn("[ExiledSector] The player is inside Star Chart system " + system.getOptionalUniqueId()
                        + " while another run is open; leaving it until they leave");
                default -> {
                }
            }
        }
    }

    private static void adopt(SectorAPI sector, StarSystemAPI system, String runId, ChartRun staleRun) {
        if (runId == null) {
            return;
        }
        if (staleRun != null && staleRun.runId().equals(runId)) {
            staleRun.resumeInside();
            ensureInnerWormhole(sector, system, runId);
            return;
        }
        LocationAPI origin = sector.getRespawnLocation();
        Vector2f originCoordinates = sector.getRespawnCoordinates();
        if (origin == null || origin == system || originCoordinates == null) {
            origin = sector.getHyperspace();
            originCoordinates = new Vector2f();
        }
        ChartRun adopted = ChartRun.adoptedWhileInside(runId, ChartIds.locationIdOf(origin), originCoordinates.x, originCoordinates.y);
        if (!adopted.systemId().equals(system.getOptionalUniqueId())) {
            return;
        }
        LOG.warn("[ExiledSector] The player is inside Star Chart system " + system.getOptionalUniqueId() + " with no run record; adopting it");
        ChartRuns.replace(adopted);
        ensureInnerWormhole(sector, system, runId);
    }

    private static void ensureInnerWormhole(SectorAPI sector, StarSystemAPI system, String runId) {
        if (system.getEntityById(ChartIds.innerWormholeId(runId)) == null) {
            Vector2f clearSpot = ChartWormholePlacement.choose(system, new Random(runId.hashCode()));
            ChartSystemFactory.addInnerWormhole(system, runId, clearSpot);
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
            if (location == playerLocation && location.hasTag(ChartIds.SYSTEM_TAG)) {
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
        ChartRun run = ChartRuns.currentOrNull();
        String liveRunId = run != null && run.runState().isLive() ? run.runId() : null;
        return ChartRunCloser.endIntelWhere(sector, intelManager -> intelManager.getIntel(ChartObjectiveIntel.class),
                intel -> intel instanceof ChartObjectiveIntel objectiveIntel && !objectiveIntel.belongsToRun(liveRunId));
    }

    private static void runSafely(String stepName, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[ExiledSector] The Star Chart load sweep could not " + stepName, e);
        }
    }
}
