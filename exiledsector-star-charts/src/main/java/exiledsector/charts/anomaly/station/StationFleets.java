package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantAssignmentAI;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;

import java.util.Random;
import java.util.function.BiConsumer;

final class StationFleets {

    static final String SOURCE_ID_KEY = "$sourceId";

    private StationFleets() {
    }

    record Launch(String factionId, int maxShipSize, BiConsumer<Random, CampaignFleetAPI> dress) {
    }

    static CampaignFleetAPI spawn(SectorEntityToken source, Launch launch, String runId, int runTier, int fleetsLost, long fleetSeed) {
        ChartRun run = ChartRuns.live(runId);
        LocationAPI location = source == null ? null : source.getContainingLocation();
        if (run == null || location == null || !source.isAlive()) {
            return null;
        }
        Random random = new Random(fleetSeed);
        ChartGenContext context = ChartRunEffects.contextOf(run);
        ChartTier tier = ChartAnomalyFleets.tier(runTier, context);
        ChartAnomalyFleets.Spec spec = ChartAnomalyFleets.combat(launch.factionId(), FleetTypes.PATROL_MEDIUM, tier,
                tier.fleetSize() * StationRules.fleetShare(fleetsLost));
        CampaignFleetAPI fleet = launch.maxShipSize() > StationRules.NO_SHIP_SIZE_CAP
                ? ChartAnomalyFleets.create(spec, random, launch.maxShipSize(), true) : ChartAnomalyFleets.create(spec, random);
        if (fleet == null) {
            return null;
        }
        location.addEntity(fleet);
        fleet.setLocation(source.getLocation().x, source.getLocation().y);
        fleet.setFacing(random.nextFloat() * 360f);
        launch.dress().accept(random, fleet);
        ChartAnomalyFleets.prepare(fleet, runId, runTier, fleetSeed, false, context);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        fleet.getMemoryWithoutUpdate().set(SOURCE_ID_KEY, source.getId());
        if (location instanceof StarSystemAPI system) {
            fleet.addScript(new RemnantAssignmentAI(fleet, system, source));
        }
        return fleet;
    }
}
