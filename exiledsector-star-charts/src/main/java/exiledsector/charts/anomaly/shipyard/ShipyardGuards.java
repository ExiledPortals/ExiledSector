package exiledsector.charts.anomaly.shipyard;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;

import java.util.List;
import java.util.Random;

public final class ShipyardGuards {

    record Escort(String variantId, String aiCoreId) {
    }

    static final String FACTION_ID = "rat_abyssals";
    static final String CHRONOS_CORE = "rat_chronos_core";
    static final String COSMOS_CORE = "rat_cosmos_core";
    static final String FLAGSHIP_VARIANT = "rat_morkoth_Anchor";
    static final List<Escort> ESCORTS = List.of(new Escort("rat_aboleth_m_Attack", CHRONOS_CORE), new Escort("rat_aboleth_Attack", COSMOS_CORE),
            new Escort("rat_chuul_Attack", COSMOS_CORE), new Escort("rat_chuul_Strike", COSMOS_CORE), new Escort("rat_chuul_Support", null),
            new Escort("rat_makara_Attack", null), new Escort("rat_makara_Attack", null), new Escort("rat_merrow_Support", null),
            new Escort("rat_merrow_Support", null));
    static final int ESCORTS_AT_LOWEST_TIER = 3;
    static final int TIERS_PER_EXTRA_ESCORT = 2;
    static final long GUARD_SALT = 0x5348495059L;

    private ShipyardGuards() {
    }

    static int escortCount(int tier) {
        int extra = (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER) / TIERS_PER_EXTRA_ESCORT;
        return Math.min(ESCORTS.size(), ESCORTS_AT_LOWEST_TIER + extra);
    }

    static CampaignFleetAPI muster(SectorEntityToken shipyard, ChartRun run) {
        return VanillaDefenders.muster(shipyard, run, GUARD_SALT, ShipyardGuards::create);
    }

    static CampaignFleetAPI create(int runTier, ChartGenContext context, long seed) {
        Random random = new Random(seed);
        CampaignFleetAPI guards = ChartAnomalyFleets.emptyFleet(FACTION_ID, FleetTypes.PATROL_LARGE);
        if (guards == null || !ChartAnomalyFleets.addCoreFlagship(guards, FLAGSHIP_VARIANT, CHRONOS_CORE, random)) {
            return null;
        }
        for (Escort escort : ESCORTS.subList(0, escortCount(runTier))) {
            ChartAnomalyFleets.addCoreShip(guards, escort.variantId(), escort.aiCoreId(), random);
        }
        guards.setName(Translation.gameText("chart.anomaly.abyssal_shipyard.guards.name"));
        guards.setNoFactionInName(true);
        return guards;
    }
}
