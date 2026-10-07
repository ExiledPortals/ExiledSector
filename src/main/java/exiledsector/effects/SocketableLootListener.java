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
        private final Map<String, List<SocketableItemData>> byMember = new LinkedHashMap<>();
        private boolean playerWon;
        private float parts;

        List<SocketableItemData> items() {
            List<SocketableItemData> items = new ArrayList<>();
            byMember.values().forEach(items::addAll);
            return items;
        }
    }

    private final Map<BattleAPI, Pending> pendingByBattle = new IdentityHashMap<>();
    private final Random random = new Random();

    public SocketableLootListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (result == null || result.getBattle() == null) {
            return;
        }
        Pending pending = pendingByBattle.computeIfAbsent(result.getBattle(), key -> new Pending());
        pending.playerWon = result.didPlayerWin();
        for (EngagementResultForFleetAPI side : new EngagementResultForFleetAPI[]{result.getWinnerResult(), result.getLoserResult()}) {
            if (side != null && !side.isPlayer()) {
                hold(pending, side.getDestroyed());
                hold(pending, side.getDisabled());
            }
        }
        if (pending.playerWon) {
            float defeatedDp = CombatXpListener.enemyDeploymentPointsDefeated(result);
            if (defeatedDp > 0f) {
                float difficulty = ShipLevelSystem.difficultyMultiplier(BattleDifficulty.current(), ShipLevelConfig.xpDifficultyStrength(),
                        ShipLevelConfig.xpDifficultyMaxMultiplier());
                pending.parts += defeatedDp * SocketableDrops.battlePartsPerDeploymentPoint() * difficulty;
            }
        }
    }

    private static void hold(Pending pending, List<FleetMemberAPI> members) {
        if (members == null) {
            return;
        }
        for (FleetMemberAPI member : members) {
            String tag = member == null ? null : NpcTreeTag.find(member.getVariant());
            List<SocketableItemData> items = tag == null ? List.of() : NpcSocketables.carriedBy(NpcTreeTag.decode(tag));
            if (!items.isEmpty()) {
                pending.byMember.put(member.getId(), items);
            }
        }
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
        if (ships == null) {
            return;
        }
        for (FleetMemberAPI ship : ships) {
            pendingByBattle.values().forEach(pending -> pending.byMember.remove(ship.getId()));
        }
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            SkillTreeInstaller.adoptNpcTrees(playerFleet);
        }
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {
        Pending pending = plugin == null ? null : pendingByBattle.remove(plugin.getBattle());
        if (pending != null && loot != null) {
            pending.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            int parts = SocketableDrops.wholeParts(pending.parts, random);
            if (parts > 0) {
                loot.addCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, parts);
            }
        }
        pendingByBattle.keySet().removeIf(BattleAPI::isDone);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        Pending pending = pendingByBattle.remove(battle);
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (pending == null || !pending.playerWon || playerFleet == null) {
            return;
        }
        for (SocketableItemData item : pending.items()) {
            playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f);
            Socketable preview = item.preview();
            if (preview != null && Global.getSector().getCampaignUI() != null) {
                String message = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name", preview.name()).text());
                Global.getSector().getCampaignUI().addMessage(message, Misc.getPositiveHighlightColor());
            }
        }
        int parts = SocketableDrops.wholeParts(pending.parts, random);
        if (parts > 0) {
            playerFleet.getCargo().addCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, parts);
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(SocketableDisassembly.PARTS_COMMODITY_ID);
            if (spec != null && Global.getSector().getCampaignUI() != null) {
                String message = I18n.forGameText(() -> Translation.msg("socketable.salvaged").arg("name",
                        Translation.msg("socketable.techMining.material").arg("count", parts).arg("name", spec.getName()).text()).text());
                Global.getSector().getCampaignUI().addMessage(message, Misc.getPositiveHighlightColor());
            }
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        pendingByBattle.keySet().removeIf(battle -> battle.isDone() || !battle.isPlayerInvolved());
    }
}
