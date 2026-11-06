package exiledsector.skills;

import java.util.List;
import java.util.Set;

public final class LogisticsNodes {

    public static final String TAG = "logistics_cap";
    public static final int DEFAULT_CAP = 2;
    private static final Set<SkillTier> CAPPED_TIERS = Set.of(SkillTier.NOTABLE, SkillTier.KEYSTONE);

    private LogisticsNodes() {
    }

    public static boolean isLogistics(SkillType type) {
        return type != null && CAPPED_TIERS.contains(type.getTier()) && type.getTags().contains(TAG);
    }

    public static boolean isLogistics(SkillNode node, ShipSkillData data) {
        return node != null && isLogistics(node.resolveEffectiveType(data));
    }

    public static int cap(ShipSkillData data) {
        return DEFAULT_CAP + (data == null ? 0 : data.getExtraLogisticsSlots());
    }

    public static int allocated(ShipSkillData data) {
        return data == null ? 0 : allocated(AllocatedNode.of(data));
    }

    public static int allocated(List<AllocatedNode> allocated) {
        int count = 0;
        for (AllocatedNode node : allocated) {
            if (isLogistics(node.effectiveType())) {
                count++;
            }
        }
        return count;
    }
}
