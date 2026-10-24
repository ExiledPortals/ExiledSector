package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.Seeds;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.compat.AbyssSingularity;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionType;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class GateRewardAnchors implements EconomyTickListener {

    static final String GRANTED_PREFIX = "$exiledSector_rewardAnchorGranted_";
    private static final Map<String, String> FORMER_GATE_TAGS = Map.of("gate_existential_threat_defeated", "gate_lunar_fleet_defeated");
    static final String MIGRATION_DONE_KEY = "exiledSector_rewardAnchorMigrationDone";
    private static final Object BATTLE_STATE_KEY = new Object();
    private static final Map<UnlockConditionType, Predicate<BattleEngagement>> BOSS_BEATEN_IN = Map.of(
            UnlockConditionType.RAT_SINGULARITY_DEFEATED, engagement -> AbyssSingularity.containsGenesis(engagement.lostEnemies()),
            UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED, NightcrossUnlocks::existentialThreatBeatenIn);
    private static final Logger LOG = Logger.getLogger(GateRewardAnchors.class);

    @Override
    public void reportEconomyTick(int iterIndex) {
        grantMissed(Global.getSector());
    }

    @Override
    public void reportEconomyMonthEnd() {
    }

    public static void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        if (engagement == null) {
            return;
        }
        Set<String> rewardedGates = battle.state(BATTLE_STATE_KEY, LinkedHashSet::new);
        for (String gateTag : rewardGateTags()) {
            if (bossBeatenIn(gateTag, engagement)) {
                rewardedGates.add(gateTag);
            }
        }
    }

    private static boolean bossBeatenIn(String gateTag, BattleEngagement engagement) {
        for (UnlockCondition condition : NodeGates.gate(gateTag).conditions()) {
            Predicate<BattleEngagement> beaten = BOSS_BEATEN_IN.get(condition.getType());
            if (beaten != null && beaten.test(engagement)) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemData> claim(BattleLoot battle, List<ItemData> drops, SectorAPI sector) {
        List<ItemData> rewards = new ArrayList<>();
        if (!battle.playerWon()) {
            return rewards;
        }
        Set<String> rewardedGates = battle.state(BATTLE_STATE_KEY, LinkedHashSet::new);
        for (String gateTag : rewardedGates) {
            List<ItemDefinition> areaAnchors = anchorsBehind(gateTag);
            if (granted(sector, gateTag) || areaAnchors.isEmpty()) {
                continue;
            }
            markGranted(sector, gateTag);
            if (drops.stream().noneMatch(drop -> areaAnchors.contains(drop.definition()))) {
                rewards.add(ItemData.rolled(areaAnchors.get(battle.random().nextInt(areaAnchors.size())), battle.random().nextLong()));
            }
        }
        return rewards;
    }

    public static void grantMissed(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector == null ? null : sector.getPlayerFleet();
        if (playerFleet == null || !SkillTreeSetting.enabled()) {
            return;
        }
        for (String gateTag : rewardGateTags()) {
            List<ItemDefinition> areaAnchors = anchorsBehind(gateTag);
            if (granted(sector, gateTag) || areaAnchors.isEmpty() || !bossBeaten(gateTag, sector)) {
                continue;
            }
            Random random = Seeds.random(sector.getSeedString() + "|" + GRANTED_PREFIX + gateTag);
            ItemData reward = ItemData.rolled(areaAnchors.get(random.nextInt(areaAnchors.size())), random.nextLong());
            playerFleet.getCargo().addSpecial(reward.toSpecialItem(), 1f);
            markGranted(sector, gateTag);
            WormholeAnchorDefinitions.recordFound(List.of(reward), sector, "a boss reward");
            LOG.info("[ExiledSector] Gave the player a " + reward.definitionId() + " for opening " + gateTag);
            announce(reward);
        }
    }

    private static boolean bossBeaten(String gateTag, SectorAPI sector) {
        for (UnlockCondition condition : NodeGates.gate(gateTag).conditions()) {
            if (condition.getType().metWithModInstalled(sector, condition.getKey())) {
                return true;
            }
        }
        return false;
    }

    public static void markOpenGatesGrantedOnce(SectorAPI sector) {
        Map<String, Object> persistentData = sector == null ? null : sector.getPersistentData();
        if (persistentData == null || Boolean.TRUE.equals(persistentData.get(MIGRATION_DONE_KEY)) || NodeGates.all().isEmpty()) {
            return;
        }
        for (String gateTag : rewardGateTags()) {
            if (bossBeaten(gateTag, sector)) {
                markGranted(sector, gateTag);
            }
        }
        persistentData.put(MIGRATION_DONE_KEY, Boolean.TRUE);
    }

    static List<String> rewardGateTags() {
        List<String> gateTags = new ArrayList<>();
        for (String gateTag : NodeGates.all().keySet()) {
            if (NodeGates.gate(gateTag).rewardAnchor()) {
                gateTags.add(gateTag);
            }
        }
        return gateTags;
    }

    static List<ItemDefinition> anchorsBehind(String gateTag) {
        List<ItemDefinition> anchors = new ArrayList<>();
        for (ItemDefinition anchor : ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR)) {
            String exitNodeId = WormholeAnchorDefinitions.exitNodeId(anchor.id());
            SkillNode exitNode = exitNodeId == null ? null : SkillTree.get(exitNodeId);
            if (exitNode != null && exitNode.getTags().contains(gateTag)) {
                anchors.add(anchor);
            }
        }
        return anchors;
    }

    static boolean granted(SectorAPI sector, String gateTag) {
        MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
        if (sectorMemory == null || sectorMemory.getBoolean(GRANTED_PREFIX + gateTag)) {
            return true;
        }
        String formerTag = FORMER_GATE_TAGS.get(gateTag);
        return formerTag != null && sectorMemory.getBoolean(GRANTED_PREFIX + formerTag);
    }

    private static void markGranted(SectorAPI sector, String gateTag) {
        MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
        if (sectorMemory != null) {
            sectorMemory.set(GRANTED_PREFIX + gateTag, true);
        }
    }

    private static void announce(ItemData reward) {
        Item preview = reward.preview();
        if (preview != null && CampaignMessages.available()) {
            String message = I18n.forGameText(() -> Translation.msg("socketable.anchor.rewardGranted").arg("name", preview.name()).text());
            CampaignMessages.plain(message, Misc.getPositiveHighlightColor());
        }
    }
}
