package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.enc.StrandedGiveTJScript;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ChartRunCloser {

    private static final Logger LOG = Logger.getLogger(ChartRunCloser.class);

    static final List<CloseStep> STEPS = List.of(
            new CloseStep("clear the player's interaction target and course", ChartRunCloser::clearPlayerTargetAndCourse),
            new CloseStep("reset the campaign map location", ChartRunCloser::resetCampaignMapLocation),
            new CloseStep("move the respawn location out of the pocket", ChartRunCloser::moveRespawnLocationOut),
            new CloseStep("refresh the stranded-in-deep-space script", ChartRunCloser::refreshStrandedScript),
            new CloseStep("re-point the player's officers at the current fleet", ChartRunCloser::repointOfficers),
            new CloseStep("despawn the pocket's fleets", ChartRunCloser::despawnFleets),
            new CloseStep("end intel pointing into the pocket", ChartRunCloser::endIntel),
            new CloseStep("drop a pending Salvor's Tally for the pocket",
                    target -> ChartSalvorsTally.removePendingFor(target.sector(), target.pocket())),
            new CloseStep("remove the run's scripts, listeners and memory keys", ChartRunCloser::removeRunScriptsAndMemory),
            new CloseStep("remove the pocket's entities", ChartRunCloser::removePocketEntities),
            new CloseStep("remove the wormholes and tagged entities", ChartRunCloser::removeWormholesAndTaggedEntities),
            new CloseStep("remove the pocket system", ChartRunCloser::removePocketSystem),
            new CloseStep("delete the run record", ChartRunCloser::deleteRunRecord));

    private ChartRunCloser() {
    }

    public static int close(SectorAPI sector, ChartRun run) {
        StarSystemAPI pocket = sector.getStarSystem(run.pocketSystemId());
        return close(sector, run.runId(), pocket);
    }

    public static int close(SectorAPI sector, String runId, StarSystemAPI pocket) {
        CloseTarget target = new CloseTarget(sector, runId, pocket);
        int failedSteps = 0;
        for (CloseStep step : STEPS) {
            try {
                step.action().accept(target);
            } catch (RuntimeException | LinkageError e) {
                failedSteps++;
                LOG.warn("[ExiledSector] Closing Star Chart run " + runId + " could not " + step.name() + "; the remaining steps still run", e);
            }
        }
        LOG.info("[ExiledSector] Closed Star Chart run " + runId + (failedSteps == 0 ? "" : " with " + failedSteps + " failed step(s)"));
        return failedSteps;
    }

    public static void removeOuterWormhole(SectorAPI sector, ChartRun run) {
        removeEntity(sector.getEntityById(run.outerWormholeId()));
    }

    static boolean pointsIntoRun(SectorEntityToken entity, CloseTarget target) {
        if (entity == null) {
            return false;
        }
        if (target.pocket() != null && entity.getContainingLocation() == target.pocket()) {
            return true;
        }
        return ChartIds.belongsToRun(entity, target.runId());
    }

    static void clearPlayerTargetAndCourse(CloseTarget target) {
        CampaignFleetAPI playerFleet = target.sector().getPlayerFleet();
        if (playerFleet != null && pointsIntoRun(playerFleet.getInteractionTarget(), target)) {
            playerFleet.setInteractionTarget(null);
        }
        CampaignUIAPI campaignUi = target.sector().getCampaignUI();
        PersistentUIDataAPI uiData = target.sector().getUIData();
        SectorEntityToken plotted = uiData == null ? null : uiData.getCourseTarget();
        boolean courseIntoRun = pointsIntoRun(plotted, target)
                || campaignUi != null && pointsIntoRun(campaignUi.getCurrentCourseTarget(), target);
        if (!courseIntoRun) {
            return;
        }
        if (campaignUi != null) {
            campaignUi.clearLaidInCourse();
        }
        if (uiData != null && pointsIntoRun(uiData.getCourseTarget(), target)) {
            ChartUiData.forgetCourseTarget(uiData);
        }
    }

    static void resetCampaignMapLocation(CloseTarget target) {
        if (target.pocket() == null) {
            return;
        }
        Object uiData = target.sector().getUIData();
        if (ChartUiData.campaignMapLocation(uiData) == target.pocket()) {
            ChartUiData.forgetCampaignMapLocation(uiData);
        }
    }

    static void moveRespawnLocationOut(CloseTarget target) {
        if (target.pocket() != null && target.sector().getRespawnLocation() == target.pocket()) {
            target.sector().setRespawnLocation(target.sector().getHyperspace());
            if (target.sector().getRespawnCoordinates() != null) {
                target.sector().getRespawnCoordinates().set(0f, 0f);
            }
        }
    }

    static void refreshStrandedScript(CloseTarget target) {
        SectorAPI sector = target.sector();
        if (sector.hasScript(StrandedGiveTJScript.class)) {
            sector.removeScriptsOfClass(StrandedGiveTJScript.class);
            sector.addScript(new StrandedGiveTJScript());
        }
    }

    static void repointOfficers(CloseTarget target) {
        CampaignFleetAPI playerFleet = target.sector().getPlayerFleet();
        if (playerFleet == null || target.pocket() == null) {
            return;
        }
        repoint(playerFleet.getCommander(), playerFleet, target.pocket());
        if (playerFleet.getFleetData() == null) {
            return;
        }
        for (OfficerDataAPI officer : playerFleet.getFleetData().getOfficersCopy()) {
            repoint(officer.getPerson(), playerFleet, target.pocket());
        }
    }

    private static void repoint(PersonAPI person, CampaignFleetAPI playerFleet, StarSystemAPI pocket) {
        if (person == null) {
            return;
        }
        CampaignFleetAPI recordedFleet = person.getFleet();
        if (recordedFleet != null && recordedFleet != playerFleet && recordedFleet.getContainingLocation() == pocket) {
            person.setFleet(playerFleet);
        }
    }

    static void despawnFleets(CloseTarget target) {
        if (target.pocket() == null) {
            return;
        }
        CampaignFleetAPI playerFleet = target.sector().getPlayerFleet();
        for (CampaignFleetAPI fleet : new ArrayList<>(target.pocket().getFleets())) {
            if (fleet != playerFleet) {
                fleet.despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
            }
        }
    }

    static void endIntel(CloseTarget target) {
        IntelManagerAPI intelManager = target.sector().getIntelManager();
        if (intelManager == null) {
            return;
        }
        for (IntelInfoPlugin intel : new ArrayList<>(intelManager.getIntel())) {
            if (intelPointsIntoRun(intel, target)) {
                if (intel instanceof BaseIntelPlugin baseIntel) {
                    baseIntel.endImmediately();
                }
                intelManager.removeIntel(intel);
            }
        }
        for (IntelInfoPlugin queued : new ArrayList<>(intelManager.getCommQueue())) {
            if (intelPointsIntoRun(queued, target)) {
                intelManager.unqueueIntel(queued);
            }
        }
    }

    private static boolean intelPointsIntoRun(IntelInfoPlugin intel, CloseTarget target) {
        try {
            return pointsIntoRun(intel.getMapLocation(null), target);
        } catch (RuntimeException e) {
            return false;
        }
    }

    static void removeRunScriptsAndMemory(CloseTarget target) {
        SectorAPI sector = target.sector();
        for (EveryFrameScript script : new ArrayList<>(sector.getScripts())) {
            if (script instanceof ChartRunBound bound && bound.belongsToRun(target.runId())) {
                sector.removeScript(script);
            }
        }
        for (EveryFrameScript script : new ArrayList<>(sector.getTransientScripts())) {
            if (script instanceof ChartRunBound bound && bound.belongsToRun(target.runId())) {
                sector.removeTransientScript(script);
            }
        }
        for (CampaignEventListener listener : new ArrayList<>(sector.getAllListeners())) {
            if (listener instanceof ChartRunBound bound && bound.belongsToRun(target.runId())) {
                sector.removeListener(listener);
            }
        }
        if (sector.getListenerManager() != null) {
            for (ChartRunBound listener : new ArrayList<>(sector.getListenerManager().getListeners(ChartRunBound.class))) {
                if (listener.belongsToRun(target.runId())) {
                    sector.getListenerManager().removeListener(listener);
                }
            }
        }
        unsetChartKeys(sector.getMemoryWithoutUpdate(), target.runId());
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet != null) {
            unsetChartKeys(playerFleet.getMemoryWithoutUpdate(), target.runId());
        }
    }

    private static void unsetChartKeys(MemoryAPI memory, String runId) {
        if (memory == null || runId == null) {
            return;
        }
        String runPrefix = ChartIds.MEMORY_PREFIX + runId;
        for (String key : new ArrayList<>(memory.getKeys())) {
            if (key.startsWith(runPrefix)) {
                memory.unset(key);
            }
        }
    }

    static void removePocketEntities(CloseTarget target) {
        StarSystemAPI pocket = target.pocket();
        if (pocket == null) {
            return;
        }
        CampaignFleetAPI playerFleet = target.sector().getPlayerFleet();
        for (SectorEntityToken entity : new ArrayList<>(pocket.getAllEntities())) {
            if (entity != playerFleet) {
                pocket.removeEntity(entity);
            }
        }
    }

    static void removeWormholesAndTaggedEntities(CloseTarget target) {
        SectorAPI sector = target.sector();
        String runId = target.runId();
        if (runId == null) {
            return;
        }
        removeEntity(sector.getEntityById(ChartIds.outerWormholeId(runId)));
        removeEntity(sector.getEntityById(ChartIds.innerWormholeId(runId)));
        removeTagged(sector.getHyperspace(), runId);
        removeHyperspacePresence(sector.getHyperspace(), target.pocket());
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (system != target.pocket()) {
                removeTagged(system, runId);
            }
        }
    }

    static void removeHyperspacePresence(LocationAPI hyperspace, StarSystemAPI pocket) {
        if (hyperspace == null || pocket == null) {
            return;
        }
        List<SectorEntityToken> pointingIn = new ArrayList<>();
        for (SectorEntityToken candidate : hyperspace.getJumpPoints()) {
            if (candidate instanceof JumpPointAPI jumpPoint && leadsInto(jumpPoint, pocket)) {
                pointingIn.add(candidate);
            }
        }
        for (NascentGravityWellAPI well : hyperspace.getGravityWells()) {
            if (well.getTarget() != null && well.getTarget().getStarSystem() == pocket) {
                pointingIn.add(well);
            }
        }
        if (pocket.getHyperspaceAnchor() != null) {
            pointingIn.add(pocket.getHyperspaceAnchor());
        }
        pointingIn.forEach(hyperspace::removeEntity);
    }

    private static boolean leadsInto(JumpPointAPI jumpPoint, StarSystemAPI pocket) {
        for (JumpPointAPI.JumpDestination destination : jumpPoint.getDestinations()) {
            if (destination.getDestination() != null && destination.getDestination().getStarSystem() == pocket) {
                return true;
            }
        }
        return false;
    }

    private static void removeTagged(LocationAPI location, String runId) {
        if (location == null) {
            return;
        }
        for (SectorEntityToken entity : new ArrayList<>(location.getEntitiesWithTag(ChartIds.runTag(runId)))) {
            location.removeEntity(entity);
        }
    }

    static void removeEntity(SectorEntityToken entity) {
        if (entity != null && entity.getContainingLocation() != null) {
            entity.getContainingLocation().removeEntity(entity);
        }
    }

    static void removePocketSystem(CloseTarget target) {
        StarSystemAPI pocket = target.pocket();
        if (pocket != null && target.sector().getStarSystems().contains(pocket)) {
            target.sector().removeStarSystemNextFrame(pocket);
        }
    }

    static void deleteRunRecord(CloseTarget target) {
        ChartRun current = ChartRuns.currentOrNull();
        if (current != null && current.runId().equals(target.runId())) {
            current.markClosed();
            ChartRuns.delete(current.runId());
        }
    }

    record CloseTarget(SectorAPI sector, String runId, StarSystemAPI pocket) {
    }

    record CloseStep(String name, Consumer<CloseTarget> action) {
    }
}
