package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.LogisticsSchematic;
import exiledsector.items.LogisticsSchematicDrops;
import exiledsector.items.ShipSchematic;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.SiteFind;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LogisticsSchematicLootSource implements LootSource {

    public static final LogisticsSchematicLootSource INSTANCE = new LogisticsSchematicLootSource();
    private static final Logger LOG = Logger.getLogger(LogisticsSchematicLootSource.class);

    private static final class FleetLosses {
        private float deploymentPoints;
        private float difficultyWeighted;
    }

    private LogisticsSchematicLootSource() {
    }

    private Map<CampaignFleetAPI, FleetLosses> losses(BattleLoot battle) {
        return battle.state(this, LinkedHashMap::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        Map<CampaignFleetAPI, FleetLosses> losses = losses(battle);
        float difficulty = BattleLootRates.difficulty(engagement.difficultyMultiplier());
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            for (FleetMemberAPI member : members) {
                float deploymentPoints = member.getDeploymentPointsCost();
                if (deploymentPoints > 0f) {
                    FleetLosses fleetLosses = losses.computeIfAbsent(sourceFleet, fleet -> new FleetLosses());
                    fleetLosses.deploymentPoints += deploymentPoints;
                    fleetLosses.difficultyWeighted += deploymentPoints * difficulty;
                }
            }
        });
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        ItemLootListener.addItems(loot, List.of(), battleDrops(battle));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        for (ShipSchematic schematic : battleDrops(battle)) {
            playerCargo.addSpecial(schematic.toSpecialItem(), 1f);
            ItemLootListener.announceSalvaged(schematic::displayName);
        }
    }

    @Override
    public void addSiteFinds(SiteFind find) {
        if (LogisticsSchematicDrops.rollSiteFind(find.dropSite(), find.random(), find.chances())) {
            find.upgrades().add(LogisticsSchematic.INSTANCE);
        }
    }

    private List<ShipSchematic> battleDrops(BattleLoot battle) {
        if (!battle.playerWon()) {
            return List.of();
        }
        try {
            List<LogisticsSchematicDrops.SourceLoss> sources = new ArrayList<>();
            losses(battle).forEach((fleet, fleetLosses) -> sources.add(new LogisticsSchematicDrops.SourceLoss(fleetLosses.deploymentPoints,
                    fleetLosses.difficultyWeighted / fleetLosses.deploymentPoints, LootSources.fleetLootRules(fleet))));
            return Collections.<ShipSchematic>nCopies(LogisticsSchematicDrops.rollBattle(sources, battle.random()), LogisticsSchematic.INSTANCE);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling Logistics Schematic battle drops failed", e);
            return List.of();
        }
    }
}
