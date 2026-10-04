package exiledsector.skills.progression;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import java.util.Collection;
import java.util.List;

public final class ShipLevelSystem {

    private ShipLevelSystem() {
    }

    public static void awardXpToFleet(CampaignFleetAPI fleet, float xpAmount) {
        if (fleet == null) return;
        awardXpToMembers(fleet.getFleetData().getMembersListCopy(), xpAmount);
    }

    public static void awardXpToMember(FleetMemberAPI member, float xpAmount) {
        awardXpToMembers(List.of(member), xpAmount);
    }

    private static void awardXpToMembers(List<FleetMemberAPI> members, float xpAmount) {
        LevelCurve curve = new LevelCurve(ShipLevelConfig.xpBase(), ShipLevelConfig.xpGrowth(),
                ShipLevelConfig.xpGrowthCutoffLevel(), ShipLevelConfig.maxLevel());
        Collection<SkillNode> allNodes = SkillTree.getAllNodes().values();
        for (FleetMemberAPI member : members) {
            ShipSkillData data = ShipSkillDataManager.get(member.getId());
            awardXp(data, xpAmount, curve, allNodes);
        }
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

    public static void awardXp(ShipSkillData data, float xpAmount, LevelCurve curve,
                                Collection<SkillNode> allNodes) {
        if (data.getLevel() >= curve.maxLevel() || !Float.isFinite(xpAmount)) return;

        data.addXp(xpAmount);
        while (data.getLevel() < curve.maxLevel()) {
            float required = xpToReachNextLevel(data.getLevel(), curve.xpBase(), curve.xpGrowth(), curve.growthCutoffLevel());
            if (data.getXp() < required) break;

            data.subtractXp(required);
            data.incrementLevel();
            if (!data.convertMostRecentAllocationToFree(allNodes)) {
                data.addFreeAllocationCredit();
            }
        }
    }

    public record LevelCurve(float xpBase, float xpGrowth, int growthCutoffLevel, int maxLevel) {
    }
}
