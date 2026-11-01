package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.run.ChartRun;

import java.util.Random;

public final class ForgeGuards {

    static final String GUARDIAN_VARIANT = "guardian_Standard";
    static final long GUARD_SALT = 0x4755415244L;

    private ForgeGuards() {
    }

    static CampaignFleetAPI muster(SectorEntityToken forge, ChartRun run) {
        return VanillaDefenders.muster(forge, run, GUARD_SALT, ForgeGuards::create);
    }

    static CampaignFleetAPI create(int runTier, ChartGenContext context, long seed) {
        Random random = new Random(seed);
        ChartTier tier = ChartAnomalyFleets.tier(runTier, context);
        ChartAnomalyFleets.Spec escort = ChartAnomalyFleets.combat(Factions.DERELICT, FleetTypes.PATROL_MEDIUM, tier,
                tier.fleetSize() * ForgeRules.ESCORT_SIZE_SHARE);
        CampaignFleetAPI guards = ChartAnomalyFleets.create(escort, random, ForgeRules.ESCORT_MAX_SHIP_SIZE, false);
        if (guards == null) {
            guards = ChartAnomalyFleets.emptyFleet(Factions.DERELICT, FleetTypes.PATROL_MEDIUM);
        }
        if (guards == null) {
            return null;
        }
        boolean withGuardian = ChartAnomalyFleets.addCoreFlagship(guards, GUARDIAN_VARIANT, Commodities.ALPHA_CORE, random);
        if (!withGuardian && guards.isEmpty()) {
            return null;
        }
        guards.setName(Translation.gameText("chart.forge.guards.name"));
        return guards;
    }
}
