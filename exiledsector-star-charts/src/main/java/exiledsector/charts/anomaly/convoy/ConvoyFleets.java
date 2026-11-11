package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartSites;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.loot.DropType;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class ConvoyFleets {

    private static final long FEEDSTOCK_SALT = 0x5DEECE66DL;

    private ConvoyFleets() {
    }

    static ChartAnomalyFleets.Spec spec(String factionId, ChartTier tier, float escortMult) {
        float freighterPoints = ConvoyRules.freighterPoints(tier.tier());
        return ChartAnomalyFleets.bossCombat(factionId, FleetTypes.TRADE, tier, ConvoyRules.escortSize(tier, escortMult))
                .atLeast(ConvoyRules.FREIGHTER_GUARD_POINTS)
                .withCargoShips(freighterPoints * (1f - ConvoyRules.TANKER_SHARE), freighterPoints * ConvoyRules.TANKER_SHARE);
    }

    static int strengthBonus(ChartTier tier, float escortMult) {
        return ChartContentPlanner.overflowStrength(ConvoyRules.escortSize(tier, escortMult), ChartContentPlanner.BOSS_SIZE_CAP);
    }

    static boolean spawn(AnomalyContentSite site, ConvoyState convoy, String convoyFactionId, FleetEventListener convoyListener, Random random) {
        ChartGenContext context = site.context();
        ChartTier tier = ChartAnomalyFleets.tier(site.tier(), context);
        float escortMult = context.anomalyMult(SupplyConvoy.ESCORT_KEY);
        StarSystemAPI system = site.system();
        SectorEntityToken entry = system.createToken(convoy.entryX(), convoy.entryY());
        convoy.placeTokens(entry, system.createToken(convoy.exitX(), convoy.exitY()));
        long fleetSeed = random.nextLong();
        CampaignFleetAPI fleet = ChartAnomalyFleets.create(spec(site.factionId(), tier, escortMult), new Random(fleetSeed));
        if (fleet == null) {
            return false;
        }
        ChartAnomalyFleets.prepare(fleet, site.runId(), site.tier(), fleetSeed, true, context);
        int strengthBonus = strengthBonus(tier, escortMult);
        if (strengthBonus > 0) {
            fleet.getMemoryWithoutUpdate().set(ChartIds.STRENGTH_BONUS_MEMORY_KEY, strengthBonus);
        }
        dress(fleet, convoy, convoyFactionId);
        double angle = random.nextDouble() * Math.PI * 2d;
        site.addFleet(fleet, ChartSites.pointAround(new Vector2f(convoy.entryX(), convoy.entryY()), angle, ConvoyRules.SPAWN_SPREAD));
        convoy.setFleetId(fleet.getId());
        boolean civilianOnly = ConvoyCargo.freighterPoints(fleet, true) > 0f;
        float freighterPoints = ConvoyCargo.freighterPoints(fleet, civilianOnly);
        convoy.countCivilianOnly(civilianOnly);
        convoy.loadCargo(freighterPoints, ConvoyRules.cargoSteps(freighterPoints, context.anomalyMult(SupplyConvoy.CARGO_KEY)),
                ConvoyRules.itemChanceMult(site.tier(), context.anomalyMult(SupplyConvoy.CARGO_KEY)));
        convoy.loadFeedstock(ConvoyFeedstock.roll(site.tier(), context.dropMult(DropType.CRAFTING_ITEM) * context.anomalyMult(SupplyConvoy.CARGO_KEY),
                new Random(convoy.cargoSeed() ^ FEEDSTOCK_SALT)));
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, entry, ChartContentGenerator.ASSIGNMENT_DAYS);
        fleet.addEventListener(convoyListener);
        return true;
    }

    static void dress(CampaignFleetAPI fleet, ConvoyState convoy, String convoyFactionId) {
        if (convoyFactionId != null) {
            fleet.setFaction(convoyFactionId, true);
        }
        fleet.setName(Translation.gameText("chart.anomaly.supply_convoy.fleet.name"));
        fleet.getMemoryWithoutUpdate().set(SupplyConvoy.CONVOY_MEMORY_KEY, convoy.index());
        ConvoyHostility.makePeaceful(fleet);
    }
}
