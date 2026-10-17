package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.hullitems.HullUpgradeData;
import exiledsector.hullitems.HullUpgradesSetting;
import exiledsector.hullitems.NpcHullItems;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemDisassembly;
import exiledsector.hullitems.HullItemDrops;
import exiledsector.hullitems.HullItemData;
import exiledsector.ui.CampaignMessages;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class HullItemLootListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    private static final class Pending {
        private final Map<String, List<HullItemData>> itemsByMemberId = new LinkedHashMap<>();
        private final Map<String, HullUpgradeData> upgradeByMemberId = new LinkedHashMap<>();
        private boolean playerWon;
        private float partsEarned;

        List<HullItemData> items() {
            List<HullItemData> allItems = new ArrayList<>();
            itemsByMemberId.values().forEach(allItems::addAll);
            return allItems;
        }
    }

    private final Map<BattleAPI, Pending> pendingByBattle = new IdentityHashMap<>();
    private final Random lootRandom;

    public HullItemLootListener() {
        this(new Random());
    }

    HullItemLootListener(Random lootRandom) {
        super(false);
        this.lootRandom = lootRandom;
    }

    void holdLoot(PlayerEngagement engagement) {
        EngagementResultAPI engagementResult = engagement.result();
        if (engagementResult.getBattle() == null) {
            return;
        }
        Pending pendingLoot = pendingByBattle.computeIfAbsent(engagementResult.getBattle(), key -> new Pending());
        pendingLoot.playerWon = engagement.playerWon();
        for (EngagementResultForFleetAPI fleetResult : new EngagementResultForFleetAPI[]{engagementResult.getWinnerResult(), engagementResult.getLoserResult()}) {
            if (fleetResult != null && !fleetResult.isPlayer()) {
                hold(pendingLoot, fleetResult.getDestroyed(), lootRandom);
                hold(pendingLoot, fleetResult.getDisabled(), lootRandom);
            }
        }
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        if (pendingLoot.playerWon && defeatedDp > 0f) {
            float difficultyMultiplier = ShipLevelSystem.difficultyMultiplier(engagement.difficulty(), ShipLevelConfig.xpDifficultyStrength(),
                    ShipLevelConfig.xpDifficultyMaxMultiplier());
            pendingLoot.partsEarned += defeatedDp * HullItemDrops.battlePartsPerDeploymentPoint() * difficultyMultiplier;
        }
    }

    private static void hold(Pending pendingLoot, List<FleetMemberAPI> lostMembers, Random random) {
        if (lostMembers == null) {
            return;
        }
        for (FleetMemberAPI member : lostMembers) {
            String npcTreeTag = member == null ? null : NpcTreeTag.find(member.getVariant());
            ShipSkillData npcTree = npcTreeTag == null ? null : NpcTreeTag.decode(npcTreeTag);
            List<HullItemData> carriedItems = HullUpgradesSetting.enabled() ? NpcHullItems.carriedBy(npcTree)
                    : NpcHullItems.subroutinesCarriedBy(npcTree);
            if (!carriedItems.isEmpty()) {
                pendingLoot.itemsByMemberId.put(member.getId(), carriedItems);
            }
            HullUpgradeData droppedUpgrade = !HullUpgradesSetting.enabled() ? null
                    : NpcHullItems.rollUpgradeDrop(npcTree, member.getHullSpec() == null ? null : member.getHullSpec().getHullSize(), random);
            if (droppedUpgrade != null) {
                pendingLoot.upgradeByMemberId.put(member.getId(), droppedUpgrade);
            }
        }
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        if (recoveredShips == null) {
            return;
        }
        for (FleetMemberAPI recoveredShip : recoveredShips) {
            pendingByBattle.values().forEach(pendingLoot -> {
                pendingLoot.itemsByMemberId.remove(recoveredShip.getId());
                pendingLoot.upgradeByMemberId.remove(recoveredShip.getId());
            });
        }
        ShipTreeSync.fleetChanged(PlayerFleet.orNull());
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin encounterPlugin, CargoAPI loot) {
        Pending pendingLoot = encounterPlugin == null ? null : pendingByBattle.remove(encounterPlugin.getBattle());
        if (pendingLoot != null && loot != null) {
            pendingLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            pendingLoot.upgradeByMemberId.values().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
            int wholeParts = HullItemDrops.wholeParts(pendingLoot.partsEarned, lootRandom);
            if (wholeParts > 0) {
                loot.addCommodity(HullItemDisassembly.PARTS_COMMODITY_ID, wholeParts);
            }
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
        for (HullItemData item : pendingLoot.items()) {
            playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f);
            HullItem previewHullItem = item.preview();
            if (previewHullItem != null) {
                announceSalvaged(previewHullItem::name);
            }
        }
        for (HullUpgradeData upgrade : pendingLoot.upgradeByMemberId.values()) {
            playerFleet.getCargo().addSpecial(upgrade.toSpecialItem(), 1f);
            announceSalvaged(upgrade::name);
        }
        int wholeParts = HullItemDrops.wholeParts(pendingLoot.partsEarned, lootRandom);
        if (wholeParts > 0) {
            playerFleet.getCargo().addCommodity(HullItemDisassembly.PARTS_COMMODITY_ID, wholeParts);
            CommoditySpecAPI partsSpec = Global.getSettings().getCommoditySpec(HullItemDisassembly.PARTS_COMMODITY_ID);
            if (partsSpec != null && CampaignMessages.available()) {
                String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name",
                        Translation.msg("socketable.techMining.material").arg("count", wholeParts).arg("name", partsSpec.getName()).text()).text());
                CampaignMessages.plain(salvageMessage, Misc.getPositiveHighlightColor());
            }
        }
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
