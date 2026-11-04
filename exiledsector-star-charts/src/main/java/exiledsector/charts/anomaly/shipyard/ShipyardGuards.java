package exiledsector.charts.anomaly.shipyard;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.charts.anomaly.CoredLineup;
import exiledsector.charts.anomaly.CoredLineup.CoredShip;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;

import java.util.List;
import java.util.Random;

public final class ShipyardGuards {

    static final String CHRONOS_CORE = "rat_chronos_core";
    static final String COSMOS_CORE = "rat_cosmos_core";
    static final CoredLineup LINEUP = new CoredLineup("rat_abyssals", FleetTypes.PATROL_LARGE, new CoredShip("rat_morkoth_Anchor", CHRONOS_CORE),
            List.of(new CoredShip("rat_aboleth_m_Attack", CHRONOS_CORE), new CoredShip("rat_aboleth_Attack", COSMOS_CORE),
                    new CoredShip("rat_chuul_Attack", COSMOS_CORE), new CoredShip("rat_chuul_Strike", COSMOS_CORE), new CoredShip("rat_chuul_Support", null),
                    new CoredShip("rat_makara_Attack", null), new CoredShip("rat_makara_Attack", null), new CoredShip("rat_merrow_Support", null),
                    new CoredShip("rat_merrow_Support", null)),
            3, 2);
    static final long GUARD_SALT = 0x5348495059L;

    private ShipyardGuards() {
    }

    static CampaignFleetAPI muster(SectorEntityToken shipyard, ChartRun run) {
        return VanillaDefenders.muster(shipyard, run, GUARD_SALT, ShipyardGuards::create);
    }

    static CampaignFleetAPI create(int runTier, ChartGenContext context, long seed) {
        return LINEUP.create(runTier, "chart.anomaly.abyssal_shipyard.guards.name", new Random(seed));
    }
}
