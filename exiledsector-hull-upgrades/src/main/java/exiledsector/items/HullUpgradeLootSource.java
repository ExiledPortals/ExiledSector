package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class HullUpgradeLootSource implements LootSource {

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
        ItemLootListener.addSectionLoot(loot, battleItemDrops(battle));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        ItemDrops.SectionItemLoot battleItems = battleItemDrops(battle);
        battleItems.items().forEach(item -> playerCargo.addSpecial(item.toSpecialItem(), 1f));
        ItemLootListener.announceBattleItems(battleItems.items());
        for (ShipCurrency upgrade : battleItems.upgrades()) {
            playerCargo.addSpecial(upgrade.toSpecialItem(), 1f);
            ItemLootListener.announceSalvaged(upgrade::displayName);
        }
    }

    private ItemDrops.SectionItemLoot battleItemDrops(BattleLoot battle) {
        SectorAPI sector = Global.getSector();
        if (!battle.playerWon() || sector == null) {
            return ItemDrops.SectionItemLoot.NONE;
        }
        boolean sectionsOpen = HullUpgradesSetting.sectionsOpen(sector);
        if (!sectionsOpen && !MasterSchematic.required()) {
            return ItemDrops.SectionItemLoot.NONE;
        }
        try {
            return BattleItemDrops.roll(losses(battle).battleItems(), BattleItemDrops.settingMultiplier(), battle.random(),
                    definition -> ItemUnlock.canDrop(definition, sector), sectionsOpen);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling battle hull item drops failed", e);
            return ItemDrops.SectionItemLoot.NONE;
        }
    }

    @Override
    public void addTechMiningFinds(TechMiningItemListener.Find find) {
        ItemDrops.SectionItemLoot sectionItemLoot = HullUpgradesSetting.sectionsOpen(find.sector())
                ? ItemDrops.rollSectionItemLoot(find.dropSite(), find.random(), find.chanceMult()) : ItemDrops.SectionItemLoot.NONE;
        find.items().addAll(sectionItemLoot.items());
        find.upgrades().addAll(sectionItemLoot.upgrades());
        find.upgrades().addAll(ItemDrops.rollMasterSchematic(find.dropSite(), find.random(), find.chanceMult()));
    }

    @Override
    public void stockSubmarket(SubmarketAPI submarket, CargoAPI cargo, Random random) {
        if (MarketHullItems.stocks(submarket.getSpecId())) {
            stockHullItems(submarket.getSpecId(), cargo, random);
        }
    }

    private static void stockHullItems(String submarketId, CargoAPI cargo, Random random) {
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (stack.isSpecialStack() && MarketHullItems.isStock(ItemData.of(stack.getSpecialDataIfSpecial()))) {
                cargo.removeStack(stack);
            }
        }
        if (!HullUpgradesSetting.sectionsOpen(Global.getSector())) {
            return;
        }
        for (ItemData item : MarketHullItems.roll(submarketId, random)) {
            cargo.addSpecial(item.toSpecialItem(), 1f);
        }
    }
}
