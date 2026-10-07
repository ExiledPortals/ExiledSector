package exiledsector.skills.progression;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ShipLevelSystem {

    private ShipLevelSystem() {
    }

    public static float awardXpToFleet(CampaignFleetAPI awardedFleet, float xpAmount) {
        if (awardedFleet == null) return 1f;
        List<ShipSkillData> shipRecords = new ArrayList<>();
        int topLevel = 0;
        for (FleetMemberAPI member : awardedFleet.getFleetData().getMembersListCopy()) {
            ShipSkillData shipData = ShipSkillDataManager.get(member.getId());
            shipRecords.add(shipData);
            topLevel = Math.max(topLevel, shipData.getLevel());
        }

        LevelCurve levelCurve = currentCurve();
        Collection<SkillNode> allNodes = SkillTree.getAllNodes().values();
        float catchUpPerLevel = ShipLevelConfig.catchUpPerLevel();
        float catchUpCap = ShipLevelConfig.catchUpMaxMultiplier();
        float highestCatchUp = 1f;
        for (ShipSkillData shipData : shipRecords) {
            float catchUp = catchUpMultiplier(topLevel - shipData.getLevel(), catchUpPerLevel, catchUpCap);
            if (shipData.getLevel() < levelCurve.maxLevel()) {
                highestCatchUp = Math.max(highestCatchUp, catchUp);
            }
            awardXp(shipData, xpAmount * catchUp, levelCurve, allNodes);
        }
        return highestCatchUp;
    }

    public static void awardXpToMember(FleetMemberAPI member, float xpAmount) {
        awardXp(ShipSkillDataManager.get(member.getId()), xpAmount, currentCurve(), SkillTree.getAllNodes().values());
    }

    private static LevelCurve currentCurve() {
        return new LevelCurve(ShipLevelConfig.xpBase(), ShipLevelConfig.xpGrowth(),
                ShipLevelConfig.xpGrowthCutoffLevel(), ShipLevelConfig.maxLevel());
    }

    public static float xpToReachNextLevel(int currentLevel, float xpBase, float xpGrowth, int growthCutoffLevel) {
        int cappedLevel = Math.min(currentLevel, Math.max(growthCutoffLevel - 1, 0));
        return xpBase * (float) Math.pow(xpGrowth, cappedLevel);
    }

    public static float difficultyMultiplier(float difficulty, float strength, float maxMultiplier) {
        if (!Float.isFinite(difficulty) || !Float.isFinite(strength) || !Float.isFinite(maxMultiplier)) {
            return 1f;
        }
        float difficultyBonus = Math.max(0f, difficulty - 1f) * Math.max(0f, strength);
        return Math.max(1f, Math.min(maxMultiplier, 1f + difficultyBonus));
    }

    public static float catchUpMultiplier(int levelsBehind, float perLevel, float maxMultiplier) {
        if (levelsBehind <= 0 || !Float.isFinite(perLevel) || !Float.isFinite(maxMultiplier)) {
            return 1f;
        }
        return Math.max(1f, Math.min(maxMultiplier, 1f + levelsBehind * Math.max(0f, perLevel)));
    }

    public static int levelFloor(int playerLevel, int floorPercent, int maxLevel) {
        int clampedFloorPercent = Math.max(0, Math.min(100, floorPercent));
        return Math.max(0, Math.min(maxLevel, playerLevel * clampedFloorPercent / 100));
    }

    public static boolean raiseToLevel(ShipSkillData shipData, int targetLevel, Collection<SkillNode> allNodes) {
        if (shipData.getLevel() >= targetLevel) return false;
        while (shipData.getLevel() < targetLevel) {
            levelUp(shipData, allNodes);
        }
        return true;
    }

    public static void awardXp(ShipSkillData shipData, float xpAmount, LevelCurve levelCurve,
                                Collection<SkillNode> allNodes) {
        if (shipData.getLevel() >= levelCurve.maxLevel() || !Float.isFinite(xpAmount)) return;

        shipData.addXp(xpAmount);
        while (shipData.getLevel() < levelCurve.maxLevel()) {
            float requiredXp = xpToReachNextLevel(shipData.getLevel(), levelCurve.xpBase(), levelCurve.xpGrowth(), levelCurve.growthCutoffLevel());
            if (shipData.getXp() < requiredXp) break;

            shipData.subtractXp(requiredXp);
            levelUp(shipData, allNodes);
        }
    }

    private static void levelUp(ShipSkillData shipData, Collection<SkillNode> allNodes) {
        shipData.incrementLevel();
        if (!shipData.convertMostRecentAllocationToFree(allNodes)) {
            shipData.addFreeAllocationCredit();
        }
    }

    public record LevelCurve(float xpBase, float xpGrowth, int growthCutoffLevel, int maxLevel) {
    }
}
