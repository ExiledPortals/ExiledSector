package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.items.ShipSchematic;
import exiledsector.items.SkillTreeSchematicDrops;
import exiledsector.loot.BattleItemDropSetting;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.FleetLoss;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.OutsideChartShare;
import exiledsector.loot.SiteFind;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SkillTreeSchematicLootSource implements LootSource {

    public static final SkillTreeSchematicLootSource INSTANCE = new SkillTreeSchematicLootSource();
    private static final Logger LOG = Logger.getLogger(SkillTreeSchematicLootSource.class);

    private SkillTreeSchematicLootSource() {
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        LootDelivery.toLootScreen(loot, new Loot(List.of(), battleDrops(battle), Map.of()), DropOrigin.ANYWHERE);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        LootDelivery.deliver(playerCargo, new Loot(List.of(), battleDrops(battle), Map.of()), DropOrigin.ANYWHERE);
    }

    @Override
    public void addSiteFinds(SiteFind find) {
        for (SkillTreeSchematicDrops drop : SkillTreeSchematicDrops.values()) {
            if (SkillTreeSchematicDrops.rollSiteFind(find.dropSite(), find.random(), find.chances())) {
                find.schematics().add(drop.schematic());
            }
        }
    }

    private List<ShipSchematic> battleDrops(BattleLoot battle) {
        if (!battle.playerWon()) {
            return List.of();
        }
        List<ShipSchematic> drops = new ArrayList<>();
        try {
            List<FleetLoss> fleetLosses = battle.losses().fleetLosses(LootSources::fleetLootRules);
            float levelScaledShare = OutsideChartShare.current();
            for (SkillTreeSchematicDrops drop : SkillTreeSchematicDrops.values()) {
                drops.addAll(Collections.nCopies(drop.rollBattle(fleetLosses, levelScaledShare, BattleItemDropSetting.multiplier(), battle.random()),
                        drop.schematic()));
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling skill tree schematic battle drops failed", e);
        }
        return drops;
    }
}
