package exiledsector.charts.anomaly.forge;

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

public final class ComplexGuards {

    static final String SERAPH_CORE = "rat_seraph_core";
    static final CoredLineup LINEUP = new CoredLineup("rat_abyssals_solitude", FleetTypes.PATROL_LARGE, new CoredShip("rat_gabriel_Attack", SERAPH_CORE),
            List.of(new CoredShip("rat_sariel_Attack", SERAPH_CORE), new CoredShip("rat_raguel_Attack", SERAPH_CORE),
                    new CoredShip("rat_raguel_Strike", SERAPH_CORE), new CoredShip("rat_sariel_Attack", SERAPH_CORE),
                    new CoredShip("rat_raguel_Attack", SERAPH_CORE), new CoredShip("rat_sariel_Strike", SERAPH_CORE),
                    new CoredShip("rat_raguel_Attack", SERAPH_CORE), new CoredShip("rat_raguel_Strike", SERAPH_CORE)),
            3, 2);
    static final long GUARD_SALT = 0x5345524150L;

    private ComplexGuards() {
    }

    static CampaignFleetAPI muster(SectorEntityToken complex, ChartRun run) {
        return VanillaDefenders.muster(complex, run, GUARD_SALT, ComplexGuards::create);
    }

    static CampaignFleetAPI create(int runTier, ChartGenContext context, long seed) {
        return LINEUP.create(runTier, "chart.anomaly.abyssal_research_complex.guards.name", new Random(seed));
    }
}
