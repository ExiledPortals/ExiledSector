package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.effects.ItemLootListener;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.BattleItemDropSetting;
import exiledsector.loot.FleetDifficulties;
import exiledsector.loot.FleetLossTally;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.MarketStock;
import exiledsector.loot.OutsideChartShare;
import exiledsector.loot.SiteFind;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class HullUpgradeLootSource implements LootSource, MarketStock {

    public static final HullUpgradeLootSource INSTANCE = new HullUpgradeLootSource();
    private static final Logger LOG = Logger.getLogger(HullUpgradeLootSource.class);

    private HullUpgradeLootSource() {
    }

    private FleetLossTally losses(BattleLoot battle) {
        return battle.state(this, () -> new FleetLossTally(member -> BattleItemDrops.levelBoost(FleetDifficulties.shipLevel(member))));
    }

    private static BattleItemDrops.Battle battleItems(FleetLossTally losses) {
        List<BattleItemDrops.SourceLoss> sources = new ArrayList<>();
        for (FleetLossTally.FleetLoss fleetLoss : losses.fleetLosses()) {
            sources.add(new BattleItemDrops.SourceLoss(fleetLoss.deploymentPoints(), fleetLoss.averageDifficulty(), fleetLoss.averageShipWeight(),
                    LootSources.fleetLootRules(fleetLoss.fleet())));
        }
        return new BattleItemDrops.Battle(sources, losses.mostDefeatedFaction(), OutsideChartShare.current());
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        losses(battle).record(engagement);
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        HullItemDrops.SectionItemLoot battleItems = battleItemDrops(battle);
        ItemLootListener.addBattleItems(loot, battleItems.items(), battleItems.upgrades());
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        HullItemDrops.SectionItemLoot battleItems = battleItemDrops(battle);
        List<ItemData> items = LootSources.shapeBattleDrops(battleItems.items());
        items.forEach(item -> playerCargo.addSpecial(item.toSpecialItem(), 1f));
        ItemLootListener.announceBattleItems(items);
        ItemLootListener.deliverSchematics(playerCargo, battleItems.upgrades());
    }

    private HullItemDrops.SectionItemLoot battleItemDrops(BattleLoot battle) {
        SectorAPI sector = Global.getSector();
        if (!battle.playerWon() || sector == null) {
            return HullItemDrops.SectionItemLoot.NONE;
        }
        boolean hullItemDropsOpen = HullUpgradesSetting.hullItemDropsOpen(sector);
        if (!hullItemDropsOpen && !MasterSchematic.required()) {
            return HullItemDrops.SectionItemLoot.NONE;
        }
        try {
            return BattleItemDrops.roll(battleItems(losses(battle)), BattleItemDropSetting.multiplier(),
                    FoundItemQuantity.hullItemMult(), battle.random(),
                    definition -> ItemUnlock.canDrop(definition, sector), hullItemDropsOpen);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling battle hull item drops failed", e);
            return HullItemDrops.SectionItemLoot.NONE;
        }
    }

    @Override
    public void addSiteFinds(SiteFind find) {
        if (HullUpgradesSetting.hullItemDropsOpen(find.sector())) {
            HullItemDrops.SectionItemLoot hullItemLoot = HullItemDrops.rollHullItemLoot(find.dropSite(), find.random(), find.chances(),
                    FoundItemQuantity.hullItemMult());
            find.items().addAll(hullItemLoot.items());
            find.upgrades().addAll(hullItemLoot.upgrades());
        }
        find.upgrades().addAll(HullItemDrops.rollMasterSchematic(find.dropSite(), find.random(), find.chances().general()));
    }

    @Override
    public boolean isOwnListing(ItemData listing) {
        return MarketHullItems.isStock(listing);
    }

    @Override
    public List<ItemData> restock(SubmarketAPI submarket, Random random) {
        if (!HullUpgradesSetting.hullItemDropsOpen(Global.getSector())) {
            return List.of();
        }
        return MarketHullItems.roll(submarket.getSpecId(), random);
    }
}
