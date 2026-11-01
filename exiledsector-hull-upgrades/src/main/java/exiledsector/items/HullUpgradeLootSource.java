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
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.MarketStock;
import exiledsector.loot.SiteFind;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class HullUpgradeLootSource implements LootSource, MarketStock {

    public static final HullUpgradeLootSource INSTANCE = new HullUpgradeLootSource();
    private static final Logger LOG = Logger.getLogger(HullUpgradeLootSource.class);

    private static final class Losses {
        private final Map<String, Float> defeatedDeploymentPointsByFaction = new LinkedHashMap<>();
        private final Map<CampaignFleetAPI, Float> battlePowerByFleet = new LinkedHashMap<>();

        BattleItemDrops.Battle battleItems() {
            List<BattleItemDrops.SourcePower> sources = new ArrayList<>();
            battlePowerByFleet.forEach((fleet, power) -> sources.add(new BattleItemDrops.SourcePower(power, LootSources.itemChancesFor(fleet))));
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
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            for (FleetMemberAPI member : members) {
                float power = BattleItemDrops.shipPower(member.getDeploymentPointsCost(), LootSources.shipLevel(member)) * engagement.difficultyMultiplier();
                if (power > 0f) {
                    losses.battlePowerByFleet.merge(sourceFleet, power, Float::sum);
                }
            }
        });
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        battleItemDrops(battle).addTo(loot);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        HullItemDrops.SectionItemLoot battleItems = battleItemDrops(battle);
        battleItems.items().forEach(item -> playerCargo.addSpecial(item.toSpecialItem(), 1f));
        ItemLootListener.announceBattleItems(battleItems.items());
        for (ShipCurrency upgrade : battleItems.upgrades()) {
            playerCargo.addSpecial(upgrade.toSpecialItem(), 1f);
            ItemLootListener.announceSalvaged(upgrade::displayName);
        }
    }

    private HullItemDrops.SectionItemLoot battleItemDrops(BattleLoot battle) {
        SectorAPI sector = Global.getSector();
        if (!battle.playerWon() || sector == null) {
            return HullItemDrops.SectionItemLoot.NONE;
        }
        boolean sectionsOpen = HullUpgradesSetting.sectionsOpen(sector);
        if (!sectionsOpen && !MasterSchematic.required()) {
            return HullItemDrops.SectionItemLoot.NONE;
        }
        try {
            return BattleItemDrops.roll(losses(battle).battleItems(), BattleItemDrops.settingMultiplier(), battle.random(),
                    definition -> ItemUnlock.canDrop(definition, sector), sectionsOpen);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling battle hull item drops failed", e);
            return HullItemDrops.SectionItemLoot.NONE;
        }
    }

    @Override
    public void addSiteFinds(SiteFind find) {
        if (HullUpgradesSetting.sectionsOpen(find.sector())) {
            HullItemDrops.SectionItemLoot sectionItemLoot = HullItemDrops.rollSectionItemLoot(find.dropSite(), find.random(), find.chances());
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
        if (!HullUpgradesSetting.sectionsOpen(Global.getSector())) {
            return List.of();
        }
        return MarketHullItems.roll(submarket.getSpecId(), random);
    }
}
