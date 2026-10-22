package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.charts.ChartDrops;
import exiledsector.charts.ChartFleetLoot;
import exiledsector.charts.LootContexts;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.items.BattleItemDrops;
import exiledsector.items.FactionFleetDrops;
import exiledsector.items.HullUpgradeData;
import exiledsector.items.Item;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemUnlock;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.cargo.MaterialDropAnnouncer;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class ItemLootListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    private static final class Pending {
        private final Map<CampaignFleetAPI, Float> chartFleetDeploymentPoints = new LinkedHashMap<>();
        private final Map<String, Float> defeatedDeploymentPointsByFaction = new LinkedHashMap<>();
        private final Map<CampaignFleetAPI, Float> battlePowerByFleet = new LinkedHashMap<>();
        private final Map<HullSize, Float> battlePowerByHullSize = new EnumMap<>(HullSize.class);
        private boolean playerWon;
        private float partsEarned;
        private float deploymentPointsDefeated;
        private float chartStrengthDefeated;
        private int highestDefeatedNpcLevel;

        BattleItemDrops.Battle battleItems() {
            List<BattleItemDrops.SourcePower> sources = new ArrayList<>();
            battlePowerByFleet.forEach((fleet, power) -> sources.add(new BattleItemDrops.SourcePower(power, chancesFor(fleet))));
            return new BattleItemDrops.Battle(sources, battlePowerByHullSize, mostDefeatedFaction());
        }

        private static ItemChances chancesFor(CampaignFleetAPI fleet) {
            try {
                return LootContexts.forFleet(fleet).itemChances();
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Reading the item chances of a defeated fleet failed", e);
                return LootContexts.global().itemChances();
            }
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

    private static final Logger LOG = Logger.getLogger(ItemLootListener.class);
    private final Map<BattleAPI, Pending> pendingByBattle = new IdentityHashMap<>();
    private final Random lootRandom;
    private final Random chartRandom;

    public ItemLootListener() {
        this(new Random());
    }

    ItemLootListener(Random lootRandom) {
        this(lootRandom, new Random());
    }

    ItemLootListener(Random lootRandom, Random chartRandom) {
        super(false);
        this.lootRandom = lootRandom;
        this.chartRandom = chartRandom;
    }

    void holdLoot(PlayerEngagement engagement) {
        EngagementResultAPI engagementResult = engagement.result();
        if (engagementResult.getBattle() == null) {
            return;
        }
        Pending pendingLoot = pendingByBattle.computeIfAbsent(engagementResult.getBattle(), key -> new Pending());
        pendingLoot.playerWon = engagement.playerWon();
        if (!pendingLoot.playerWon) {
            return;
        }
        float difficultyMultiplier = ShipLevelSystem.difficultyMultiplier(engagement.difficulty(), ShipLevelConfig.xpDifficultyStrength(),
                ShipLevelConfig.xpDifficultyMaxMultiplier());
        for (EngagementResultForFleetAPI fleetResult : new EngagementResultForFleetAPI[]{engagementResult.getWinnerResult(), engagementResult.getLoserResult()}) {
            if (fleetResult != null && !fleetResult.isPlayer()) {
                for (List<FleetMemberAPI> lostMembers : List.of(nullToEmpty(fleetResult.getDestroyed()), nullToEmpty(fleetResult.getDisabled()))) {
                    holdChartFleetLosses(pendingLoot, engagementResult.getBattle(), lostMembers);
                    holdFactionLosses(pendingLoot, engagementResult.getBattle(), lostMembers);
                    holdBattlePower(pendingLoot, engagementResult.getBattle(), lostMembers, difficultyMultiplier);
                }
            }
        }
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        if (defeatedDp > 0f) {
            pendingLoot.partsEarned += defeatedDp * ItemDrops.battlePartsPerDeploymentPoint() * difficultyMultiplier;
            pendingLoot.deploymentPointsDefeated += defeatedDp;
            pendingLoot.chartStrengthDefeated += defeatedDp * difficultyMultiplier;
        }
    }

    private static List<FleetMemberAPI> nullToEmpty(List<FleetMemberAPI> members) {
        return members == null ? List.of() : members;
    }

    private static void holdBattlePower(Pending pendingLoot, BattleAPI battle, List<FleetMemberAPI> lostMembers, float difficultyMultiplier) {
        for (FleetMemberAPI member : lostMembers) {
            if (member == null) {
                continue;
            }
            String npcTreeTag = NpcTreeTag.find(member.getVariant());
            int allocatedNodes = npcTreeTag == null ? 0 : NpcTreeTag.allocatedNodeCount(npcTreeTag);
            pendingLoot.highestDefeatedNpcLevel = Math.max(pendingLoot.highestDefeatedNpcLevel, allocatedNodes);
            float power = BattleItemDrops.shipPower(member.getDeploymentPointsCost(), allocatedNodes) * difficultyMultiplier;
            if (!(power > 0f)) {
                continue;
            }
            pendingLoot.battlePowerByFleet.merge(battle.getSourceFleet(member), power, Float::sum);
            HullSize hullSize = member.getHullSpec() == null ? null : member.getHullSpec().getHullSize();
            if (hullSize != null) {
                pendingLoot.battlePowerByHullSize.merge(hullSize, power, Float::sum);
            }
        }
    }

    private BattleItemDrops.Loot battleItemDrops(Pending pendingLoot) {
        SectorAPI sector = Global.getSector();
        if (!pendingLoot.playerWon || sector == null || !ItemUnlock.sectionsOpen(sector)) {
            return BattleItemDrops.Loot.NONE;
        }
        try {
            return BattleItemDrops.roll(pendingLoot.battleItems(), BattleItemDrops.settingMultiplier(), lootRandom,
                    definition -> ItemUnlock.canDrop(definition, sector));
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling battle hull item drops failed", e);
            return BattleItemDrops.Loot.NONE;
        }
    }

    private ItemDrops.SiteLoot chartLoot(Pending pendingLoot) {
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (sector == null || playerFleet == null || !(pendingLoot.deploymentPointsDefeated > 0f)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDrops.SiteLoot chartLoot = ItemDrops.SiteLoot.NONE;
        if (pendingLoot.playerWon) {
            try {
                ChartDrops.Loot battleLoot = ChartDrops.forBattle(sector, playerFleet.getContainingLocation(), pendingLoot.chartStrengthDefeated,
                        pendingLoot.highestDefeatedNpcLevel, chartRandom);
                chartLoot = chartLoot.plus(battleLoot.charts(), List.of(), battleLoot.currencies());
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Rolling Star Chart battle drops failed", e);
            }
        }
        for (Map.Entry<CampaignFleetAPI, Float> fleetLosses : pendingLoot.chartFleetDeploymentPoints.entrySet()) {
            try {
                chartLoot = chartLoot.plus(ChartFleetLoot.roll(sector, fleetLosses.getKey(), fleetLosses.getValue(), chartRandom));
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] Rolling Star Chart fleet loot failed for fleet " + fleetLosses.getKey().getId(), e);
            }
        }
        return chartLoot;
    }

    private static void holdChartFleetLosses(Pending pendingLoot, BattleAPI battle, List<FleetMemberAPI> lostMembers) {
        if (lostMembers == null) {
            return;
        }
        for (FleetMemberAPI member : lostMembers) {
            CampaignFleetAPI sourceFleet = member == null ? null : battle.getSourceFleet(member);
            if (ChartFleetLoot.isChartFleet(sourceFleet) || adoptedAsDefender(sourceFleet)) {
                pendingLoot.chartFleetDeploymentPoints.merge(sourceFleet, member.getDeploymentPointsCost(), Float::sum);
            }
        }
    }

    private static void holdFactionLosses(Pending pendingLoot, BattleAPI battle, List<FleetMemberAPI> lostMembers) {
        if (lostMembers == null) {
            return;
        }
        for (FleetMemberAPI member : lostMembers) {
            CampaignFleetAPI sourceFleet = member == null ? null : battle.getSourceFleet(member);
            String factionId = sourceFleet == null || sourceFleet.getFaction() == null ? null : sourceFleet.getFaction().getId();
            if (factionId != null) {
                pendingLoot.defeatedDeploymentPointsByFaction.merge(factionId, member.getDeploymentPointsCost(), Float::sum);
            }
        }
    }

    private List<ItemData> factionFleetDrops(Pending pendingLoot) {
        if (!pendingLoot.playerWon) {
            return List.of();
        }
        try {
            return FactionFleetDrops.roll(pendingLoot.defeatedDeploymentPointsByFaction, lootRandom, FactionFleetDrops::available);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling faction fleet item drops failed", e);
            return List.of();
        }
    }

    private static boolean adoptedAsDefender(CampaignFleetAPI sourceFleet) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (sourceFleet == null || playerFleet == null) {
            return false;
        }
        try {
            return ChartFleetLoot.adoptDefender(sourceFleet, playerFleet.getContainingLocation());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Checking whether a fleet defends a Star Chart site failed", e);
            return false;
        }
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        ShipTreeSync.fleetChanged(PlayerFleet.orNull());
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin encounterPlugin, CargoAPI loot) {
        Pending pendingLoot = encounterPlugin == null ? null : pendingByBattle.remove(encounterPlugin.getBattle());
        if (pendingLoot != null && loot != null) {
            factionFleetDrops(pendingLoot).forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            BattleItemDrops.Loot battleItems = battleItemDrops(pendingLoot);
            battleItems.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            battleItems.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
            int wholeParts = ItemDrops.wholeParts(pendingLoot.partsEarned, lootRandom);
            if (wholeParts > 0) {
                MaterialItems.add(loot, ItemDisassembly.PARTS_ITEM_ID, wholeParts);
            }
            ItemDrops.SiteLoot chartLoot = chartLoot(pendingLoot);
            chartLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            chartLoot.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
            chartLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        }
        pendingByBattle.keySet().removeIf(BattleAPI::isDone);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        Pending pendingLoot = pendingByBattle.remove(battle);
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (pendingLoot == null || !pendingLoot.playerWon || playerFleet == null) {
            return;
        }
        for (ItemData item : factionFleetDrops(pendingLoot)) {
            playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f);
            Item previewItem = item.preview();
            if (previewItem != null) {
                announceSalvaged(previewItem::name);
            }
        }
        BattleItemDrops.Loot battleItems = battleItemDrops(pendingLoot);
        battleItems.items().forEach(item -> playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f));
        announceBattleItems(battleItems.items());
        for (HullUpgradeData upgrade : battleItems.upgrades()) {
            playerFleet.getCargo().addSpecial(upgrade.toSpecialItem(), 1f);
            announceSalvaged(upgrade::name);
        }
        int wholeParts = ItemDrops.wholeParts(pendingLoot.partsEarned, lootRandom);
        if (wholeParts > 0) {
            MaterialItems.add(playerFleet.getCargo(), ItemDisassembly.PARTS_ITEM_ID, wholeParts);
            String partsName = MaterialItems.displayName(ItemDisassembly.PARTS_ITEM_ID);
            if (CampaignMessages.available()) {
                String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name",
                        Translation.msg("socketable.techMining.material").arg("count", wholeParts).arg("name", partsName).text()).text());
                CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
            }
        }
        ItemDrops.SiteLoot chartLoot = chartLoot(pendingLoot);
        for (ItemData item : chartLoot.items()) {
            playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f);
            Item previewItem = item.preview();
            if (previewItem != null) {
                announceSalvaged(previewItem::name);
            }
        }
        for (HullUpgradeData upgrade : chartLoot.upgrades()) {
            playerFleet.getCargo().addSpecial(upgrade.toSpecialItem(), 1f);
            announceSalvaged(upgrade::name);
        }
        chartLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(playerFleet.getCargo(), itemId, quantity));
        MaterialDropAnnouncer.announce(chartLoot.materials());
    }

    private static void announceBattleItems(List<ItemData> items) {
        Map<ItemRarity, List<Item>> previewsByRarity = new EnumMap<>(ItemRarity.class);
        for (ItemData item : items) {
            Item previewItem = item.preview();
            if (previewItem != null) {
                previewsByRarity.computeIfAbsent(previewItem.rarity(), rarity -> new ArrayList<>()).add(previewItem);
            }
        }
        previewsByRarity.forEach((rarity, previews) -> {
            if (rarity == ItemRarity.UNIQUE || previews.size() == 1) {
                previews.forEach(previewItem -> announceSalvaged(previewItem::name));
            } else if (CampaignMessages.available()) {
                String messageKey = rarity == ItemRarity.COMMON ? "socketable.salvagedCommon" : "socketable.salvagedRare";
                String salvageMessage = I18n.forGameText(() -> Translation.msg(messageKey).arg("count", previews.size()).text());
                CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
            }
        });
    }

    private static void announceSalvaged(Supplier<String> itemName) {
        if (CampaignMessages.available()) {
            String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name", itemName.get()).text());
            CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        pendingByBattle.keySet().removeIf(battle -> battle.isDone() || !battle.isPlayerInvolved());
    }
}
