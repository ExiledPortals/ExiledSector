package exiledsector.ui.hyperspace;

import exiledsector.skills.layout.SkillTreeObject;
import exiledsector.skills.layout.Star;
import exiledsector.skills.layout.StaticImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record HyperspaceAnchor(String id, String region, float x, float y, float radius, boolean star) {

    private static final float NEBULA_CORE_FRACTION = 0.25f;

    public static List<HyperspaceAnchor> collect(List<Star> stars, List<StaticImage> images) {
        List<HyperspaceAnchor> anchors = new ArrayList<>();
        Set<String> regionsWithStars = new HashSet<>();
        for (Star star : stars) {
            String region = star.getRegion();
            if (region != null) {
                anchors.add(of(star, region, star.getRadius(), true));
                regionsWithStars.add(region);
            }
        }
        Set<String> regionsWithNebulae = new HashSet<>();
        for (StaticImage image : images) {
            String region = image.getRegion();
            if (region != null && !regionsWithStars.contains(region) && regionsWithNebulae.add(region)) {
                anchors.add(of(image, region, Math.min(image.getWidth(), image.getHeight()) * NEBULA_CORE_FRACTION, false));
            }
        }
        return anchors;
    }

    public static float mapStarRadius(List<HyperspaceAnchor> anchors, float scale) {
        float largest = 0f;
        for (HyperspaceAnchor anchor : anchors) {
            if (anchor.star) {
                largest = Math.max(largest, anchor.radius);
            }
        }
        return largest * scale;
    }

    public float displayRadius(float mapStarRadius, float mapAmount) {
        return star ? radius + (mapStarRadius - radius) * mapAmount : radius;
    }

    private static HyperspaceAnchor of(SkillTreeObject object, String region, float radius, boolean star) {
        return new HyperspaceAnchor(object.getId(), region, object.getX(), object.getY(), radius, star);
    }
}
