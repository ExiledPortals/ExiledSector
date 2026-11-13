package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ItemLootListener;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.BattleItemDropSetting;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.MarketStock;
import exiledsector.loot.SiteFind;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class HullUpgradeLootSource implements LootSource, MarketStock {

    public static final HullUpgradeLootSource INSTANCE = new HullUpgradeLootSource();
    private static final Logger LOG = Logger.getLogger(HullUpgradeLootSource.class);

    private static final class FleetLosses {
        private float deploymentPoints;
        private float difficultyWeighted;
        private float levelBoostWeighted;
    }

    private static final class Losses {
        private final Map<String, Float> defeatedDeploymentPointsByFaction = new LinkedHashMap<>();
        private final Map<CampaignFleetAPI, FleetLosses> lossesByFleet = new LinkedHashMap<>();

        BattleItemDrops.Battle battleItems() {
            List<BattleItemDrops.SourceLoss> sources = new ArrayList<>();
            lossesByFleet.forEach((fleet, fleetLosses) -> sources.add(new BattleItemDrops.SourceLoss(fleetLosses.deploymentPoints,
                    fleetLosses.difficultyWeighted / fleetLosses.deploymentPoints, fleetLosses.levelBoostWeighted / fleetLosses.deploymentPoints,
                    LootSources.fleetLootRules(fleet))));
            return new BattleItemDrops.Battle(sources, mostDefeatedFaction());
        }

        private String mostDefeatedFaction() {
            String mostDefeated = null;
            for (Map.Entry<String, Float> defeated : defeatedDeploymentPointsByFaction.entrySet()) {
                if (mostDefeated == null || defeated.getValue() > defeatedDeploymentPointsByFaction.get(mostDefeated)) {
                    mostDefeated = defeated.getKey();
                }
            }
            return mostDefeated;
        }
    }

    private HullUpgradeLootSource() {
    }

    private Losses losses(BattleLoot battle) {
        return battle.state(this, Losses::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        Losses losses = losses(battle);
        engagement.deploymentPointsLostByFaction().forEach((factionId, deploymentPoints) ->
                losses.defeatedDeploymentPointsByFaction.merge(factionId, deploymentPoints, Float::sum));
        float difficulty = BattleLootRates.difficulty(engagement.difficultyMultiplier());
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            for (FleetMemberAPI member : members) {
                float deploymentPoints = member.getDeploymentPointsCost();
                if (deploymentPoints > 0f) {
                    FleetLosses fleetLosses = losses.lossesByFleet.computeIfAbsent(sourceFleet, fleet -> new FleetLosses());
                    fleetLosses.deploymentPoints += deploymentPoints;
                    fleetLosses.difficultyWeighted += deploymentPoints * difficulty;
                    fleetLosses.levelBoostWeighted += deploymentPoints * BattleItemDrops.levelBoost(LootSources.shipLevel(member));
                }
            }
        });
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
        for (ShipSchematic upgrade : battleItems.upgrades()) {
            playerCargo.addSpecial(upgrade.toSpecialItem(), 1f);
            ItemLootListener.announceSalvaged(upgrade::displayName);
        }
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
            return BattleItemDrops.roll(losses(battle).battleItems(), BattleItemDropSetting.multiplier(),
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
            HullItemDrops.SectionItemLoot sectionItemLoot = HullItemDrops.rollSectionItemLoot(find.dropSite(), find.random(), find.chances(),
                    FoundItemQuantity.hullItemMult());
            find.items().addAll(sectionItemLoot.items());
            find.upgrades().addAll(sectionItemLoot.upgrades());
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
