package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.items.ShipSchematic;
import exiledsector.items.SkillTreeSchematicDrops;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleItemDropSetting;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.FleetLossTally;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.SiteFind;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkillTreeSchematicLootSource implements LootSource {

    public static final SkillTreeSchematicLootSource INSTANCE = new SkillTreeSchematicLootSource();
    private static final Logger LOG = Logger.getLogger(SkillTreeSchematicLootSource.class);

    private SkillTreeSchematicLootSource() {
    }

    private FleetLossTally losses(BattleLoot battle) {
        return battle.state(this, FleetLossTally::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        losses(battle).record(engagement);
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        ItemLootListener.addItems(loot, List.of(), battleDrops(battle));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        ItemLootListener.deliverSchematics(playerCargo, battleDrops(battle));
    }

    @Override
    public void addSiteFinds(SiteFind find) {
        for (SkillTreeSchematicDrops drop : SkillTreeSchematicDrops.values()) {
            if (SkillTreeSchematicDrops.rollSiteFind(find.dropSite(), find.random(), find.chances())) {
                find.upgrades().add(drop.schematic());
            }
        }
    }

    private List<ShipSchematic> battleDrops(BattleLoot battle) {
        if (!battle.playerWon()) {
            return List.of();
        }
        List<ShipSchematic> drops = new ArrayList<>();
        try {
            List<SkillTreeSchematicDrops.SourceLoss> sources = new ArrayList<>();
            for (FleetLossTally.FleetLoss fleetLoss : losses(battle).fleetLosses()) {
                sources.add(new SkillTreeSchematicDrops.SourceLoss(fleetLoss.deploymentPoints(), fleetLoss.averageDifficulty(),
                        LootSources.fleetLootRules(fleetLoss.fleet())));
            }
            for (SkillTreeSchematicDrops drop : SkillTreeSchematicDrops.values()) {
                drops.addAll(Collections.nCopies(drop.rollBattle(sources, BattleItemDropSetting.multiplier(), battle.random()), drop.schematic()));
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling skill tree schematic battle drops failed", e);
        }
        return drops;
    }
}
