package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.tags.SkillTags;

import java.util.Collection;

record AbyssalDepth(float directionX, float directionY, float fadeStart, float fadeEnd) {

    static final float FADE_START_SHARE_OF_GAP = 0.1f;
    static final float FADE_END_SHARE_OF_GAP = 0.8f;

    static AbyssalDepth of(Collection<SkillNode> nodes) {
        float abyssSumX = 0f;
        float abyssSumY = 0f;
        int abyssCount = 0;
        for (SkillNode node : nodes) {
            if (node.getTags().contains(SkillTags.ABYSS_REGION)) {
                abyssSumX += node.getX();
                abyssSumY += node.getY();
                abyssCount++;
            }
        }
        if (abyssCount == 0) return null;
        float abyssCenterX = abyssSumX / abyssCount;
        float abyssCenterY = abyssSumY / abyssCount;
        float abyssDistance = (float) Math.hypot(abyssCenterX, abyssCenterY);
        if (abyssDistance <= 0f) return null;
        float directionX = abyssCenterX / abyssDistance;
        float directionY = abyssCenterY / abyssDistance;

        float mainTreeReach = 0f;
        float abyssNearEdge = Float.POSITIVE_INFINITY;
        for (SkillNode node : nodes) {
            float along = node.getX() * directionX + node.getY() * directionY;
            if (node.getTags().contains(SkillTags.ABYSS_REGION)) {
                abyssNearEdge = Math.min(abyssNearEdge, along);
            } else {
                mainTreeReach = Math.max(mainTreeReach, along);
            }
        }
        float gap = abyssNearEdge - mainTreeReach;
        if (gap <= 0f) return null;
        return new AbyssalDepth(directionX, directionY, mainTreeReach + gap * FADE_START_SHARE_OF_GAP,
                mainTreeReach + gap * FADE_END_SHARE_OF_GAP);
    }

    float depthAt(float treeX, float treeY) {
        return (treeX * directionX + treeY * directionY - fadeStart) / (fadeEnd - fadeStart);
    }
}
