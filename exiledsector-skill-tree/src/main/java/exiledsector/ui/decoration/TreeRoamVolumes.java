package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.decoration.RingBelt;
import exiledsector.skills.decoration.Star;
import exiledsector.skills.tags.SkillTags;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

final class TreeRoamVolumes {

    static final float DEFAULT_OUTER_RADIUS = 4000f;
    static final float DEFAULT_STAR_RADIUS = 150f;
    static final float STAR_CLEARANCE_MULT = 2.5f;
    static final float OUTLIER_MEDIAN_MULT = 2f;
    static final float VOLUME_EDGE_MULT = 1.1f;

    static RoamVolume core(List<Star> stars, List<RingBelt> ringBelts) {
        Star coreStar = volumeStar(SkillTags.CORE_VOLUME, stars);
        float centerX = coreStar == null ? 0f : coreStar.getX();
        float centerY = coreStar == null ? 0f : coreStar.getY();
        float starRadius = coreStar == null ? DEFAULT_STAR_RADIUS : coreStar.getRadius();
        float outerRadius = 0f;
        for (RingBelt belt : ringBelts) {
            if (SkillTags.CORE_VOLUME.equals(belt.getVolume())) {
                float beltCenterDistance = (float) Math.hypot(belt.getX() - centerX, belt.getY() - centerY);
                outerRadius = Math.max(outerRadius, beltCenterDistance + belt.getOuterRadius());
            }
        }
        float innerRadius = starRadius * STAR_CLEARANCE_MULT;
        if (outerRadius <= innerRadius) {
            outerRadius = Math.max(DEFAULT_OUTER_RADIUS, innerRadius * 2f);
        }
        return new RoamVolume(centerX, centerY, innerRadius, outerRadius);
    }

    static RoamVolume volume(String volume, List<Star> stars, Collection<SkillNode> nodes) {
        List<SkillNode> volumeNodes = nodes.stream().filter(node -> volume.equals(node.getVolume())).toList();
        if (volumeNodes.isEmpty()) {
            return null;
        }
        Star star = volumeStar(volume, stars);
        float centerX = star == null ? (float) volumeNodes.stream().mapToDouble(SkillNode::getOffsetX).average().orElse(0) : star.getX();
        float centerY = star == null ? (float) volumeNodes.stream().mapToDouble(SkillNode::getOffsetY).average().orElse(0) : star.getY();
        float[] distances = new float[volumeNodes.size()];
        for (int i = 0; i < distances.length; i++) {
            distances[i] = (float) Math.hypot(volumeNodes.get(i).getOffsetX() - centerX, volumeNodes.get(i).getOffsetY() - centerY);
        }
        Arrays.sort(distances);
        float outlierDistance = distances[distances.length / 2] * OUTLIER_MEDIAN_MULT;
        float edge = 0f;
        for (float distance : distances) {
            if (distance <= outlierDistance) {
                edge = distance;
            }
        }
        float innerRadius = star == null ? 0f : star.getRadius() * STAR_CLEARANCE_MULT;
        float outerRadius = edge * VOLUME_EDGE_MULT;
        return outerRadius <= innerRadius ? null : new RoamVolume(centerX, centerY, innerRadius, outerRadius);
    }

    private static Star volumeStar(String volume, List<Star> stars) {
        return stars.stream().filter(star -> volume.equals(star.getVolume())).findFirst().orElse(null);
    }

    private TreeRoamVolumes() {
    }
}
