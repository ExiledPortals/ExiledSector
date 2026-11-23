package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.content.ChartFleetOrders;
import exiledsector.charts.run.ChartRun;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class BossVengeance {

    private static final Logger LOG = Logger.getLogger(BossVengeance.class);

    private BossVengeance() {
    }

    static void begin(SectorAPI sector, ChartRun run, CampaignFleetAPI fallenFleet) {
        try {
            StarSystemAPI system = sector.getStarSystem(run.systemId());
            begin(system, run.runId(), bossAt(system, fallenFleet, sector.getPlayerFleet()));
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Rallying the Star Chart fleets to avenge the boss of run " + run.runId() + " failed", e);
        }
    }

    static boolean begin(StarSystemAPI system, String runId, Vector2f bossAt) {
        if (system == null || bossAt == null) {
            return false;
        }
        List<CampaignFleetAPI> avengers = new ArrayList<>();
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (joins(fleet, runId)) {
                avengers.add(fleet);
            }
        }
        if (avengers.isEmpty()) {
            return false;
        }
        SectorEntityToken rallyPoint = system.createToken(bossAt.x, bossAt.y);
        for (CampaignFleetAPI avenger : avengers) {
            ChartFleetMarks.markAvenger(avenger);
            avenger.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        }
        system.addScript(new VengeanceScript(system, rallyPoint));
        return true;
    }

    static boolean joins(CampaignFleetAPI fleet, String runId) {
        return ChartFleetOrders.canTakeOrders(fleet) && !fleet.isEmpty() && ChartIds.belongsToRun(fleet, runId)
                && !ChartFleetMarks.isAnomalyFleet(fleet) && !ChartFleetMarks.isAvenger(fleet);
    }

    static Vector2f bossAt(StarSystemAPI system, CampaignFleetAPI fallenFleet, CampaignFleetAPI playerFleet) {
        if (system == null) {
            return null;
        }
        for (CampaignFleetAPI witness : new CampaignFleetAPI[]{fallenFleet, playerFleet}) {
            if (witness != null && witness.getContainingLocation() == system && witness.getLocation() != null) {
                return new Vector2f(witness.getLocation());
            }
        }
        return null;
    }
}
