package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemStore;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.items.StarChart;
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
        PLAYER_IN_SYSTEM,
        NO_CHART,
        NOT_REACHABLE,
        FAILED
    }

    private ChartRunActions() {
    }

    public static OpenResult openBesidePlayer(SectorAPI sector, int entriesAllowed, Random random) {
        return openBesidePlayer(sector, entriesAllowed, random, ChartLayoutRequest.randomFrom(random));
    }

    public static OpenResult openBesidePlayer(SectorAPI sector, int entriesAllowed, Random random, ChartLayoutRequest layoutRequest) {
        return openBesidePlayer(sector, entriesAllowed, random, layoutRequest, null);
    }

    public static OpenResult openWithChart(SectorAPI sector, int entriesAllowed, Random random, StarChart chart) {
        if (chart == null || chart.definition() == null) {
            return OpenResult.NO_CHART;
        }
        return openBesidePlayer(sector, entriesAllowed, random, ChartLayoutRequest.forChart(chart), ItemData.of(chart).encode());
    }

    public static OpenResult openFromAtlas(SectorAPI sector, int entriesAllowed, Random random, ItemStore store, Item storedChart,
                                           AtlasCell cell) {
        if (!(storedChart instanceof StarChart chart) || chart.definition() == null || store == null || !store.holdsStarCharts()
                || store.find(chart.id()) != chart || cell == null) {
            return OpenResult.NO_CHART;
        }
        AtlasGenerator generator = AtlasRuntime.generator(sector);
        AtlasState state = AtlasState.get(sector);
        if (!AtlasOpening.check(generator, state, cell, chart).allowsOpening()) {
            return OpenResult.NOT_REACHABLE;
        }
        AtlasRunNode runNode = AtlasOpening.runNode(generator, state, cell);
        OpenResult result = openBesidePlayer(sector, entriesAllowed, random, AtlasOpening.request(generator, state, cell, chart, runNode),
                ItemData.of(chart).encode(), runNode);
        if (result == OpenResult.OPENED) {
            store.remove(chart);
            AtlasProgress.opened(sector, generator, cell);
        }
        return result;
    }

    public static OpenResult openFromStore(SectorAPI sector, int entriesAllowed, Random random, ItemStore store, Item storedChart) {
        if (!(storedChart instanceof StarChart chart) || store == null || !store.holdsStarCharts() || store.find(chart.id()) != chart) {
            return OpenResult.NO_CHART;
        }
        OpenResult result = openWithChart(sector, entriesAllowed, random, chart);
        if (result == OpenResult.OPENED) {
            store.remove(chart);
        }
        return result;
    }

    private static OpenResult openBesidePlayer(SectorAPI sector, int entriesAllowed, Random random, ChartLayoutRequest layoutRequest,
                                               String chartData) {
        return openBesidePlayer(sector, entriesAllowed, random, layoutRequest, chartData, null);
    }

    private static OpenResult openBesidePlayer(SectorAPI sector, int entriesAllowed, Random random, ChartLayoutRequest layoutRequest,
                                               String chartData, AtlasRunNode atlasNode) {
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
        if (origin.hasTag(ChartIds.SYSTEM_TAG)) {
            return OpenResult.PLAYER_IN_SYSTEM;
        }
        Vector2f playerLocation = playerFleet.getLocation();
        ChartRun run = new ChartRun(newRunId(random), ChartIds.locationIdOf(origin), playerLocation.x, playerLocation.y, entriesAllowed);
        run.recordChart(chartData);
        run.recordAtlasNode(atlasNode);
        ChartRuns.store(run);
        try {
            ChartSystemFactory.ChartSystem created = ChartSystemFactory.createSystem(sector, run.runId(), layoutRequest);
            run.recordLayout(created.layout());
            run.recordContent(created.content());
            ChartSystemFactory.addOuterWormhole(origin, playerLocation, run.runId());
            ChartObjectiveIntel.addFor(sector, run);
            return OpenResult.OPENED;
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Opening Star Chart run " + run.runId() + " failed; cleaning up what was created", e);
            run.recordAtlasNode(null);
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
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        if (system == null) {
            run.beginClosing();
            return false;
        }
        SectorEntityToken entry = ChartSystemFactory.entryPoint(system, system.getEntityById(run.innerWormholeId()));
        ChartRunScript.blockEscapeAbilities(playerFleet);
        sector.doHyperspaceTransition(playerFleet, outerWormhole, new JumpDestination(entry, system.getName()));
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
        if (outer != null && outer.getContainingLocation() != null && !outer.getContainingLocation().hasTag(ChartIds.SYSTEM_TAG)) {
            Vector2f outerLocation = outer.getLocation();
            return outer.getContainingLocation().createToken(outerLocation.x + ChartSystemFactory.ENTRY_OFFSET, outerLocation.y);
        }
        LocationAPI origin = ChartIds.resolveLocation(sector, run.originLocationId());
        if (origin != null && !origin.hasTag(ChartIds.SYSTEM_TAG)) {
            return origin.createToken(run.originX(), run.originY());
        }
        LocationAPI respawn = sector.getRespawnLocation();
        if (respawn != null && !respawn.hasTag(ChartIds.SYSTEM_TAG) && sector.getRespawnCoordinates() != null) {
            return respawn.createToken(sector.getRespawnCoordinates());
        }
        LocationAPI hyperspace = sector.getHyperspace();
        return hyperspace == null ? null : hyperspace.createToken(0f, 0f);
    }

    public static boolean collapseOuter(ChartRun run) {
        return run.collapseOuter();
    }
}
