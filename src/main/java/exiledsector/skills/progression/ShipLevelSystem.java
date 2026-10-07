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

    public static float awardXpToFleet(CampaignFleetAPI fleet, float xpAmount) {
        if (fleet == null) return 1f;
        List<ShipSkillData> records = new ArrayList<>();
        int topLevel = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            ShipSkillData data = ShipSkillDataManager.get(member.getId());
            records.add(data);
            topLevel = Math.max(topLevel, data.getLevel());
        }

        LevelCurve curve = currentCurve();
        Collection<SkillNode> allNodes = SkillTree.getAllNodes().values();
        float perLevel = ShipLevelConfig.catchUpPerLevel();
        float cap = ShipLevelConfig.catchUpMaxMultiplier();
        float highestCatchUp = 1f;
        for (ShipSkillData data : records) {
            float catchUp = catchUpMultiplier(topLevel - data.getLevel(), perLevel, cap);
            if (data.getLevel() < curve.maxLevel()) {
                highestCatchUp = Math.max(highestCatchUp, catchUp);
            }
            awardXp(data, xpAmount * catchUp, curve, allNodes);
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

    public static float difficultyMultiplier(float difficulty, float strength, float cap) {
        if (!Float.isFinite(difficulty) || !Float.isFinite(strength) || !Float.isFinite(cap)) {
            return 1f;
        }
        float bonus = Math.max(0f, difficulty - 1f) * Math.max(0f, strength);
        return Math.max(1f, Math.min(cap, 1f + bonus));
    }

    public static float catchUpMultiplier(int levelsBehind, float perLevel, float cap) {
        if (levelsBehind <= 0 || !Float.isFinite(perLevel) || !Float.isFinite(cap)) {
            return 1f;
        }
        return Math.max(1f, Math.min(cap, 1f + levelsBehind * Math.max(0f, perLevel)));
    }

    public static int levelFloor(int playerLevel, int floorPercent, int maxLevel) {
        int percent = Math.max(0, Math.min(100, floorPercent));
        return Math.max(0, Math.min(maxLevel, playerLevel * percent / 100));
    }

    public static boolean raiseToLevel(ShipSkillData data, int level, Collection<SkillNode> allNodes) {
        if (data.getLevel() >= level) return false;
        while (data.getLevel() < level) {
            levelUp(data, allNodes);
        }
        return true;
    }

    public static void awardXp(ShipSkillData data, float xpAmount, LevelCurve curve,
                                Collection<SkillNode> allNodes) {
        if (data.getLevel() >= curve.maxLevel() || !Float.isFinite(xpAmount)) return;

        data.addXp(xpAmount);
        while (data.getLevel() < curve.maxLevel()) {
            float required = xpToReachNextLevel(data.getLevel(), curve.xpBase(), curve.xpGrowth(), curve.growthCutoffLevel());
            if (data.getXp() < required) break;

            data.subtractXp(required);
            levelUp(data, allNodes);
        }
    }

    private static void levelUp(ShipSkillData data, Collection<SkillNode> allNodes) {
        data.incrementLevel();
        if (!data.convertMostRecentAllocationToFree(allNodes)) {
            data.addFreeAllocationCredit();
        }
    }

    public record LevelCurve(float xpBase, float xpGrowth, int growthCutoffLevel, int maxLevel) {
    }
}
