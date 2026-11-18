package exiledsector.items.hull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemUnlock;
import exiledsector.items.drop.BattleItemDrops;
import exiledsector.items.drop.HullItemDrops;
import exiledsector.items.drop.MarketHullItems;
import exiledsector.items.schematic.MasterSchematic;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.DropType;
import exiledsector.loot.FleetLossTally;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.MarketStock;
import exiledsector.loot.OutsideChartShare;
import exiledsector.loot.SiteFind;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Random;

public final class HullUpgradeLootSource implements LootSource, MarketStock {

    public static final HullUpgradeLootSource INSTANCE = new HullUpgradeLootSource();
    private static final Logger LOG = Logger.getLogger(HullUpgradeLootSource.class);

    private HullUpgradeLootSource() {
    }

    private static BattleItemDrops.Battle battleItems(BattleLoot battle) {
        FleetLossTally losses = battle.losses();
        return new BattleItemDrops.Battle(losses.fleetLosses(LootSources::fleetLootRules), losses.mostDefeatedFaction(), OutsideChartShare.current());
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        LootDelivery.toLootScreen(loot, battleItemDrops(battle), DropOrigin.BATTLE);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        LootDelivery.deliver(playerCargo, battleItemDrops(battle), DropOrigin.BATTLE, LootDelivery.ITEMS_BY_RARITY);
    }

    private Loot battleItemDrops(BattleLoot battle) {
        SectorAPI sector = Global.getSector();
        if (!battle.playerWon() || sector == null) {
            return Loot.NONE;
        }
        boolean hullItemDropsOpen = HullUpgradesSetting.hullItemDropsOpen(sector);
        if (!hullItemDropsOpen && !MasterSchematic.required()) {
            return Loot.NONE;
        }
        try {
            return BattleItemDrops.roll(battleItems(battle), battle.random(),
                    definition -> ItemUnlock.canDrop(definition, sector), hullItemDropsOpen);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling battle hull item drops failed", e);
            return Loot.NONE;
        }
    }

    @Override
    public void addSiteFinds(SiteFind find) {
        if (HullUpgradesSetting.hullItemDropsOpen(find.sector())) {
            Loot hullItemLoot = HullItemDrops.rollHullItemLoot(find.dropSite(), find.random(), find.chances(),
                    definition -> ItemUnlock.canDrop(definition, find.sector()));
            find.items().addAll(hullItemLoot.items());
            find.schematics().addAll(hullItemLoot.schematics());
        }
        find.schematics().addAll(HullItemDrops.rollMasterSchematic(find.dropSite(), find.random(), find.chances().quantity(DropType.SCHEMATIC)));
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
