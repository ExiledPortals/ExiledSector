package exiledsector.effects;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ExiledSector;
import exiledsector.PlayerFleet;
import exiledsector.compat.SecretsOfTheFrontierCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemRarity;
import exiledsector.items.ShipSchematic;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.BattleMaterials;
import exiledsector.loot.EncounterSalvage;
import exiledsector.loot.LootSources;
import exiledsector.skills.skilleffect.logistics.SalvagedArmaments;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.cargo.MaterialDropAnnouncer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemLootListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    private static final class Pending {
        private final BattleLoot battleLoot;
        private final BattleMaterials materials = new BattleMaterials();

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
        if (engagementResult.getBattle() == null || SecretsOfTheFrontierCompat.inFlashbackEncounter()) {
            return;
        }
        Pending pendingLoot = pendingByBattle.computeIfAbsent(engagementResult.getBattle(),
                key -> new Pending(new BattleLoot(lootRandom, chartRandom)));
        pendingLoot.battleLoot.setPlayerWon(engagement.playerWon());
        if (!engagement.playerWon()) {
            return;
        }
        float difficultyMultiplier = LootSources.difficultyMultiplier(engagement.difficulty());
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        record(pendingLoot, new BattleEngagement(engagementResult.getBattle(), difficultyMultiplier, defeatedDp, engagement.enemyLosses()));
    }

    private static void record(Pending pendingLoot, BattleEngagement battleEngagement) {
        LootSources.all().forEach(source -> source.recordEngagement(pendingLoot.battleLoot, battleEngagement));
        battleEngagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> pendingLoot.materials.record(
                BattleEngagement.deploymentPoints(members), battleEngagement.difficultyMultiplier(), LootSources.fleetLootRules(sourceFleet)));
    }

    public static void addUnfoughtBattleLoot(CargoAPI loot, CampaignFleetAPI fleet, Random random) {
        if (loot == null || fleet == null || fleet.getFleetData() == null) {
            return;
        }
        Pending pendingLoot = new Pending(new BattleLoot(random, new Random(random.nextLong())));
        pendingLoot.battleLoot.setPlayerWon(true);
        pendingLoot.battleLoot.setEncounterSalvage(EncounterSalvage.NO_WRECKS);
        record(pendingLoot, BattleEngagement.unfought(fleet, fleet.getFleetData().getMembersListCopy()));
        addEncounterLoot(pendingLoot, loot, random);
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        ExiledSector.domains().forEach(domain -> domain.fleetChanged(PlayerFleet.orNull()));
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin encounterPlugin, CargoAPI loot) {
        if (SecretsOfTheFrontierCompat.isFlashbackEncounter(encounterPlugin)) {
            return;
        }
        SalvagedArmaments.addExtra(PlayerFleet.orNull(), loot, lootRandom);
        Pending pendingLoot = encounterPlugin == null ? null : pendingByBattle.remove(encounterPlugin.getBattle());
        if (pendingLoot != null && loot != null) {
            pendingLoot.battleLoot.setEncounterSalvage(EncounterSalvage.of(encounterPlugin));
            addEncounterLoot(pendingLoot, loot, lootRandom);
        }
        pendingByBattle.keySet().removeIf(BattleAPI::isDone);
    }

    private static void addEncounterLoot(Pending pendingLoot, CargoAPI loot, Random random) {
        LootSources.all().forEach(source -> source.addEncounterLoot(pendingLoot.battleLoot, loot));
        int wholeFeedstock = ItemDrops.wholeFeedstock(pendingLoot.materials.feedstock(), random);
        if (wholeFeedstock > 0) {
            MaterialItems.add(loot, ItemDisassembly.FEEDSTOCK_ITEM_ID, wholeFeedstock);
        }
        pendingLoot.materials.rollMaterials(random).forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        Pending pendingLoot = pendingByBattle.remove(battle);
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (pendingLoot == null || !pendingLoot.battleLoot.playerWon() || playerFleet == null) {
            return;
        }
        LootSources.all().forEach(source -> source.deliverBattleLoot(pendingLoot.battleLoot, playerFleet.getCargo()));
        Map<String, Integer> materials = pendingLoot.materials.rollMaterials(lootRandom);
        materials.forEach((itemId, quantity) -> MaterialItems.add(playerFleet.getCargo(), itemId, quantity));
        MaterialDropAnnouncer.announce(materials);
        int wholeFeedstock = ItemDrops.wholeFeedstock(pendingLoot.materials.feedstock(), lootRandom);
        if (wholeFeedstock > 0) {
            MaterialItems.add(playerFleet.getCargo(), ItemDisassembly.FEEDSTOCK_ITEM_ID, wholeFeedstock);
            String feedstockName = MaterialItems.displayName(ItemDisassembly.FEEDSTOCK_ITEM_ID);
            if (CampaignMessages.available()) {
                String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name",
                        Translation.msg("socketable.techMining.material").arg("count", wholeFeedstock).arg("name", feedstockName).text()).text());
                CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
            }
        }
    }

    public static void addSiteLoot(CargoAPI loot, ItemDrops.SiteLoot siteLoot) {
        addItems(loot, siteLoot.items(), siteLoot.upgrades());
        siteLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
    }

    public static void addItems(CargoAPI cargo, List<ItemData> items, List<ShipSchematic> upgrades) {
        LootSources.shapeDrops(items).forEach(item -> cargo.addSpecial(item.toSpecialItem(), 1f));
        upgrades.forEach(upgrade -> cargo.addSpecial(upgrade.toSpecialItem(), 1f));
    }

    public static void deliverSiteLoot(CargoAPI cargo, ItemDrops.SiteLoot siteLoot) {
        deliverSiteLoot(cargo, new ItemDrops.SiteLoot(LootSources.shapeDrops(siteLoot.items()), siteLoot.upgrades(), siteLoot.materials()),
                ItemLootListener::announceSalvaged, MaterialDropAnnouncer::announce);
    }

    public static void deliverSiteLoot(CargoAPI cargo, ItemDrops.SiteLoot siteLoot, Consumer<Supplier<String>> announceFind,
                                       Consumer<Map<String, Integer>> announceMaterials) {
        deliverItems(cargo, siteLoot.items(), announceFind);
        for (ShipSchematic upgrade : siteLoot.upgrades()) {
            cargo.addSpecial(upgrade.toSpecialItem(), 1f);
            announceFind.accept(upgrade::displayName);
        }
        siteLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        announceMaterials.accept(siteLoot.materials());
    }

    public static void deliverItems(CargoAPI cargo, List<ItemData> items) {
        deliverItems(cargo, LootSources.shapeDrops(items), ItemLootListener::announceSalvaged);
    }

    private static void deliverItems(CargoAPI cargo, List<ItemData> items, Consumer<Supplier<String>> announceFind) {
        for (ItemData item : items) {
            cargo.addSpecial(item.toSpecialItem(), 1f);
            Item previewItem = item.preview();
            if (previewItem != null) {
                announceFind.accept(previewItem::name);
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
