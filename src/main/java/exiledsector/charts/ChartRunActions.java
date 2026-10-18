package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public final class ChartRunActions {

    private static final Logger LOG = Logger.getLogger(ChartRunActions.class);
    private static final int RUN_ID_RADIX = 36;

    public enum OpenResult {
        OPENED,
        RUN_EXISTS,
        NO_PLAYER_FLEET,
        PLAYER_IN_TRANSITION,
        PLAYER_IN_POCKET,
        FAILED
    }

    private ChartRunActions() {
    }

    public static OpenResult openBesidePlayer(SectorAPI sector, int entriesAllowed, Random random) {
        return openBesidePlayer(sector, entriesAllowed, random, ChartLayoutRequest.randomFrom(random));
    }

    public static OpenResult openBesidePlayer(SectorAPI sector, int entriesAllowed, Random random, ChartLayoutRequest layoutRequest) {
        if (ChartRuns.hasRun()) {
            return OpenResult.RUN_EXISTS;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.getContainingLocation() == null) {
            return OpenResult.NO_PLAYER_FLEET;
        }
        if (playerFleet.isInHyperspaceTransition()) {
            return OpenResult.PLAYER_IN_TRANSITION;
        }
        LocationAPI origin = playerFleet.getContainingLocation();
        if (origin.hasTag(ChartIds.POCKET_TAG)) {
            return OpenResult.PLAYER_IN_POCKET;
        }
        Vector2f playerLocation = playerFleet.getLocation();
        ChartRun run = new ChartRun(newRunId(random), ChartIds.locationIdOf(origin), playerLocation.x, playerLocation.y, entriesAllowed);
        ChartRuns.store(run);
        try {
            run.recordLayout(ChartPocketFactory.createPocket(sector, run.runId(), layoutRequest).layout());
            ChartPocketFactory.addOuterWormhole(origin, playerLocation, run.runId());
            return OpenResult.OPENED;
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Opening Star Chart run " + run.runId() + " failed; cleaning up what was created", e);
            run.beginClosing();
            ChartRunCloser.close(sector, run);
            return OpenResult.FAILED;
        }
    }

    static String newRunId(Random random) {
        return Long.toString(random.nextLong() & Long.MAX_VALUE, RUN_ID_RADIX);
    }

    public static boolean enter(SectorAPI sector, ChartRun run, SectorEntityToken outerWormhole) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (!run.canEnter() || playerFleet == null || playerFleet.isInHyperspaceTransition()) {
            return false;
        }
        StarSystemAPI pocket = sector.getStarSystem(run.pocketSystemId());
        if (pocket == null) {
            run.beginClosing();
            return false;
        }
        SectorEntityToken entry = ChartPocketFactory.entryPoint(pocket, pocket.getEntityById(run.innerWormholeId()));
        ChartRunScript.blockEscapeAbilities(playerFleet);
        sector.doHyperspaceTransition(playerFleet, outerWormhole, new JumpDestination(entry, pocket.getName()));
        return true;
    }

    public static boolean returnToOrigin(SectorAPI sector, ChartRun run, SectorEntityToken innerWormhole) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.isInHyperspaceTransition()) {
            return false;
        }
        SectorEntityToken arrival = returnPoint(sector, run);
        if (arrival == null) {
            return false;
        }
        run.requestReturn();
        sector.doHyperspaceTransition(playerFleet, innerWormhole, new JumpDestination(arrival, arrival.getContainingLocation().getName()));
        return true;
    }

    static SectorEntityToken returnPoint(SectorAPI sector, ChartRun run) {
        SectorEntityToken outer = run.isOuterCollapsed() ? null : sector.getEntityById(run.outerWormholeId());
        if (outer != null && outer.getContainingLocation() != null && !outer.getContainingLocation().hasTag(ChartIds.POCKET_TAG)) {
            Vector2f outerLocation = outer.getLocation();
            return outer.getContainingLocation().createToken(outerLocation.x + ChartPocketFactory.ENTRY_OFFSET, outerLocation.y);
        }
        LocationAPI origin = ChartIds.resolveLocation(sector, run.originLocationId());
        if (origin != null && !origin.hasTag(ChartIds.POCKET_TAG)) {
            return origin.createToken(run.originX(), run.originY());
        }
        LocationAPI respawn = sector.getRespawnLocation();
        if (respawn != null && !respawn.hasTag(ChartIds.POCKET_TAG) && sector.getRespawnCoordinates() != null) {
            return respawn.createToken(sector.getRespawnCoordinates());
        }
        LocationAPI hyperspace = sector.getHyperspace();
        return hyperspace == null ? null : hyperspace.createToken(0f, 0f);
    }

    public static boolean collapseOuter(ChartRun run) {
        return run.collapseOuter();
    }
}
