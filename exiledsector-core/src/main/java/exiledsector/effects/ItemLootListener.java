package exiledsector.effects;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ExiledSector;
import exiledlib.PlayerFleet;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.items.HullUpgradeData;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemRarity;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.LootSources;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.cargo.MaterialDropAnnouncer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class ItemLootListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    private static final class Pending {
        private final BattleLoot battleLoot;
        private float partsEarned;

        Pending(BattleLoot battleLoot) {
            this.battleLoot = battleLoot;
        }
    }

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
        Pending pendingLoot = pendingByBattle.computeIfAbsent(engagementResult.getBattle(),
                key -> new Pending(new BattleLoot(lootRandom, chartRandom)));
        pendingLoot.battleLoot.setPlayerWon(engagement.playerWon());
        if (!engagement.playerWon()) {
            return;
        }
        float difficultyMultiplier = LootSources.difficultyMultiplier(engagement.difficulty());
        List<FleetMemberAPI> lostEnemies = new ArrayList<>();
        for (EngagementResultForFleetAPI fleetResult : new EngagementResultForFleetAPI[]{engagementResult.getWinnerResult(), engagementResult.getLoserResult()}) {
            if (fleetResult != null && !fleetResult.isPlayer()) {
                lostEnemies.addAll(nullToEmpty(fleetResult.getDestroyed()));
                lostEnemies.addAll(nullToEmpty(fleetResult.getDisabled()));
            }
        }
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        BattleEngagement battleEngagement = new BattleEngagement(engagementResult.getBattle(), difficultyMultiplier, defeatedDp,
                List.copyOf(lostEnemies));
        LootSources.all().forEach(source -> source.recordEngagement(pendingLoot.battleLoot, battleEngagement));
        if (defeatedDp > 0f) {
            pendingLoot.partsEarned += defeatedDp * ItemDrops.battlePartsPerDeploymentPoint() * difficultyMultiplier;
        }
    }

    private static List<FleetMemberAPI> nullToEmpty(List<FleetMemberAPI> members) {
        return members == null ? List.of() : members;
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        ExiledSector.domains().forEach(domain -> domain.fleetChanged(PlayerFleet.orNull()));
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin encounterPlugin, CargoAPI loot) {
        Pending pendingLoot = encounterPlugin == null ? null : pendingByBattle.remove(encounterPlugin.getBattle());
        if (pendingLoot != null && loot != null) {
            LootSources.all().forEach(source -> source.addEncounterLoot(pendingLoot.battleLoot, loot));
            int wholeParts = ItemDrops.wholeParts(pendingLoot.partsEarned, lootRandom);
            if (wholeParts > 0) {
                MaterialItems.add(loot, ItemDisassembly.PARTS_ITEM_ID, wholeParts);
            }
        }
        pendingByBattle.keySet().removeIf(BattleAPI::isDone);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        Pending pendingLoot = pendingByBattle.remove(battle);
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (pendingLoot == null || !pendingLoot.battleLoot.playerWon() || playerFleet == null) {
            return;
        }
        LootSources.all().forEach(source -> source.deliverBattleLoot(pendingLoot.battleLoot, playerFleet.getCargo()));
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
    }

    public static void addSiteLoot(CargoAPI loot, ItemDrops.SiteLoot siteLoot) {
        siteLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
        siteLoot.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
        siteLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
    }

    public static void deliverSiteLoot(CargoAPI cargo, ItemDrops.SiteLoot siteLoot) {
        deliverItems(cargo, siteLoot.items());
        for (HullUpgradeData upgrade : siteLoot.upgrades()) {
            cargo.addSpecial(upgrade.toSpecialItem(), 1f);
            announceSalvaged(upgrade::name);
        }
        siteLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        MaterialDropAnnouncer.announce(siteLoot.materials());
    }

    public static void deliverItems(CargoAPI cargo, List<ItemData> items) {
        for (ItemData item : items) {
            cargo.addSpecial(item.toSpecialItem(), 1f);
            Item previewItem = item.preview();
            if (previewItem != null) {
                announceSalvaged(previewItem::name);
            }
        }
    }

    public static void announceBattleItems(List<ItemData> items) {
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

    public static void announceSalvaged(Supplier<String> itemName) {
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
