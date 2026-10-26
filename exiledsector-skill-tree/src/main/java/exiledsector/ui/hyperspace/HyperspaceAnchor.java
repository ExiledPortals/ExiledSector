package exiledsector.ui.hyperspace;

import exiledsector.skills.decoration.Star;
import exiledsector.skills.decoration.StaticImage;
import exiledsector.skills.decoration.ThreatUnit;
import exiledsector.skills.layout.SkillTreeObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public record HyperspaceAnchor(String id, String region, float x, float y, float radius, boolean isStar) {

    private static final float NEBULA_CORE_FRACTION = 0.25f;

    public static List<HyperspaceAnchor> collect(List<Star> stars, List<ThreatUnit> threatUnits, List<StaticImage> images) {
        List<HyperspaceAnchor> anchors = new ArrayList<>();
        Set<String> regionsWithStars = new HashSet<>();
        for (Star star : stars) {
            String region = star.getRegion();
            if (region != null) {
                anchors.add(of(star, region, star.getRadius(), true));
                regionsWithStars.add(region);
            }
        }
        for (ThreatUnit unit : threatUnits) {
            String region = unit.getRegion();
            if (region != null && regionsWithStars.add(region)) {
                anchors.add(of(unit, region, unit.getLength() / 2f, false));
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

    public static <N> Set<String> lockedRegions(Collection<N> nodes, Function<N, String> regionOf, Predicate<N> hidden) {
        Set<String> regionsWithNodes = new HashSet<>();
        Set<String> regionsWithVisibleNodes = new HashSet<>();
        for (N node : nodes) {
            String region = regionOf.apply(node);
            if (region == null) {
                continue;
            }
            regionsWithNodes.add(region);
            if (!hidden.test(node)) {
                regionsWithVisibleNodes.add(region);
            }
        }
        regionsWithNodes.removeAll(regionsWithVisibleNodes);
        return regionsWithNodes;
    }

    public static float mapStarRadius(List<HyperspaceAnchor> anchors, float scale) {
        float largest = 0f;
        for (HyperspaceAnchor anchor : anchors) {
            if (anchor.isStar) {
                largest = Math.max(largest, anchor.radius);
            }
        }
        return largest * scale;
    }

    public float displayRadius(float mapStarRadius, float mapAmount) {
        return isStar ? radius + (mapStarRadius - radius) * mapAmount : radius;
    }

    private static HyperspaceAnchor of(SkillTreeObject object, String region, float radius, boolean isStar) {
        return new HyperspaceAnchor(object.getId(), region, object.getX(), object.getY(), radius, isStar);
    }
}
