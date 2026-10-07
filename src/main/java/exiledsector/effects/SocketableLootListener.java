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
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDisassembly;
import exiledsector.socketables.SocketableDrops;
import exiledsector.socketables.SocketableItemData;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SocketableLootListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    private static final class Pending {
        private final Map<String, List<SocketableItemData>> itemsByMemberId = new LinkedHashMap<>();
        private boolean playerWon;
        private float partsEarned;

        List<SocketableItemData> items() {
            List<SocketableItemData> allItems = new ArrayList<>();
            itemsByMemberId.values().forEach(allItems::addAll);
            return allItems;
        }
    }

    private final Map<BattleAPI, Pending> pendingByBattle = new IdentityHashMap<>();
    private final Random partsRandom = new Random();

    public SocketableLootListener() {
        super(false);
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
                hold(pendingLoot, fleetResult.getDestroyed());
                hold(pendingLoot, fleetResult.getDisabled());
            }
        }
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        if (pendingLoot.playerWon && defeatedDp > 0f) {
            float difficultyMultiplier = ShipLevelSystem.difficultyMultiplier(engagement.difficulty(), ShipLevelConfig.xpDifficultyStrength(),
                    ShipLevelConfig.xpDifficultyMaxMultiplier());
            pendingLoot.partsEarned += defeatedDp * SocketableDrops.battlePartsPerDeploymentPoint() * difficultyMultiplier;
        }
    }

    private static void hold(Pending pendingLoot, List<FleetMemberAPI> lostMembers) {
        if (lostMembers == null) {
            return;
        }
        for (FleetMemberAPI member : lostMembers) {
            String npcTreeTag = member == null ? null : NpcTreeTag.find(member.getVariant());
            List<SocketableItemData> carriedItems = npcTreeTag == null ? List.of() : NpcSocketables.carriedBy(NpcTreeTag.decode(npcTreeTag));
            if (!carriedItems.isEmpty()) {
                pendingLoot.itemsByMemberId.put(member.getId(), carriedItems);
            }
        }
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        if (recoveredShips == null) {
            return;
        }
        for (FleetMemberAPI recoveredShip : recoveredShips) {
            pendingByBattle.values().forEach(pendingLoot -> pendingLoot.itemsByMemberId.remove(recoveredShip.getId()));
        }
        ShipTreeSync.fleetChanged(Global.getSector() == null ? null : Global.getSector().getPlayerFleet());
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin encounterPlugin, CargoAPI loot) {
        Pending pendingLoot = encounterPlugin == null ? null : pendingByBattle.remove(encounterPlugin.getBattle());
        if (pendingLoot != null && loot != null) {
            pendingLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            int wholeParts = SocketableDrops.wholeParts(pendingLoot.partsEarned, partsRandom);
            if (wholeParts > 0) {
                loot.addCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, wholeParts);
            }
        }
        pendingByBattle.keySet().removeIf(BattleAPI::isDone);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        Pending pendingLoot = pendingByBattle.remove(battle);
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (pendingLoot == null || !pendingLoot.playerWon || playerFleet == null) {
            return;
        }
        for (SocketableItemData item : pendingLoot.items()) {
            playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f);
            Socketable previewSocketable = item.preview();
            if (previewSocketable != null && Global.getSector().getCampaignUI() != null) {
                String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name", previewSocketable.name()).text());
                Global.getSector().getCampaignUI().addMessage(salvageMessage, Misc.getPositiveHighlightColor());
            }
        }
        int wholeParts = SocketableDrops.wholeParts(pendingLoot.partsEarned, partsRandom);
        if (wholeParts > 0) {
            playerFleet.getCargo().addCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, wholeParts);
            CommoditySpecAPI partsSpec = Global.getSettings().getCommoditySpec(SocketableDisassembly.PARTS_COMMODITY_ID);
            if (partsSpec != null && Global.getSector().getCampaignUI() != null) {
                String salvageMessage = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name",
                        Translation.msg("socketable.techMining.material").arg("count", wholeParts).arg("name", partsSpec.getName()).text()).text());
                Global.getSector().getCampaignUI().addMessage(salvageMessage, Misc.getPositiveHighlightColor());
            }
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        pendingByBattle.keySet().removeIf(battle -> battle.isDone() || !battle.isPlayerInvolved());
    }
}
