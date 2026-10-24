package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ChartLayoutRecipe(Map<String, Float> starWeights, Map<String, Float> categoryWeights, List<String> backgrounds,
                                List<String> nebulaBackgrounds, float nebulaChance, Color lightTint, float lightTintStrength,
                                Map<StarSystemType, Float> systemTypeWeights, int minOrbits, int maxOrbits, float maxOrbitRadius,
                                String featureId, boolean keepRuins, String musicSetId) {

    public static final Set<String> PHENOMENON_STARS = Set.of("black_hole", "star_neutron");
    public static final Set<String> PHENOMENON_CATEGORIES = Set.of("cat_magfield");
    public static final float VANILLA_ORBIT_RADIUS = 20000f;
    public static final ChartLayoutRecipe ORDINARY = new ChartLayoutRecipe(Map.of(), Map.of(), List.of(), List.of(), Float.NaN, null, 0f,
            Map.of(), -1, -1, Float.NaN, "", false, "");

    public ChartLayoutRecipe {
        starWeights = Map.copyOf(starWeights);
        categoryWeights = Map.copyOf(categoryWeights);
        backgrounds = List.copyOf(backgrounds);
        nebulaBackgrounds = List.copyOf(nebulaBackgrounds);
        lightTintStrength = Math.max(0f, Math.min(1f, lightTintStrength));
        systemTypeWeights = Map.copyOf(systemTypeWeights);
        if (minOrbits >= 0 || maxOrbits >= 0) {
            minOrbits = Math.max(0, minOrbits);
            maxOrbits = Math.max(minOrbits, maxOrbits);
        }
        featureId = featureId == null ? "" : featureId;
        musicSetId = musicSetId == null ? "" : musicSetId;
    }

    public ChartLayoutRecipe(Map<String, Float> starWeights, Map<String, Float> categoryWeights, List<String> backgrounds,
                             List<String> nebulaBackgrounds, float nebulaChance, Color lightTint, float lightTintStrength,
                             Map<StarSystemType, Float> systemTypeWeights, int minOrbits, int maxOrbits, float maxOrbitRadius, String featureId,
                             boolean keepRuins) {
        this(starWeights, categoryWeights, backgrounds, nebulaBackgrounds, nebulaChance, lightTint, lightTintStrength, systemTypeWeights, minOrbits,
                maxOrbits, maxOrbitRadius, featureId, keepRuins, "");
    }

    public float starWeight(String starType, float vanillaWeight) {
        return PHENOMENON_STARS.contains(starType) ? 0f : vanillaWeight * starWeights.getOrDefault(starType, 1f);
    }

    public float categoryWeight(String category, float vanillaWeight) {
        return PHENOMENON_CATEGORIES.contains(category) ? 0f : vanillaWeight * categoryWeights.getOrDefault(category, 1f);
    }

    public boolean setsOrbits() {
        return minOrbits >= 0;
    }

    public float orbitRadiusLimit() {
        return Float.isNaN(maxOrbitRadius) || maxOrbitRadius <= 0f ? VANILLA_ORBIT_RADIUS : maxOrbitRadius;
    }

    public Color tinted(Color light) {
        if (lightTint == null || light == null || lightTintStrength <= 0f) {
            return light;
        }
        return new Color(blend(light.getRed(), lightTint.getRed()), blend(light.getGreen(), lightTint.getGreen()),
                blend(light.getBlue(), lightTint.getBlue()), light.getAlpha());
    }

    private int blend(int from, int to) {
        return Math.round(from + (to - from) * lightTintStrength);
    }
}
