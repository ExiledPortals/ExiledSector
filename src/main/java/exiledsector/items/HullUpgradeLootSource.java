package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
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
        private final Map<HullSize, Float> battlePowerByHullSize = new EnumMap<>(HullSize.class);

        BattleItemDrops.Battle battleItems() {
            List<BattleItemDrops.SourcePower> sources = new ArrayList<>();
            battlePowerByFleet.forEach((fleet, power) -> sources.add(new BattleItemDrops.SourcePower(power, LootSources.itemChancesFor(fleet))));
            return new BattleItemDrops.Battle(sources, battlePowerByHullSize, mostDefeatedFaction());
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
        for (FleetMemberAPI member : engagement.lostEnemies()) {
            CampaignFleetAPI sourceFleet = member == null ? null : engagement.battle().getSourceFleet(member);
            String factionId = sourceFleet == null || sourceFleet.getFaction() == null ? null : sourceFleet.getFaction().getId();
            if (factionId != null) {
                losses.defeatedDeploymentPointsByFaction.merge(factionId, member.getDeploymentPointsCost(), Float::sum);
            }
        }
        for (FleetMemberAPI member : engagement.lostEnemies()) {
            if (member == null) {
                continue;
            }
            float power = BattleItemDrops.shipPower(member.getDeploymentPointsCost(), LootSources.shipLevel(member)) * engagement.difficultyMultiplier();
            if (!(power > 0f)) {
                continue;
            }
            losses.battlePowerByFleet.merge(engagement.battle().getSourceFleet(member), power, Float::sum);
            HullSize hullSize = member.getHullSpec() == null ? null : member.getHullSpec().getHullSize();
            if (hullSize != null) {
                losses.battlePowerByHullSize.merge(hullSize, power, Float::sum);
            }
        }
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        BattleItemDrops.Loot battleItems = battleItemDrops(battle);
        battleItems.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
        battleItems.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        BattleItemDrops.Loot battleItems = battleItemDrops(battle);
        battleItems.items().forEach(item -> playerCargo.addSpecial(item.toSpecialItem(), 1f));
        ItemLootListener.announceBattleItems(battleItems.items());
        for (HullUpgradeData upgrade : battleItems.upgrades()) {
            playerCargo.addSpecial(upgrade.toSpecialItem(), 1f);
            ItemLootListener.announceSalvaged(upgrade::name);
        }
    }

    private BattleItemDrops.Loot battleItemDrops(BattleLoot battle) {
        SectorAPI sector = Global.getSector();
        if (!battle.playerWon() || sector == null || !HullUpgradesSetting.sectionsOpen(sector)) {
            return BattleItemDrops.Loot.NONE;
        }
        try {
            return BattleItemDrops.roll(losses(battle).battleItems(), BattleItemDrops.settingMultiplier(), battle.random(),
                    definition -> ItemUnlock.canDrop(definition, sector));
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling battle hull item drops failed", e);
            return BattleItemDrops.Loot.NONE;
        }
    }

    @Override
    public void addTechMiningFinds(TechMiningItemListener.Find find) {
        ItemDrops.SectionItemLoot sectionItemLoot = HullUpgradesSetting.sectionsOpen(find.sector())
                ? ItemDrops.rollSectionItemLoot(find.dropSite(), find.random(), find.chanceMult()) : ItemDrops.SectionItemLoot.NONE;
        find.items().addAll(sectionItemLoot.items());
        find.upgrades().addAll(sectionItemLoot.upgrades());
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
