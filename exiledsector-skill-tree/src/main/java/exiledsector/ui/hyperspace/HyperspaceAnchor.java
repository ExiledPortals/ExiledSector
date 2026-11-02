package exiledsector.ui.hyperspace;

import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.decoration.ShroudedMaw;
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

public record HyperspaceAnchor(String id, String volume, float x, float y, float radius, boolean isStar) {

    private static final float NEBULA_CORE_FRACTION = 0.25f;

    public static List<HyperspaceAnchor> collect(Decorations decorations) {
        List<HyperspaceAnchor> anchors = new ArrayList<>();
        Set<String> volumesWithStars = new HashSet<>();
        for (Star star : decorations.stars()) {
            String volume = star.getVolume();
            if (volume != null) {
                anchors.add(of(star, volume, star.getRadius(), true));
                volumesWithStars.add(volume);
            }
        }
        for (ThreatUnit unit : decorations.threatUnits()) {
            addUnit(anchors, volumesWithStars, unit, unit.getLength() / 2f);
        }
        for (ShroudedMaw maw : decorations.shroudedMaws()) {
            addUnit(anchors, volumesWithStars, maw, maw.getLength() / 2f);
        }
        Set<String> volumesWithNebulae = new HashSet<>();
        for (StaticImage image : decorations.images()) {
            String volume = image.getVolume();
            if (volume != null && !volumesWithStars.contains(volume) && volumesWithNebulae.add(volume)) {
                anchors.add(of(image, volume, Math.min(image.getWidth(), image.getHeight()) * NEBULA_CORE_FRACTION, false));
            }
        }
        return anchors;
    }

    private static void addUnit(List<HyperspaceAnchor> anchors, Set<String> volumesWithStars, SkillTreeObject unit, float radius) {
        String volume = unit.getVolume();
        if (volume != null && volumesWithStars.add(volume)) {
            anchors.add(of(unit, volume, radius, false));
        }
    }

    public static <N> Set<String> lockedVolumes(Collection<N> nodes, Function<N, String> volumeOf, Predicate<N> hidden) {
        Set<String> volumesWithNodes = new HashSet<>();
        Set<String> volumesWithVisibleNodes = new HashSet<>();
        for (N node : nodes) {
            String volume = volumeOf.apply(node);
            if (volume == null) {
                continue;
            }
            volumesWithNodes.add(volume);
            if (!hidden.test(node)) {
                volumesWithVisibleNodes.add(volume);
            }
        }
        volumesWithNodes.removeAll(volumesWithVisibleNodes);
        return volumesWithNodes;
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

    private static HyperspaceAnchor of(SkillTreeObject object, String volume, float radius, boolean isStar) {
        return new HyperspaceAnchor(object.getId(), volume, object.getX(), object.getY(), radius, isStar);
    }
}
