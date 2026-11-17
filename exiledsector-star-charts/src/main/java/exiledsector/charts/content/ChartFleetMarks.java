package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartIds;

public final class ChartFleetMarks {

    private ChartFleetMarks() {
    }

    public static boolean isRunFleet(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return memory != null && memory.contains(ChartIds.TIER_MEMORY_KEY);
    }

    public static int tierOf(MemoryAPI memory, int fallback) {
        return memory.get(ChartIds.TIER_MEMORY_KEY) instanceof Number storedTier ? storedTier.intValue() : fallback;
    }

    public static int strengthBonus(MemoryAPI memory) {
        return memory.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY) instanceof Number storedBonus ? storedBonus.intValue() : 0;
    }

    public static void setStrengthBonus(MemoryAPI memory, int bonus) {
        if (bonus > 0) {
            memory.set(ChartIds.STRENGTH_BONUS_MEMORY_KEY, bonus);
        } else {
            memory.unset(ChartIds.STRENGTH_BONUS_MEMORY_KEY);
        }
    }

    public static void addStrengthBonus(MemoryAPI memory, int addedBonus) {
        memory.set(ChartIds.STRENGTH_BONUS_MEMORY_KEY, strengthBonus(memory) + addedBonus);
    }

    public static void markTwinBoss(CampaignFleetAPI twin, String flagshipId) {
        if (flagshipId != null) {
            twin.getMemoryWithoutUpdate().set(ChartIds.TWIN_BOSS_FLAGSHIP_MEMORY_KEY, flagshipId);
        }
    }

    public static String twinBossFlagshipId(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return memory != null && memory.get(ChartIds.TWIN_BOSS_FLAGSHIP_MEMORY_KEY) instanceof String flagshipId ? flagshipId : null;
    }
}
