package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledlib.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class ConvoyFleets {

    private ConvoyFleets() {
    }

    static ChartAnomalyFleets.Spec spec(ConvoyRole role, String factionId, ChartTier tier, float escortMult) {
        return switch (role) {
            case FREIGHTERS -> {
                float freighterPoints = ConvoyRules.freighterPoints(tier.tier());
                yield ChartAnomalyFleets.freighters(factionId, FleetTypes.TRADE, tier, freighterPoints * (1f - ConvoyRules.TANKER_SHARE),
                        freighterPoints * ConvoyRules.TANKER_SHARE, ConvoyRules.FREIGHTER_GUARD_POINTS);
            }
            case SCREEN -> ChartAnomalyFleets.combat(factionId, FleetTypes.PATROL_SMALL, tier, tier.fleetSize() * ConvoyRules.SCREEN_SIZE_SHARE * escortMult);
            case ESCORT -> ChartAnomalyFleets.combat(factionId, FleetTypes.PATROL_MEDIUM, tier, tier.fleetSize() * ConvoyRules.ESCORT_SIZE_SHARE * escortMult);
        };
    }

    static boolean spawn(AnomalyContentSite site, ConvoyState convoy, String convoyFactionId, Random random) {
        ChartGenContext context = site.context();
        ChartTier tier = ChartAnomalyFleets.tier(site.tier(), context);
        float escortMult = context.anomalyMult(SupplyConvoy.ESCORT_KEY);
        StarSystemAPI system = site.system();
        SectorEntityToken entry = system.createToken(convoy.entryX(), convoy.entryY());
        SectorEntityToken exit = system.createToken(convoy.exitX(), convoy.exitY());
        SectorEntityToken lead = system.createToken(convoy.entryX(), convoy.entryY());
        convoy.placeTokens(entry, exit, lead);
        CampaignFleetAPI freighters = null;
        for (ConvoyRole role : ConvoyRole.values()) {
            long fleetSeed = random.nextLong();
            if (role != ConvoyRole.FREIGHTERS && !(escortMult > 0f)) {
                continue;
            }
            CampaignFleetAPI fleet = ChartAnomalyFleets.create(spec(role, site.factionId(), tier, escortMult), new Random(fleetSeed));
            if (fleet == null) {
                if (role == ConvoyRole.FREIGHTERS) {
                    return false;
                }
                continue;
            }
            ChartAnomalyFleets.prepare(fleet, site.runId(), site.tier(), fleetSeed, role != ConvoyRole.FREIGHTERS, context);
            dress(fleet, role, convoy, convoyFactionId);
            double angle = random.nextDouble() * Math.PI * 2d;
            site.addFleet(fleet, new Vector2f(convoy.entryX() + (float) Math.cos(angle) * ConvoyRules.SPAWN_SPREAD,
                    convoy.entryY() + (float) Math.sin(angle) * ConvoyRules.SPAWN_SPREAD));
            convoy.setFleetId(role, fleet.getId());
            if (role == ConvoyRole.FREIGHTERS) {
                freighters = fleet;
                float freighterPoints = fleet.getFleetPoints();
                convoy.loadCargo(freighterPoints, ConvoyRules.cargoSteps(freighterPoints, context.anomalyMult(SupplyConvoy.CARGO_KEY)),
                        ConvoyRules.itemChanceMult(site.tier(), context.anomalyMult(SupplyConvoy.CARGO_KEY)));
                fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, entry, ChartContentGenerator.ASSIGNMENT_DAYS);
            } else {
                fleet.addAssignment(FleetAssignment.FOLLOW, role == ConvoyRole.SCREEN ? lead : freighters, ChartContentGenerator.ASSIGNMENT_DAYS);
            }
        }
        return freighters != null;
    }

    static void dress(CampaignFleetAPI fleet, ConvoyRole role, ConvoyState convoy, String convoyFactionId) {
        if (convoyFactionId != null) {
            fleet.setFaction(convoyFactionId, true);
        }
        fleet.setName(Translation.gameText("chart.convoy.fleet." + role.key() + ".name"));
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(SupplyConvoy.CONVOY_MEMORY_KEY, convoy.index());
        memory.set(SupplyConvoy.ROLE_MEMORY_KEY, role.key());
        if (role == ConvoyRole.FREIGHTERS) {
            memory.set(MemFlags.MEMORY_KEY_TRADE_FLEET, true);
        }
        ConvoyHostility.makePeaceful(fleet);
    }
}
