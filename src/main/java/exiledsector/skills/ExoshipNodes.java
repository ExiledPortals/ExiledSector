package exiledsector.skills;

import exiledsector.skills.tags.SkillTags;

public final class ExoshipNodes {

    public static final String EXOSHIP_TYPE_ID = "exoship";

    private ExoshipNodes() {
    }

    public static boolean isExoship(SkillNode node) {
        return node != null && EXOSHIP_TYPE_ID.equals(node.getType().getId());
    }

    public static boolean isCarried(SkillNode node) {
        return node != null && SkillTags.EXOTECH_REGION.equals(node.getRegion());
    }

    public static SkillNode exoship() {
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (isExoship(node)) {
                return node;
            }
        }
        return null;
    }
}
